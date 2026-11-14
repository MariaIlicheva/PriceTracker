package ru.pet.project.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

// Ежедневное обслуживание БД
// секции price_history на неделю вперёд, удаление секций старше 30 дней, чистка старых задач парсинга
@Component
public class PartitionMaintenance {
    private static final Logger log = LoggerFactory.getLogger(PartitionMaintenance.class);
    private final JdbcTemplate jdbcTemplate; //чтобы не заводить отдельный репозиторий

    public PartitionMaintenance(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 30 3 * * *", zone = "Europe/Moscow")  //каждую ночь в 3:30
    public void run() {
        Map<String, Object> partitions = jdbcTemplate.queryForMap("SELECT * FROM maintain_price_history_partitions()");
        int removedJobs = jdbcTemplate.update("DELETE FROM parse_jobs WHERE scheduled_at < now() - INTERVAL '30 days'");
        log.info("Обслуживание БД: секций создано {}, удалено {}; старых задач парсинга удалено {}",
                partitions.get("partitions_created"), partitions.get("partitions_dropped"), removedJobs);
    }
}