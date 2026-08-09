package com.mockify.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockify.backend.dto.response.Asset.UploadResponse;
import com.mockify.backend.service.AssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    @Mock
    private AssetService assetService;

    @InjectMocks
    private AssetController assetController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(assetController)
                .build();
    }

    @Test
    void shouldUploadImageSuccessfully() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                MediaType.IMAGE_PNG_VALUE,
                "dummy".getBytes()
        );

        UploadResponse response = new UploadResponse(
                true,
                "Image uploaded successfully.",
                "IMAGE",
                "https://example.com/image.png"
        );

        when(assetService.upload(file)).thenReturn(response);

        mockMvc.perform(multipart("/api/assets/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Image uploaded successfully."))
                .andExpect(jsonPath("$.assetType").value("IMAGE"))
                .andExpect(jsonPath("$.url").value("https://example.com/image.png"));
    }

    @Test
    void shouldUploadFileSuccessfully() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "dummy".getBytes()
        );

        UploadResponse response = new UploadResponse(
                true,
                "File uploaded successfully.",
                "FILE",
                "https://example.com/file.pdf"
        );

        when(assetService.upload(file)).thenReturn(response);

        mockMvc.perform(multipart("/api/assets/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File uploaded successfully."))
                .andExpect(jsonPath("$.assetType").value("FILE"))
                .andExpect(jsonPath("$.url").value("https://example.com/file.pdf"));
    }
}