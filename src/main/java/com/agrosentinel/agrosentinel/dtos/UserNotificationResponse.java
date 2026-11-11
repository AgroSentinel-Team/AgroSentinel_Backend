package com.agrosentinel.agrosentinel.dtos;

import java.time.LocalDateTime;

public record UserNotificationResponse(
        Long id,
        String title,
        String message,
        String targetPath,
        LocalDateTime createdAt,
        LocalDateTime readAt) {
}
