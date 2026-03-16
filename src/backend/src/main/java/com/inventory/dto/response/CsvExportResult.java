package com.inventory.dto.response;

public record CsvExportResult(byte[] content, String filename) {
}
