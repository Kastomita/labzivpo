package com.example.server.controllers;

import com.example.server.entities.Photo;
import com.example.server.services.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadPhoto(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "description", required = false) String description) {
        try {
            Photo photo = photoService.uploadPhoto(file, description);
            return ResponseEntity.ok(Map.of(
                    "message", "Фото успешно загружено",
                    "photoId", photo.getId(),
                    "fileName", photo.getOriginalName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Photo>> getUserPhotos() {
        return ResponseEntity.ok(photoService.getUserPhotos());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deletePhoto(@PathVariable Long id) {
        photoService.deletePhoto(id);
        return ResponseEntity.ok(Map.of("message", "Фото удалено"));
    }
}