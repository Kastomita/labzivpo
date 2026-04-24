package com.example.server.controllers;

import com.example.server.entities.User;
import com.example.server.entities.UserSession;
import com.example.server.models.ApplicationUserRole;
import com.example.server.models.SessionStatus;
import com.example.server.models.AuthenticationRequest;
import com.example.server.models.AuthenticationResponse;
import com.example.server.models.RefreshTokenRequest;
import com.example.server.repositories.UserRepository;
import com.example.server.repositories.UserSessionRepository;
import com.example.server.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$");

    @PostMapping("/debug")
    public ResponseEntity<?> debug(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("received", body);
        response.put("success", true);
        response.put("message", "Debug endpoint works!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest request) {
        try {
            Map<String, Object> response = new HashMap<>();

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                response.put("error", "Email обязателен для заполнения");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                response.put("error", "Пароль обязателен для заполнения");
                return ResponseEntity.badRequest().body(response);
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                response.put("error", "Email уже существует");
                return ResponseEntity.badRequest().body(response);
            }

            if (!isPasswordValid(request.getPassword())) {
                response.put("error", "Длина пароля должна составлять не менее 8 символов, содержать как минимум одну цифру, " +
                        "одну строчную букву, одну заглавную букву, один специальный символ (@#$%^&+=!), " +
                        "и не содержать пробелов");
                return ResponseEntity.badRequest().body(response);
            }

            User user = User.builder()
                    .name(request.getName() != null ? request.getName() : request.getEmail().split("@")[0])
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(ApplicationUserRole.USER)
                    .build();

            User savedUser = userRepository.save(user);

            response.put("message", "Пользователь успешно зарегистрирован");
            response.put("email", savedUser.getEmail());
            response.put("name", savedUser.getName());
            response.put("role", savedUser.getRole().name());
            response.put("id", savedUser.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Ошибка при регистрации: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PreAuthorize("hasAuthority('modify')")
    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody RegistrationRequest request) {
        try {
            Map<String, Object> response = new HashMap<>();

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                response.put("error", "Email обязателен для заполнения");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                response.put("error", "Пароль обязателен для заполнения");
                return ResponseEntity.badRequest().body(response);
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                response.put("error", "Email уже существует");
                return ResponseEntity.badRequest().body(response);
            }

            if (!isPasswordValid(request.getPassword())) {
                response.put("error", "Длина пароля должна составлять не менее 8 символов, содержать как минимум одну цифру, " +
                        "одну строчную букву, одну заглавную букву, один специальный символ (@#$%^&+=!), " +
                        "и не содержать пробелов");
                return ResponseEntity.badRequest().body(response);
            }

            User user = User.builder()
                    .name(request.getName() != null ? request.getName() : request.getEmail().split("@")[0])
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(ApplicationUserRole.ADMIN)
                    .build();

            User savedUser = userRepository.save(user);

            response.put("message", "Администратор успешно зарегистрирован");
            response.put("email", savedUser.getEmail());
            response.put("name", savedUser.getName());
            response.put("role", savedUser.getRole().name());
            response.put("id", savedUser.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Ошибка при регистрации администратора: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/password-requirements")
    public ResponseEntity<Map<String, String>> getPasswordRequirements() {
        return ResponseEntity.ok(Map.of(
                "requirements",
                "Длина пароля должна составлять не менее 8 символов, содержать как минимум одну цифру, " +
                        "одну строчную букву, одну заглавную букву, один специальный символ (@#$%^&+=!), " +
                        "и не содержать пробелов"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request) {
        try {
            System.out.println("=== LOGIN ATTEMPT ===");
            System.out.println("Email: " + request.getEmail());
            System.out.println("Password: " + request.getPassword());

            String email = request.getEmail();
            String password = request.getPassword();

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                System.out.println("User not found: " + email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Неверные учетные данные"));
            }

            User user = userOpt.get();
            System.out.println("User found: " + user.getEmail());
            System.out.println("Stored password hash: " + user.getPassword());

            // Проверка пароля вручную для отладки
            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
            System.out.println("Password matches: " + passwordMatches);

            if (!passwordMatches) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Неверные учетные данные"));
            }

            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String tempRefreshToken = jwtTokenProvider.generateRefreshToken(user, "temp");

            UserSession userSession = UserSession.builder()
                    .userId(user.getId())
                    .refreshToken(tempRefreshToken)
                    .status(SessionStatus.ACTIVE)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            UserSession savedSession = userSessionRepository.save(userSession);
            String finalRefreshToken = jwtTokenProvider.generateRefreshToken(user, savedSession.getId().toString());
            userSession.setRefreshToken(finalRefreshToken);
            userSessionRepository.save(userSession);

            System.out.println("Login successful for: " + email);

            return ResponseEntity.ok(new AuthenticationResponse(
                    email,
                    accessToken,
                    finalRefreshToken
            ));

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Login error: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Ошибка входа: " + ex.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Недействительный refresh token"));
            }

            String sessionId = jwtTokenProvider.getSessionIdFromRefreshToken(request.getRefreshToken());
            Long userId = jwtTokenProvider.getUserIdFromRefreshToken(request.getRefreshToken());

            Optional<UserSession> sessionOpt = userSessionRepository.findById(Long.parseLong(sessionId));
            if (sessionOpt.isEmpty() || !sessionOpt.get().getStatus().equals(SessionStatus.ACTIVE)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Сессия не активна"));
            }

            UserSession oldSession = sessionOpt.get();
            if (!oldSession.getRefreshToken().equals(request.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Недействительный refresh token"));
            }

            if (!oldSession.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Доступ запрещен"));
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Пользователь не найден"));
            }

            User user = userOpt.get();
            oldSession.setStatus(SessionStatus.REFRESHED);
            oldSession.setRevokedAt(LocalDateTime.now());
            userSessionRepository.save(oldSession);

            UserSession newSession = UserSession.builder()
                    .userId(user.getId())
                    .status(SessionStatus.ACTIVE)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            UserSession savedSession = userSessionRepository.save(newSession);
            String newAccessToken = jwtTokenProvider.generateAccessToken(user);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user, savedSession.getId().toString());

            newSession.setRefreshToken(newRefreshToken);
            userSessionRepository.save(newSession);

            return ResponseEntity.ok(new AuthenticationResponse(
                    user.getEmail(),
                    newAccessToken,
                    newRefreshToken
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Ошибка обновления токена: " + e.getMessage()));
        }
    }

    private boolean isPasswordValid(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public static class RegistrationRequest {
        private String name;
        private String email;
        private String password;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}