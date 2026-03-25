package com.inventory.util;

import java.util.Arrays;

/**
 * Shared magic-byte detection for image content validation.
 * Used by both item upload (ItemServiceImpl) and AI image analysis (ItemController).
 */
public final class ImageContentValidator {

    private ImageContentValidator() {}

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF_MAGIC = {0x47, 0x49, 0x46, 0x38};
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    public static boolean isValidImage(byte[] data) {
        return detectContentType(data) != null;
    }

    public static String detectContentType(byte[] data) {
        if (data == null || data.length < 12) return null;
        if (startsWith(data, JPEG_MAGIC)) return "image/jpeg";
        if (startsWith(data, PNG_MAGIC)) return "image/png";
        if (startsWith(data, GIF_MAGIC)) return "image/gif";
        if (startsWith(data, RIFF_MAGIC) && Arrays.equals(Arrays.copyOfRange(data, 8, 12), WEBP_MAGIC)) {
            return "image/webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
