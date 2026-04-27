package com.hireconnect.notification.controller;

import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

    // send notification
    @PostMapping
    public Notification send(@RequestBody Notification notification) {
        return service.sendNotification(notification);
    }

    @PostMapping("/public/dispatch")
    public Notification dispatch(@RequestBody Map<String, Object> payload) {
        Long userId = payload.get("userId") == null ? null : Long.valueOf(payload.get("userId").toString());
        String type = payload.get("type") == null ? null : payload.get("type").toString();
        String message = payload.get("message") == null ? null : payload.get("message").toString();
        String email = payload.get("email") == null ? null : payload.get("email").toString();
        String subject = payload.get("subject") == null ? null : payload.get("subject").toString();

        return service.dispatchNotification(userId, type, message, email, subject);
    }

    // get user notifications
    @GetMapping("/user/{userId}")
    public List<Notification> getByUser(@PathVariable Long userId) {
        return service.getByUser(userId);
    }

    // mark single as read
    @PutMapping("/read/{id}")
    public String markAsRead(@PathVariable Long id) {
        service.markAsRead(id);
        return "Marked as read";
    }

    // mark all read
    @PutMapping("/read-all/{userId}")
    public String markAll(@PathVariable Long userId) {
        service.markAllRead(userId);
        return "All marked as read";
    }

    // delete notification
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteNotification(id);
        return "Deleted";
    }

    // unread count
    @GetMapping("/unread-count/{userId}")
    public long unreadCount(@PathVariable Long userId) {
        return service.getUnreadCount(userId);
    }
}
