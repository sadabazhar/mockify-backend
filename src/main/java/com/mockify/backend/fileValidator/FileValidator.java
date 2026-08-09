package com.mockify.backend.fileValidator;

import com.mockify.backend.exception.InvalidAssetException;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FileValidator {

    private static final long MAX_SIZE = 10 * 1024 * 1024;

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "image/gif",
            "image/svg+xml",
            "image/bmp"
    );

    private static final Set<String> FILE_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip",
            "application/json",
            "application/xml"
    );

    private final Tika tika;



    public String validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidAssetException("File is empty.");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new InvalidAssetException("Maximum file size is 10 MB.");
        }

       try{
           String detectedType =tika.detect(file.getInputStream());

           if(!IMAGE_TYPES.contains(detectedType)
           && !FILE_TYPES.contains(detectedType)){
               throw new InvalidAssetException(
                       "Unsupported file type: " +detectedType
               );
           }

           return detectedType;
       }catch (IOException e){
           throw new InvalidAssetException("Unable to inspect uploaded file.");
       }
    }

    public boolean isImage(String mimeType) {

        return IMAGE_TYPES.contains(mimeType);

    }

}