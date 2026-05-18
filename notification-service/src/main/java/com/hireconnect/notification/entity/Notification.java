package com.hireconnect.notification.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private Long userId;

    private String type; // JOB_ALERT, INTERVIEW, APPLICATION_STATUS

    private String message;

    private boolean isRead = false;

    private LocalDateTime createdAt = LocalDateTime.now();

	public Long getNotificationId() {
		return notificationId;
	}

	public void setNotificationId(Long notificationId) {
		this.notificationId = notificationId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

    @JsonProperty("isRead")
    public boolean isRead() {
        return isRead;
    }

    @JsonProperty("isRead")
    @JsonAlias("read")
    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

    // getters and setters
    
}
