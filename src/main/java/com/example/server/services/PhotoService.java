package com.example.server.services;

import com.example.server.entities.Photo;
import com.example.server.entities.User;
import com.example.server.repositories.PhotoRepository;
import com.example.server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;

    private final String UPLOAD_DIR = "uploads/photos/";

    public Photo uploadPhoto(MultipartFile file, String description) throws IOException {
        User currentUser = getCurrentUser();

        // Создать директорию если не существует
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Сохранить файл
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        Photo photo = Photo.builder()
                .fileName(fileName)
                .filePath(filePath.toString())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .originalName(file.getOriginalFilename())
                .user(currentUser)
                .description(description)
                .build();

        return photoRepository.save(photo);
    }

    public List<Photo> getUserPhotos() {
        User currentUser = getCurrentUser();
        return photoRepository.findByUserId(currentUser.getId());
    }

    public void deletePhoto(Long id) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Фото не найдено"));

        User currentUser = getCurrentUser();
        if (!photo.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Нет прав на удаление этого фото");
        }

        try {
            Files.deleteIfExists(Paths.get(photo.getFilePath()));
        } catch (IOException e) {
            System.err.println("Не удалось удалить файл: " + e.getMessage());
        }

        photoRepository.delete(photo);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
}