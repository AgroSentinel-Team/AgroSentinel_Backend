package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.dtos.UserNotificationResponse;
import com.agrosentinel.agrosentinel.services.UserNotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class UserNotificationController {
    private final UserNotificationService notificationService;

    @GetMapping
    public List<UserNotificationResponse> findForCurrentUser(Authentication authentication) {
        return notificationService.findForUser(authentication.getName());
    }

    @PatchMapping("/{id}/read")
    public UserNotificationResponse markRead(@PathVariable Long id, Authentication authentication) {
        return notificationService.markRead(id, authentication.getName());
    }
}
