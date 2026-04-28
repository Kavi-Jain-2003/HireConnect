package com.hireconnect.notification.controller;

import com.hireconnect.notification.dto.ApiResponse;
import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.security.JwtUtil;
import com.hireconnect.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @Autowired
    private JwtUtil jwtUtil;

    // send notification
    @PostMapping
    public ResponseEntity<ApiResponse> send(@RequestBody Notification notification) {
        Notification saved = service.sendNotification(notification);
        return ResponseEntity.ok(ApiResponse.of("Notification sent successfully", saved));
    }

    @PostMapping("/public/dispatch")
    public ResponseEntity<ApiResponse> dispatch(@RequestBody Map<String, Object> payload) {
        Long userId = payload.get("userId") == null ? null : Long.valueOf(payload.get("userId").toString());
        String type = payload.get("type") == null ? null : payload.get("type").toString();
        String message = payload.get("message") == null ? null : payload.get("message").toString();
        String email = payload.get("email") == null ? null : payload.get("email").toString();
        String subject = payload.get("subject") == null ? null : payload.get("subject").toString();

        Notification saved = service.dispatchNotification(userId, type, message, email, subject);
        return ResponseEntity.ok(ApiResponse.of("Notification dispatched", saved));
    }

    // get current user's notifications
    @GetMapping("/my")
    public ResponseEntity<ApiResponse> getMyNotifications(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Notifications fetched successfully", service.getByUser(getCurrentUserId(request))));
    }

    // mark single as read
    @PutMapping("/read/{id}")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        service.markAsRead(id, getCurrentUserId(request));
        return ResponseEntity.ok(ApiResponse.of("Marked as read", null));
    }

    // mark all read
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse> markAll(HttpServletRequest request) {
        service.markAllRead(getCurrentUserId(request));
        return ResponseEntity.ok(ApiResponse.of("All marked as read", null));
    }

    // delete notification
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id, HttpServletRequest request) {
        service.deleteNotification(id, getCurrentUserId(request));
        return ResponseEntity.ok(ApiResponse.of("Deleted", null));
    }

    // unread count
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse> unreadCount(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.of("Unread count fetched successfully", service.getUnreadCount(getCurrentUserId(request))));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Long userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token does not contain userId");
        }
        return userId;
    }
}
