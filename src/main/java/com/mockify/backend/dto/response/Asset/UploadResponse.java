package com.mockify.backend.dto.response.Asset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResponse {

    private boolean success;

    private String message;

    private String assetType;

    private String url;

}