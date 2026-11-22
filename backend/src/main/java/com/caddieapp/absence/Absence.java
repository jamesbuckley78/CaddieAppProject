package com.caddieapp.absence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A caddie saying they're not coming in on a date. The admin approves or rejects it. */
@Entity
@Table(name = "absences")
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caddie_id", nullable = false)
    private Caddie caddie;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 500)
    private String reason;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    /** null = waiting for the admin, true = approved, false = rejected. */
    private Boolean approved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    protected Absence() {
        // for JPA
    }

    public Absence(Caddie caddie, LocalDate date, String reason, LocalDateTime submittedAt) {
        this.caddie = caddie;
        this.date = date;
        this.reason = reason;
        this.submittedAt = submittedAt;
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

    public String getReason() {
        return reason;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Boolean getApproved() {
        return approved;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public boolean isPending() {
        return approved == null;
    }

    public void decide(boolean approve, User admin, LocalDateTime at) {
        this.approved = approve;
        this.decidedBy = admin;
        this.decidedAt = at;
    }
}
