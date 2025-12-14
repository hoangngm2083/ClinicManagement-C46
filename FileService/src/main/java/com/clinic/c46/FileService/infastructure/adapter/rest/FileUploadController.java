package com.clinic.c46.FileService.infastructure.adapter.rest;

import com.clinic.c46.FileService.infastructure.adapter.uploader.S3Uploader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileUploadController {

    private final S3Uploader storageService;

    public FileUploadController(S3Uploader storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {

        String url = storageService.upload(file);

        Map<String, Object> response = Map.of("name", file.getOriginalFilename(), "size", file.getSize(), "type",
                file.getContentType(), "url", url);

        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam("url") String url) {
        storageService.delete(url);
        return ResponseEntity.ok().build();
    }
}
