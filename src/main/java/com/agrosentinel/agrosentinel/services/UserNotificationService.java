package com.agrosentinel.agrosentinel.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.UserNotificationResponse;
import com.agrosentinel.agrosentinel.models.UserNotification;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.repositories.UserNotificationRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNotificationService {
    private final UserNotificationRepository notificationRepository;
    private final UsuarioRepository usuarioRepository;

    public List<UserNotificationResponse> findForUser(String email) {
        Usuario user = requireUser(email);
        return notificationRepository.findTop50ByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserNotificationResponse markRead(Long id, String email) {
        UserNotification notification = notificationRepository.findByIdAndUser_Email(id, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Transactional
    public void notifyAssignment(Usuario user, String title, String message, String targetPath) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("La notificación requiere un usuario persistido");
        }
        UserNotification notification = new UserNotification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetPath(targetPath);
        notificationRepository.save(notification);
    }

    private Usuario requireUser(String email) {
        Usuario user = usuarioRepository.findByEmail(email);
        if (user == null || Boolean.FALSE.equals(user.getActivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no disponible");
        }
        return user;
    }

    private UserNotificationResponse toResponse(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTargetPath(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
