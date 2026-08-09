package com.mockify.backend.service;

import com.mockify.backend.fileValidator.FileValidator;
import com.mockify.backend.constants.AssetConstants;
import com.mockify.backend.dto.response.Asset.UploadResponse;
import com.mockify.backend.service.impl.AssetServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private FileValidator fileValidator;

    @InjectMocks
    private AssetServiceImpl assetService;

    @Test
    void shouldReturnImageUploadResponseWhenUploadedFileIsImage() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                "dummy-image".getBytes()
        );

        when(fileValidator.validate(file)).thenReturn("image/png");
        when(fileValidator.isImage("image/png")).thenReturn(true);

        // Act
        UploadResponse response = assetService.upload(file);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Image uploaded successfully.", response.getMessage());
        assertEquals("IMAGE", response.getAssetType());
        assertEquals(AssetConstants.SAMPLE_IMAGE_URL, response.getUrl());

        verify(fileValidator).validate(file);
        verify(fileValidator).isImage("image/png");
        verifyNoMoreInteractions(fileValidator);
    }

    @Test
    void shouldReturnFileUploadResponseWhenUploadedFileIsNotImage() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "dummy-pdf".getBytes()
        );

        when(fileValidator.validate(file)).thenReturn("application/pdf");
        when(fileValidator.isImage("application/pdf")).thenReturn(false);

        // Act
        UploadResponse response = assetService.upload(file);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("File uploaded successfully.", response.getMessage());
        assertEquals("FILE", response.getAssetType());
        assertEquals(AssetConstants.SAMPLE_FILE_URL, response.getUrl());

        verify(fileValidator).validate(file);
        verify(fileValidator).isImage("application/pdf");
        verifyNoMoreInteractions(fileValidator);
    }
}