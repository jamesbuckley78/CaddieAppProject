package com.caddieapp.availability;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.caddieapp.caddie.Caddie;
import com.caddieapp.common.Enums.QueueStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One caddie's day: created when they press "I'm here", one row per caddie per date. */
@Entity
@Table(name = "queue_entries")
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caddie_id", nullable = false)
    private Caddie caddie;

    /** Admin's manual place in today's line (next feature). Null = follow the rules. */
    @Column(name = "manual_position")
    private Integer manualPosition;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private QueueStatus status;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    /** "Will be back" estimate. Only set while status is BACK_LATER. */
    @Column(name = "back_at")
    private LocalDateTime backAt;

    protected QueueEntry() {
        // for JPA
    }

    public QueueEntry(LocalDate date, Caddie caddie, LocalDateTime checkedInAt) {
        this.date = date;
        this.caddie = caddie;
        this.status = QueueStatus.AVAILABLE;
        this.checkedInAt = checkedInAt;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public Caddie getCaddie() {
        return caddie;
    }

    public Integer getManualPosition() {
        return manualPosition;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public LocalDateTime getBackAt() {
        return backAt;
    }

    public boolean isCheckedIn() {
        return status != QueueStatus.OFF;
    }

    /** Checking in again after "Finished for today". */
    public void checkInAgain(LocalDateTime now) {
        this.status = QueueStatus.AVAILABLE;
        this.checkedInAt = now;
        this.backAt = null;
    }

    public void changeStatus(QueueStatus newStatus, LocalDateTime newBackAt) {
        this.status = newStatus;
        this.backAt = newBackAt;
    }
}
