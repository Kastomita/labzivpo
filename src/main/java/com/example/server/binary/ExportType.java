package com.example.server.binary;

public enum ExportType {
    JSON("application/json"),
    BINARY("application/octet-stream"),
    MULTIPART_MIXED("multipart/mixed");

    private final String contentType;

    ExportType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}