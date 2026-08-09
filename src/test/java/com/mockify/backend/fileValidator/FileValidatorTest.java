package com.mockify.backend.fileValidator;

import com.mockify.backend.exception.InvalidAssetException;
import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileValidatorTest {

    @Mock
    private Tika tika;

    @InjectMocks
    private FileValidator fileValidator;

    @Test
    void shouldReturnMimeTypeWhenValidImageIsUploaded() throws IOException {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                "dummy".getBytes()
        );

        when(tika.detect(any(InputStream.class))).thenReturn("image/png");

        // Act
        String mimeType = fileValidator.validate(file);

        // Assert
        assertEquals("image/png", mimeType);
    }

    @Test
    void shouldReturnMimeTypeWhenValidPdfIsUploaded() throws IOException {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        when(tika.detect(any(InputStream.class))).thenReturn("application/pdf");

        // Act
        String mimeType = fileValidator.validate(file);

        // Assert
        assertEquals("application/pdf", mimeType);
    }

    @Test
    void shouldThrowExceptionWhenFileIsNull() {

        InvalidAssetException exception = assertThrows(
                InvalidAssetException.class,
                () -> fileValidator.validate(null)
        );

        assertEquals("File is empty.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenFileIsEmpty() {

        MultipartFile file = new MockMultipartFile(
                "file",
                "",
                "application/pdf",
                new byte[0]
        );

        InvalidAssetException exception = assertThrows(
                InvalidAssetException.class,
                () -> fileValidator.validate(file)
        );

        assertEquals("File is empty.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenFileSizeExceedsTenMB() {

        byte[] largeContent = new byte[10 * 1024 * 1024 + 1];

        MultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeContent
        );

        InvalidAssetException exception = assertThrows(
                InvalidAssetException.class,
                () -> fileValidator.validate(file)
        );

        assertEquals("Maximum file size is 10 MB.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUnsupportedFileTypeIsUploaded() throws IOException {

        MultipartFile file = new MockMultipartFile(
                "file",
                "program.exe",
                "application/octet-stream",
                "dummy".getBytes()
        );

        when(tika.detect(any(InputStream.class)))
                .thenReturn("application/octet-stream");

        InvalidAssetException exception = assertThrows(
                InvalidAssetException.class,
                () -> fileValidator.validate(file)
        );

        assertEquals(
                "Unsupported file type: application/octet-stream",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenTikaThrowsIOException() throws IOException {

        MultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        when(tika.detect(any(InputStream.class)))
                .thenThrow(new IOException("Read error"));

        InvalidAssetException exception = assertThrows(
                InvalidAssetException.class,
                () -> fileValidator.validate(file)
        );

        assertEquals(
                "Unable to inspect uploaded file.",
                exception.getMessage()
        );
    }

    @Test
    void shouldReturnTrueWhenMimeTypeIsImage() {

        assertTrue(fileValidator.isImage("image/png"));
    }

    @Test
    void shouldReturnFalseWhenMimeTypeIsNotImage() {

        assertFalse(fileValidator.isImage("application/pdf"));
    }
}