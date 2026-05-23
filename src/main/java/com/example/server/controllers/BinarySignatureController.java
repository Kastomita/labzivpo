package com.example.server.controllers;

import com.example.server.binary.*;
import com.example.server.models.LicenseTicket;
import com.example.server.models.Ticket;
import com.example.server.models.TicketResponse;
import com.example.server.services.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.server.entities.User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/binary")
@RequiredArgsConstructor
public class BinarySignatureController {

    private final TicketService ticketService;
    private final DataBinarySerializer dataSerializer;
    private final ManifestBinarySerializer manifestSerializer;
    private final BinarySignatureService binarySignatureService;
    private final MultipartMixedResponseFactory multipartFactory;

    @PostMapping("/ticket/{licenseCode}")
    public ResponseEntity<?> getBinarySignedTicket(
            @PathVariable String licenseCode,
            @RequestBody Map<String, String> request,
            @RequestParam(defaultValue = "MULTIPART_MIXED") ExportType exportType,
            @AuthenticationPrincipal User user) {

        try {
            String macAddress = request.get("macAddress");
            TicketResponse ticketResponse = ticketService.generateTicket(licenseCode, macAddress, user.getId());
            Ticket ticket = ticketResponse.getTicket();

            // Временный метод для получения LicenseTicket
            LicenseTicket licenseTicket = getLicenseTicket(licenseCode, user.getId());

            switch (exportType) {
                case BINARY:
                    byte[] binaryData = dataSerializer.serializeTicket(ticket);
                    byte[] binarySignature = binarySignatureService.signBinary(binaryData);

                    return ResponseEntity.ok()
                            .header("Content-Type", "application/octet-stream")
                            .header("X-Signature", java.util.Base64.getEncoder().encodeToString(binarySignature))
                            .body(binaryData);

                case MULTIPART_MIXED:
                    byte[] manifest = manifestSerializer.serializeManifest(licenseTicket);
                    byte[] signature = binarySignatureService.signBinary(manifest);

                    return multipartFactory.createMultipartResponse(
                            manifest, signature, ExportType.BINARY, ExportType.BINARY
                    );

                case JSON:
                default:
                    return ResponseEntity.ok(ticketResponse);
            }

        } catch (Exception e) {
            log.error("Binary signature generation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private LicenseTicket getLicenseTicket(String licenseCode, Long userId) {
        // Временно возвращаем пустой объект
        // Позже замените на реальный вызов сервиса
        return new LicenseTicket();
    }
}