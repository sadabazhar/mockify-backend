package com.mockify.backend.service;

import com.mockify.backend.dto.response.Asset.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AssetService {

    UploadResponse upload(MultipartFile file);
}
