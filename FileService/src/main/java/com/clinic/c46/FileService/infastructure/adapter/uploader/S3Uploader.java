package com.clinic.c46.FileService.infastructure.adapter.uploader;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.cloud-front}")
    private String cloudFront;


    public String upload(MultipartFile file) throws IOException {

        String filename = StringUtils.cleanPath(file.getOriginalFilename());

        String key = "uploads/" + LocalDate.now() + "/" + UUID.randomUUID() + "_" + filename;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .cacheControl("public, max-age=31536000")
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return cloudFront + "/" + key;
    }


    public void delete(String url) {
        String key = getKeyFromUrl(url);
        if (key != null) {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
        }
    }

    private String getKeyFromUrl(String url) {
        // Simple extraction assuming standard S3 URL format
        // https://bucket.s3.region.amazonaws.com/key
        // or https://s3.region.amazonaws.com/bucket/key
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String path = urlObj.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }
}
