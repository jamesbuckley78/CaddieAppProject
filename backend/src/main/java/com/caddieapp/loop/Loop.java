package com.caddieapp.loop;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.User;
import com.caddieapp.common.Enums.BagType;

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

/** One bag carried: written when the admin sends a called caddie out on course. */
@Entity
@Table(name = "loops")
public class Loop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caddie_id", nullable = false)
    private Caddie caddie;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "bag_type", nullable = false, length = 10)
    private BagType bagType;

    /** Went round twice (36 holes) on this bag. Marked by the admin; counts the bag twice. */
    @Column(name = "double_loop", nullable = false)
    private boolean doubleLoop;

    @Column(name = "tee_time")
    private LocalTime teeTime;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    /** When they were sent out on course. */
    @Column(name = "accepted_at")
    private LocalDateTime sentOutAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    protected Loop() {
        // for JPA
    }

    public Loop(Caddie caddie, LocalDate date, BagType bagType, LocalDateTime calledAt, LocalDateTime sentOutAt,
                User assignedBy) {
        this.caddie = caddie;
        this.date = date;
        this.bagType = bagType;
        this.calledAt = calledAt;
        this.sentOutAt = sentOutAt;
        this.assignedBy = assignedBy;
    }

    public Long getId() {
        return id;
    }

    public Caddie getCaddie() {
        return caddie;
    }

    public LocalDate getDate() {
        return date;
    }

    public BagType getBagType() {
        return bagType;
    }

    public LocalTime getTeeTime() {
        return teeTime;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public LocalDateTime getSentOutAt() {
        return sentOutAt;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public boolean isDoubleLoop() {
        return doubleLoop;
    }

    /** Bags this loop counts as: a double is 2, and a double loop counts twice (so up to 4). */
    public int bags() {
        return bagType.bags() * (doubleLoop ? 2 : 1);
    }

    public void changeBagType(BagType bagType) {
        this.bagType = bagType;
    }

    public void markDoubleLoop(boolean doubleLoop) {
        this.doubleLoop = doubleLoop;
    }

    public void markReturned(LocalDateTime at) {
        this.returnedAt = at;
    }
}
