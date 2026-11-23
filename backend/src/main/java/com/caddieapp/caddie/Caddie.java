package com.caddieapp.caddie;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.caddieapp.common.Enums.Grade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Caddie details on top of the login account: grade and priority. */
@Entity
@Table(name = "caddies")
public class Caddie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Grade grade;

    @Column(name = "is_priority", nullable = false)
    private boolean priority;

    /** 1 = top of the club list. Only changed by the nightly recalculation and by admins. */
    @Column(name = "list_position", nullable = false)
    private int listPosition;

    protected Caddie() {
        // for JPA
    }

    public Caddie(User user, Grade grade, boolean priority, int listPosition) {
        this.user = user;
        this.grade = grade;
        this.priority = priority;
        this.listPosition = listPosition;
    }

    public int getListPosition() {
        return listPosition;
    }

    public void setListPosition(int listPosition) {
        this.listPosition = listPosition;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Grade getGrade() {
        return grade;
    }

    public boolean isPriority() {
        return priority;
    }

    /** Promote / demote, or a junior becoming an adult. The caller re-places them on the list. */
    public void changeGrade(Grade grade, boolean priority) {
        this.grade = grade;
        this.priority = priority;
    }
}
