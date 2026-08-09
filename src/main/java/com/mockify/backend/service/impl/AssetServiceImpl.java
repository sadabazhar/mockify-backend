package com.mockify.backend.service.impl;

import com.mockify.backend.fileValidator.FileValidator;
import com.mockify.backend.constants.AssetConstants;
import com.mockify.backend.dto.response.Asset.UploadResponse;
import com.mockify.backend.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final FileValidator fileValidator;

    @Override
    public UploadResponse upload(MultipartFile file) {

         String mimeType = fileValidator.validate(file);

        if (fileValidator.isImage(mimeType)) {
            return new UploadResponse(
                    true,
                    "Image uploaded successfully.",
                    "IMAGE",
                    AssetConstants.SAMPLE_IMAGE_URL
            );
        }

        return new UploadResponse(
                true,
                "File uploaded successfully.",
                "FILE",
                AssetConstants.SAMPLE_FILE_URL
        );
    }
}