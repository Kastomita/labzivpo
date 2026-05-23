package com.example.server.binary;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class MultipartMixedResponseFactory {

    private static final String BOUNDARY_PREFIX = "batch_";

    public ResponseEntity<byte[]> createMultipartResponse(
            byte[] manifest,
            byte[] signature,
            ExportType manifestType,
            ExportType signatureType) {

        String boundary = BOUNDARY_PREFIX + UUID.randomUUID().toString();

        StringBuilder response = new StringBuilder();

        // Manifest part
        response.append("--").append(boundary).append("\r\n");
        response.append("Content-Type: ").append(manifestType.getContentType()).append("\r\n");
        response.append("Content-Disposition: inline; name=\"manifest\"\r\n\r\n");

        byte[] manifestPart = response.toString().getBytes();
        byte[] manifestData = new byte[manifestPart.length + manifest.length + 2];
        System.arraycopy(manifestPart, 0, manifestData, 0, manifestPart.length);
        System.arraycopy(manifest, 0, manifestData, manifestPart.length, manifest.length);
        manifestData[manifestData.length - 2] = '\r';
        manifestData[manifestData.length - 1] = '\n';

        // Signature part
        String sigHeader = "--" + boundary + "\r\n" +
                "Content-Type: " + signatureType.getContentType() + "\r\n" +
                "Content-Disposition: inline; name=\"signature\"\r\n\r\n";

        byte[] sigHeaderBytes = sigHeader.getBytes();
        byte[] signatureData = new byte[sigHeaderBytes.length + signature.length + 4];
        System.arraycopy(sigHeaderBytes, 0, signatureData, 0, sigHeaderBytes.length);
        System.arraycopy(signature, 0, signatureData, sigHeaderBytes.length, signature.length);
        signatureData[signatureData.length - 4] = '\r';
        signatureData[signatureData.length - 3] = '\n';
        signatureData[signatureData.length - 2] = '-';
        signatureData[signatureData.length - 1] = '-';

        // Combine
        byte[] result = new byte[manifestData.length + signatureData.length];
        System.arraycopy(manifestData, 0, result, 0, manifestData.length);
        System.arraycopy(signatureData, 0, result, manifestData.length, signatureData.length);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "multipart/mixed; boundary=" + boundary);

        log.info("Created multipart/mixed response, boundary: {}, total size: {} bytes",
                boundary, result.length);

        return new ResponseEntity<>(result, headers, HttpStatus.OK);
    }
}