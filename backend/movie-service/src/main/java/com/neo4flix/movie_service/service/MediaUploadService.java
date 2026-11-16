package com.neo4flix.movie_service.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.neo4flix.movie_service.exceptions.ApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaUploadService {
    private final Cloudinary cloudinary;
    private final Tika tika = new Tika();
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    public record UploadResult(String url, String publicId) {
    }

    public UploadResult uploadFile(MultipartFile file) throws IOException {

        String realMimeType = tika.detect(file.getBytes(), file.getOriginalFilename());

        // Check mime type
        if (!ALLOWED_MIME_TYPES.contains(realMimeType)) {
            throw ApiException.badRequest("Invalid file type. Only JPEG, PNG, and WEBP images are allowed.");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(), // Upload directly from bytes in memory
                    ObjectUtils.asMap("resource_type", "auto"));

            return new UploadResult(
                    uploadResult.get("secure_url").toString(),
                    uploadResult.get("public_id").toString());

        } catch (Exception e) {
            throw ApiException.badRequest("Failed to upload file to cloud storage");
        }
    }
}