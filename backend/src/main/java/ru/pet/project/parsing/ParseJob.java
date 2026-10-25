package ru.pet.project.parsing;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.pet.project.product.Link;

import java.time.Instant;

@Entity
@Table(name = "parse_jobs")
public class ParseJob {

    public static final short MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id", nullable = false, updatable = false)
    private Link link;

    @Column(name = "scheduled_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant scheduledAt;

    @Convert(converter = ParseJobStatusConverter.class)
    @Column(name = "status", nullable = false, length = 12)
    private ParseJobStatus status;

    @Column(name = "attempts", nullable = false)
    private Short attempts;

    @Column(name = "locked_at", columnDefinition = "timestamptz")
    private Instant lockedAt;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    protected ParseJob() {
    }

    public ParseJob(Link link, Instant scheduledAt) {
        this.link = link;
        this.scheduledAt = scheduledAt;
        this.status = ParseJobStatus.PENDING;
        this.attempts = 0;
    }

    // Обработчик взял задачу в работу.
    public void start() {
        this.status = ParseJobStatus.PROCESSING;
        this.lockedAt = Instant.now();
    }

    public void complete() {
        this.status = ParseJobStatus.DONE;
        this.error = null;
    }

    // задача вернётся в очередь, а после последней попытки станет FAILED
    public void fail(String error) {
        this.attempts = (short) (attempts + 1);
        this.error = error;
        this.lockedAt = null;
        this.status = attempts >= MAX_ATTEMPTS ? ParseJobStatus.FAILED : ParseJobStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public Link getLink() {
        return link;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public ParseJobStatus getStatus() {
        return status;
    }

    public short getAttempts() {
        return attempts;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public String getError() {
        return error;
    }
}