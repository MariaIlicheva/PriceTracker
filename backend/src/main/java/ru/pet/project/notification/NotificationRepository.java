package ru.pet.project.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop5ByProductIdOrderByCreatedAtDesc(Long productId);
}