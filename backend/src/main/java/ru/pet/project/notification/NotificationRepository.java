package ru.pet.project.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop5ByProductIdOrderByCreatedAtDesc(Long productId);
    @Query(value = """
            SELECT *
            FROM notifications
            WHERE status = 'pending'
            ORDER BY created_at, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Notification> lockNextPending();
}