package com.example.server.binary;

import com.example.server.models.LicenseTicket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ManifestBinarySerializer {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE;

    public byte[] serializeManifest(LicenseTicket ticket) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // License ID
        baos.write(BinaryWriteUtils.writeLong(ticket.getLicenseId()));

        // Code
        baos.write(BinaryWriteUtils.writeString(ticket.getCode()));

        // Product Name
        baos.write(BinaryWriteUtils.writeString(ticket.getProductName()));

        // License Type
        baos.write(BinaryWriteUtils.writeString(ticket.getLicenseType()));

        // Owner Email
        baos.write(BinaryWriteUtils.writeString(ticket.getOwnerEmail()));

        // User Email (может быть null)
        String userEmail = ticket.getUserEmail() != null ? ticket.getUserEmail() : "";
        baos.write(BinaryWriteUtils.writeString(userEmail));

        // First Activation Date
        String firstActivation = ticket.getFirstActivationDate() != null
                ? ticket.getFirstActivationDate().format(DATE_FORMATTER) : "";
        baos.write(BinaryWriteUtils.writeString(firstActivation));

        // Ending Date
        String endingDate = ticket.getEndingDate() != null
                ? ticket.getEndingDate().format(DATE_FORMATTER) : "";
        baos.write(BinaryWriteUtils.writeString(endingDate));

        // Blocked
        baos.write(BinaryWriteUtils.writeBoolean(ticket.getBlocked()));

        // Device Count
        baos.write(BinaryWriteUtils.writeInt(ticket.getDeviceCount()));

        // Activated Devices Count
        baos.write(BinaryWriteUtils.writeInt(ticket.getActivatedDevicesCount()));

        log.debug("Manifest serialized to {} bytes", baos.size());
        return baos.toByteArray();
    }
}