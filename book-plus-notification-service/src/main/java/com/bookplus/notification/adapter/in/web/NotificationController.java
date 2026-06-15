package com.bookplus.notification.adapter.in.web;

import com.bookplus.notification.domain.model.Notification;
import com.bookplus.notification.domain.port.in.GetNotificationsUseCase;
import com.bookplus.notification.shared.annotation.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@WebAdapter
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification history")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final GetNotificationsUseCase getNotificationsUseCase;

    @GetMapping
    @Operation(summary = "Get current user's notification history (paginated)")
    public ResponseEntity<PagedNotificationResponse> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userId = jwt.getSubject();
        List<NotificationResponse> items = getNotificationsUseCase.getByUserId(userId, page, size)
                .stream().map(NotificationResponse::from).toList();
        long total = getNotificationsUseCase.countByUserId(userId);
        int  pages = size == 0 ? 0 : (int) Math.ceil((double) total / size);

        return ResponseEntity.ok(new PagedNotificationResponse(items, page, size, total, pages));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "[ADMIN] All sent notifications across users (paginated)")
    public ResponseEntity<PagedNotificationResponse> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<NotificationResponse> items = getNotificationsUseCase.getAll(page, size)
                .stream().map(NotificationResponse::from).toList();
        long total = getNotificationsUseCase.countAll();
        int  pages = size == 0 ? 0 : (int) Math.ceil((double) total / size);

        return ResponseEntity.ok(new PagedNotificationResponse(items, page, size, total, pages));
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────

    record NotificationResponse(
            String  id,
            String  type,
            String  channel,
            String  subject,
            String  status,
            String  recipientEmail,
            String  referenceId,
            Instant createdAt,
            Instant sentAt
    ) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId().toString(),
                    n.getType().name(),
                    n.getChannel().name(),
                    n.getSubject(),
                    n.getStatus().name(),
                    n.getRecipientEmail(),
                    n.getReferenceId(),
                    n.getCreatedAt(),
                    n.getSentAt()
            );
        }
    }

    record PagedNotificationResponse(
            List<NotificationResponse> content,
            int page, int size, long totalElements, int totalPages
    ) {}
}
