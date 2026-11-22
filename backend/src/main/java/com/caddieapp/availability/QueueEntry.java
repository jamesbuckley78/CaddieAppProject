package com.caddieapp.availability;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.caddieapp.caddie.Caddie;
import com.caddieapp.common.Enums.BagType;
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

    // While status is CALLED: which bag, when, and whether the caddie tapped "On my way"
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "called_bag_type", length = 10)
    private BagType calledBagType;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @Column(name = "call_accepted_at")
    private LocalDateTime callAcceptedAt;

    /** Calls today they didn't answer ("missed opportunities"). They keep their place. */
    @Column(name = "missed_calls", nullable = false)
    private int missedCalls;

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

    public BagType getCalledBagType() {
        return calledBagType;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public LocalDateTime getCallAcceptedAt() {
        return callAcceptedAt;
    }

    public int getMissedCalls() {
        return missedCalls;
    }

    /** Waiting in line (any of available / getting food / will be back) - can be called. */
    public boolean isWaiting() {
        return status == QueueStatus.AVAILABLE || status == QueueStatus.FOOD || status == QueueStatus.BACK_LATER;
    }

    /** Admin calls them for a bag. */
    public void call(BagType bagType, LocalDateTime at) {
        this.status = QueueStatus.CALLED;
        this.calledBagType = bagType;
        this.calledAt = at;
        this.callAcceptedAt = null;
        this.backAt = null;
    }

    /** Caddie tapped "On my way". */
    public void acceptCall(LocalDateTime at) {
        this.callAcceptedAt = at;
    }

    /** Admin sent them out on course. A manual move only holds until their next bag. */
    public void sendOut() {
        this.status = QueueStatus.OUT;
        this.manualPosition = null;
        clearCall();
    }

    /** Didn't reply: back to waiting, same place in line, one more missed call. */
    public void missCall() {
        this.status = QueueStatus.AVAILABLE;
        this.missedCalls++;
        clearCall();
    }

    /** Back from a loop. */
    public void backIn() {
        this.status = QueueStatus.AVAILABLE;
        this.backAt = null;
    }

    /** Admin's manual place in today's line; null puts them back where the rules say. */
    public void moveTo(Integer lineNumber) {
        this.manualPosition = lineNumber;
    }

    private void clearCall() {
        this.calledBagType = null;
        this.calledAt = null;
        this.callAcceptedAt = null;
    }
}
