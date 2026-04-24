package com.example.server.services;

import com.example.server.entities.Device;
import com.example.server.repositories.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Transactional(readOnly = true)
    public Device getDeviceByMacOrFail(String macAddress) {
        return deviceRepository.findByMacAddress(macAddress)
                .orElseThrow(() -> new RuntimeException("Device not found with MAC: " + macAddress));
    }

    @Transactional(readOnly = true)
    public Device getDeviceByMacAndUserOrFail(String macAddress, Long userId) {
        return deviceRepository.findByMacAddress(macAddress)
                .filter(device -> device.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Device not found or does not belong to user"));
    }

    @Transactional
    public Device registerDevice(String macAddress, String deviceName, Long userId) {
        if (deviceRepository.existsByMacAddress(macAddress)) {
            Device existingDevice = deviceRepository.findByMacAddress(macAddress).get();
            if (!existingDevice.getUserId().equals(userId)) {
                throw new RuntimeException("Device with MAC " + macAddress + " is already registered to another user");
            }
            return existingDevice;
        }

        Device newDevice = Device.builder()
                .name(deviceName != null && !deviceName.trim().isEmpty() ? deviceName : "Unknown Device")
                .macAddress(macAddress)
                .userId(userId)
                .build();

        return deviceRepository.save(newDevice);
    }

    @Transactional(readOnly = true)
    public List<Device> getUserDevices(Long userId) {
        return deviceRepository.findByUserId(userId);
    }

    @Transactional
    public Device updateDeviceName(Long deviceId, String newName, Long userId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (!device.getUserId().equals(userId)) {
            throw new RuntimeException("Device does not belong to this user");
        }

        device.setName(newName);
        return deviceRepository.save(device);
    }

    @Transactional
    public void deleteDevice(Long deviceId, Long userId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (!device.getUserId().equals(userId)) {
            throw new RuntimeException("Device does not belong to this user");
        }

        deviceRepository.delete(device);
    }

    @Transactional(readOnly = true)
    public boolean deviceExistsAndBelongsToUser(String macAddress, Long userId) {
        return deviceRepository.findByMacAddress(macAddress)
                .map(device -> device.getUserId().equals(userId))
                .orElse(false);
    }
}