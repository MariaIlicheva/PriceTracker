package ru.pet.project.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pet.project.product.ProductStatus;
import java.time.Instant;
import java.util.Optional;

// Очередь исходящих писем: берёт одно неотправленное уведомление и отправляет его
// вызов sendNext() это одна транзакция: блокировка строки, отправка и статус sent
@Service
public class NotificationOutbox {
    private static final Logger log = LoggerFactory.getLogger(NotificationOutbox.class);
    // чем закончилась попытка
    public enum Result { SENT, SKIPPED, EMPTY, MAIL_UNAVAILABLE }
    private final NotificationRepository notificationRepository;
    private final NotificationMessages messages;
    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public NotificationOutbox(NotificationRepository notificationRepository,
                              NotificationMessages messages,
                              JavaMailSender mailSender,
                              NotificationProperties properties) {
        this.notificationRepository = notificationRepository;
        this.messages = messages;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Transactional
    public Result sendNext() {
        Optional<Notification> next = notificationRepository.lockNextPending(); //берем самое старое неотправленное
        if (next.isEmpty()) {
            return Result.EMPTY;
        }
        Notification notification = next.get();

        // устарело
        if (notification.getCreatedAt().isBefore(Instant.now().minus(properties.maxAge()))) {
            notification.markFailed();
            log.info("Уведомление {} устарело и не отправлено", notification.getId());
            return Result.SKIPPED;
        }
        // пока уведомление ждало отправки, пользователь поставил товар на паузу
        if (notification.getProduct().getStatus() == ProductStatus.PAUSED) {
            notification.markFailed();
            log.info("Уведомление {} не отправлено: товар на паузе", notification.getId());
            return Result.SKIPPED;
        }

        try {
            mailSender.send(messages.priceDropped(notification));
            notification.markSent();
            log.info("Отправлено письмо по уведомлению {} (товар {})",
                    notification.getId(), notification.getProduct().getId());
            return Result.SENT;
        } catch (MailParseException | MailPreparationException e) {
            // нельзя собрать, например некорректный адрес
            notification.markFailed();
            log.warn("Уведомление {} не отправлено: {}", notification.getId(), e.getMessage());
            return Result.SKIPPED; // не повторяем больше
        } catch (MailException e) {
            // почтовый сервер недоступен
            log.warn("Почтовый сервер недоступен, отправка отложена: {}", e.getMessage());
            return Result.MAIL_UNAVAILABLE; //попробоавть еще раз
        }
    }
}