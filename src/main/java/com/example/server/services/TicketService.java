package com.example.server.services;

import com.example.server.entities.Device;
import com.example.server.entities.License;
import com.example.server.models.Ticket;
import com.example.server.models.TicketResponse;
import com.example.server.repositories.DeviceLicenseRepository;
import com.example.server.repositories.DeviceRepository;
import com.example.server.repositories.LicenseRepository;
import com.example.server.repositories.ProductRepository;
import com.example.server.signature.SigningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final LicenseRepository licenseRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final ProductRepository productRepository;
    private final SigningService signingService;

    @Value("${ticket.time-to-live:300}")
    private Long defaultTimeToLive;

    public TicketResponse generateTicket(String activationKey, String macAddress, Long userId) {
        // Get license and device
        License license = getLicenseOrThrow(activationKey);
        Device device = getDeviceAndValidateOwnership(macAddress, userId);

        // Validate license activation on device
        validateLicenseActivation(license, device);

        // Build ticket
        Ticket ticket = buildTicket(license, device, userId);

        // Generate signature and return response
        return buildTicketResponse(ticket, license, device);
    }

    public TicketResponse generateTicketByLicenseId(Long licenseId, Long deviceId, Long userId) {
        // Get license and device
        License license = getLicenseByIdOrThrow(licenseId);
        Device device = getDeviceByIdAndValidateOwnership(deviceId, userId);

        // Validate license activation on device
        validateLicenseActivation(license, device);

        // Build ticket
        Ticket ticket = buildTicket(license, device, userId);

        // Generate signature and return response
        return buildTicketResponse(ticket, license, device);
    }

    private License getLicenseOrThrow(String activationKey) {
        return licenseRepository.findByCode(activationKey)
                .orElseThrow(() -> new RuntimeException("License not found with code: " + activationKey));
    }

    private License getLicenseByIdOrThrow(Long licenseId) {
        return licenseRepository.findById(licenseId)
                .orElseThrow(() -> new RuntimeException("License not found with id: " + licenseId));
    }

    private Device getDeviceAndValidateOwnership(String macAddress, Long userId) {
        Device device = deviceRepository.findByMacAddress(macAddress)
                .orElseThrow(() -> new RuntimeException("Device not found with MAC: " + macAddress));

        if (!device.getUserId().equals(userId)) {
            throw new RuntimeException("Device does not belong to this user");
        }
        return device;
    }

    private Device getDeviceByIdAndValidateOwnership(Long deviceId, Long userId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found with id: " + deviceId));

        if (!device.getUserId().equals(userId)) {
            throw new RuntimeException("Device does not belong to this user");
        }
        return device;
    }

    private void validateLicenseActivation(License license, Device device) {
        boolean isLicenseActivatedOnDevice = deviceLicenseRepository
                .existsByLicenseIdAndDeviceId(license.getId(), device.getId());

        if (!isLicenseActivatedOnDevice) {
            throw new RuntimeException("License is not activated on this device");
        }
    }

    private Ticket buildTicket(License license, Device device, Long userId) {
        String productName = getProductName(license.getProductId());

        return Ticket.builder()
                .serverTime(LocalDateTime.now())
                .timeToLive(defaultTimeToLive)
                .activationDate(license.getFirstActivationDate() != null
                        ? license.getFirstActivationDate().atStartOfDay()
                        : null)
                .expirationDate(license.getEndingDate() != null
                        ? license.getEndingDate().atStartOfDay()
                        : null)
                .userId(userId)
                .deviceId(device.getId())
                .blocked(license.getBlocked())
                .licenseCode(license.getCode())
                .productName(productName)
                .build();
    }

    private String getProductName(Long productId) {
        try {
            return productRepository.findById(productId)
                    .map(product -> product.getName())
                    .orElse("");
        } catch (Exception e) {
            log.warn("Could not fetch product name for productId {}: {}", productId, e.getMessage());
            return "";
        }
    }

    private TicketResponse buildTicketResponse(Ticket ticket, License license, Device device) {
        Long timestamp = System.currentTimeMillis();
        String signature = null;
        String signatureAlgorithm = null;

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("ticket", ticket);
            payload.put("timestamp", timestamp);
            payload.put("licenseCode", license.getCode());
            payload.put("deviceMac", device.getMacAddress());

            signature = signingService.sign(payload);
            signatureAlgorithm = "SHA256withRSA";

            log.debug("Ticket signed successfully for license: {}", license.getCode());
        } catch (Exception e) {
            log.error("Failed to sign ticket for license {}: {}", license.getCode(), e.getMessage(), e);
        }

        return TicketResponse.builder()
                .ticket(ticket)
                .signature(signature)
                .signatureAlgorithm(signatureAlgorithm)
                .timestamp(timestamp)
                .build();
    }
}