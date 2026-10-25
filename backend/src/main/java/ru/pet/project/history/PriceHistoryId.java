package ru.pet.project.history;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;


public class PriceHistoryId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long linkId;
    private Instant capturedAt;

    protected PriceHistoryId() {
    }

    public PriceHistoryId(Long linkId, Instant capturedAt) {
        this.linkId = linkId;
        this.capturedAt = capturedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PriceHistoryId other)) {
            return false;
        }
        return Objects.equals(linkId, other.linkId) && Objects.equals(capturedAt, other.capturedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkId, capturedAt);
    }
}