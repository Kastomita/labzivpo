package com.example.server.binary;

import com.example.server.models.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class DataBinarySerializer {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public byte[] serializeTicket(Ticket ticket) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // ServerTime
        baos.write(BinaryWriteUtils.writeString(
                ticket.getServerTime().format(ISO_FORMATTER)
        ));

        // TimeToLive
        baos.write(BinaryWriteUtils.writeLong(ticket.getTimeToLive()));

        // ActivationDate (может быть null)
        String activationDate = ticket.getActivationDate() != null
                ? ticket.getActivationDate().format(ISO_FORMATTER) : "";
        baos.write(BinaryWriteUtils.writeString(activationDate));

        // ExpirationDate (может быть null)
        String expirationDate = ticket.getExpirationDate() != null
                ? ticket.getExpirationDate().format(ISO_FORMATTER) : "";
        baos.write(BinaryWriteUtils.writeString(expirationDate));

        // UserId
        baos.write(BinaryWriteUtils.writeLong(ticket.getUserId()));

        // DeviceId
        baos.write(BinaryWriteUtils.writeLong(ticket.getDeviceId()));

        // Blocked
        baos.write(BinaryWriteUtils.writeBoolean(ticket.getBlocked()));

        log.debug("Ticket serialized to {} bytes", baos.size());
        return baos.toByteArray();
    }
}