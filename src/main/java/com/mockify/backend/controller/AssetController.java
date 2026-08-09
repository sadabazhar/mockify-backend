package com.mockify.backend.controller;

import com.mockify.backend.dto.response.Asset.UploadResponse;
import com.mockify.backend.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE

    )
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file
            ){
        return ResponseEntity.ok(assetService.upload(file));
    }
}
