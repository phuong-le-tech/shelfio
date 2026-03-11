package com.inventory.service;

import com.inventory.exception.ImageProcessingException;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class ImageProcessingService {

    private static final int MAX_DIMENSION = 1200;
    private static final int WEBP_QUALITY = 80;
    private static final int MAX_SOURCE_DIMENSION = 8000;
    private static final long MAX_PIXEL_COUNT = 25_000_000L; // 25 megapixels
    private static final int PROCESSING_TIMEOUT_SECONDS = 30;

    private final ExecutorService processingExecutor;

    public ImageProcessingService(
            @Value("${image.processing.thread-pool-size:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}") int threadPoolSize) {
        this.processingExecutor = Executors.newFixedThreadPool(threadPoolSize);
    }

    @PreDestroy
    public void shutdownExecutor() {
        processingExecutor.shutdown();
        try {
            if (!processingExecutor.awaitTermination(PROCESSING_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                processingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            processingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public byte[] processToWebP(byte[] input) {
        validateImageDimensions(input);

        Callable<byte[]> task = () -> {
            ImmutableImage image = ImmutableImage.loader().fromBytes(input);
            image = image.max(MAX_DIMENSION, MAX_DIMENSION);
            byte[] output = image.bytes(new WebpWriter().withQ(WEBP_QUALITY));
            if (output.length > input.length) {
                log.warn("WebP conversion increased size: {} -> {} bytes (+{}%)",
                        input.length, output.length,
                        (output.length - input.length) * 100 / input.length);
            }
            return output;
        };

        Future<byte[]> future = processingExecutor.submit(task);
        try {
            return future.get(PROCESSING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("Image processing timed out after {}s", PROCESSING_TIMEOUT_SECONDS);
            throw new ImageProcessingException("Image processing timed out", e);
        } catch (ExecutionException e) {
            log.error("Failed to process image to WebP", e.getCause());
            throw new ImageProcessingException("Image processing failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImageProcessingException("Image processing interrupted", e);
        }
    }

    private void validateImageDimensions(byte[] input) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(input);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)) {
            if (iis == null) {
                throw new ImageProcessingException("Unsupported image format");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new ImageProcessingException("Unsupported image format");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_SOURCE_DIMENSION || height > MAX_SOURCE_DIMENSION) {
                    throw new ImageProcessingException(
                            "Image dimensions too large: " + width + "x" + height
                                    + " (max " + MAX_SOURCE_DIMENSION + "x" + MAX_SOURCE_DIMENSION + ")");
                }
                long pixelCount = (long) width * height;
                if (pixelCount > MAX_PIXEL_COUNT) {
                    throw new ImageProcessingException(
                            "Image pixel count too large: " + pixelCount + " (max " + MAX_PIXEL_COUNT + ")");
                }
            } finally {
                reader.dispose();
            }
        } catch (ImageProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw new ImageProcessingException("Failed to read image dimensions", e);
        }
    }
}
