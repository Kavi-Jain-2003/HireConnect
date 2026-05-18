package com.hireconnect.auth.controller;

import com.hireconnect.auth.dto.ApiResponse;
import com.hireconnect.auth.entity.Role;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.repository.AuthRepository;
import com.hireconnect.auth.security.JwtUtil;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AdminController — only accessible by users with role ADMIN.
 *
 * Endpoints:
 *   GET  /admin/users              — list all registered users
 *   GET  /admin/users/{id}         — get a single user by id
 *   PUT  /admin/users/{id}/suspend — suspend a user (block login)
 *   PUT  /admin/users/{id}/unsuspend — unsuspend a user
 *   DELETE /admin/users/{id}       — permanently delete a user
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AuthRepository authRepository;
    private final JwtUtil jwtUtil;

    public AdminController(AuthRepository authRepository, JwtUtil jwtUtil) {
        this.authRepository = authRepository;
        this.jwtUtil = jwtUtil;
    }

    // ──────────────────────────────────────────────
    // Helper: check that the caller is ADMIN
    // ──────────────────────────────────────────────
    private boolean isAdmin(HttpServletRequest request) {
        // JwtFilter already sets "role" as a request attribute
        String role = (String) request.getAttribute("role");
        if(role==null)
        {
            role=request.getHeader("X-User-Role");
        }
        return "ADMIN".equalsIgnoreCase(role);
    }

    // ──────────────────────────────────────────────
    // GET /admin/users  — list all users
    // ──────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<ApiResponse> getAllUsers(HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.of("Access denied. Admins only.", null));
        }

        List<UserCredential> users = authRepository.findAll();

        // Don't return password hashes — map to safe DTO inline
        List<Map<String, Object>> safeList = users.stream()
                .map(u -> Map.<String, Object>of(
                        "userId", u.getUserId(),
                        "email", u.getEmail(),
                        "role", u.getRole().name(),
                        "suspended", u.isSuspended(),
                        "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""))
                .toList();

        return ResponseEntity.ok(ApiResponse.of("All users fetched successfully", safeList));
    }

    // ──────────────────────────────────────────────
    // GET /admin/users/{id}  — get one user
    // ──────────────────────────────────────────────
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long id,
                                                   HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.of("Access denied. Admins only.", null));
        }

        Optional<UserCredential> userOpt = authRepository.findByUserId(id);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.of("User not found", null));
        }

        UserCredential u = userOpt.get();
        Map<String, Object> safeUser = Map.of(
                "userId", u.getUserId(),
                "email", u.getEmail(),
                "role", u.getRole().name(),
                "suspended", u.isSuspended(),
                "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");

        return ResponseEntity.ok(ApiResponse.of("User fetched successfully", safeUser));
    }

    // ──────────────────────────────────────────────
    // PUT /admin/users/{id}/suspend  — suspend a user
    // ──────────────────────────────────────────────
    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse> suspendUser(@PathVariable Long id,
                                                   HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.of("Access denied. Admins only.", null));
        }

        Optional<UserCredential> userOpt = authRepository.findByUserId(id);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.of("User not found", null));
        }

        UserCredential user = userOpt.get();

        // Don't let admin suspend another admin
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.of("Cannot suspend another admin account.", null));
        }

        user.setSuspended(true);
        authRepository.save(user);

        return ResponseEntity.ok(ApiResponse.of("User suspended successfully", null));
    }

    // ──────────────────────────────────────────────
    // PUT /admin/users/{id}/unsuspend  — unsuspend
    // ──────────────────────────────────────────────
    @PutMapping("/users/{id}/unsuspend")
    public ResponseEntity<ApiResponse> unsuspendUser(@PathVariable Long id,
                                                     HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.of("Access denied. Admins only.", null));
        }

        Optional<UserCredential> userOpt = authRepository.findByUserId(id);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.of("User not found", null));
        }

        UserCredential user = userOpt.get();
        user.setSuspended(false);
        authRepository.save(user);

        return ResponseEntity.ok(ApiResponse.of("User unsuspended successfully", null));
    }

    // ──────────────────────────────────────────────
    // DELETE /admin/users/{id}  — delete a user
    // ──────────────────────────────────────────────
    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id,
                                                  HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.of("Access denied. Admins only.", null));
        }

        Optional<UserCredential> userOpt = authRepository.findByUserId(id);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.of("User not found", null));
        }

        UserCredential user = userOpt.get();

        // Safety: don't let admin delete another admin account
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.of("Cannot delete an admin account.", null));
        }

        authRepository.delete(user);

        return ResponseEntity.ok(ApiResponse.of("User deleted successfully", null));
    }
}
