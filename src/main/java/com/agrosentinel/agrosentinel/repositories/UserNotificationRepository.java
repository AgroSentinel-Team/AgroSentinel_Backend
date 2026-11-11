package com.agrosentinel.agrosentinel.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agrosentinel.agrosentinel.models.UserNotification;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findTop50ByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<UserNotification> findByIdAndUser_Email(Long id, String email);
}
