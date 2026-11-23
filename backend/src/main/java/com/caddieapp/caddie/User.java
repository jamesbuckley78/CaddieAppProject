package com.caddieapp.caddie;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.caddieapp.common.Enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A login account: every caddie and every admin has one row in `users`. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Stored normalised (digits and a leading + only), see PhoneNumbers. Used to log in. */
    @Column(nullable = false, unique = true, length = 30)
    private String phone;

    // @JdbcTypeCode(VARCHAR): store the enum as plain text. Without it Hibernate expects a
    // MySQL ENUM column and schema validation fails.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Role role;

    /** False once they've left the club: can't log in, off the club list. History is kept. */
    @Column(nullable = false)
    private boolean active = true;

    /** BCrypt hash of the password, or of the one-time setup code until the user sets one. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Column(name = "push_token")
    private String pushToken;

    /** Which service the push token belongs to: "EXPO" now, "FCM" for a future Flutter app. */
    @Column(name = "push_provider", length = 10)
    private String pushProvider;

    /** Goes up when the password changes or is reset, which cancels every older login token. */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    /** One-time setup codes stop working after this (null = no expiry, e.g. the first admin). */
    @Column(name = "setup_code_expires_at")
    private LocalDateTime setupCodeExpiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
        // for JPA
    }

    public User(String name, String phone, Role role, String passwordHash, boolean mustChangePassword) {
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.passwordHash = passwordHash;
        this.mustChangePassword = mustChangePassword;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    /** Admin corrects their name or phone number (phone already normalised). */
    public void changeDetails(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    /** Left the club: logs them out everywhere and stops push notifications to their phone. */
    public void markLeft() {
        this.active = false;
        this.tokenVersion++;
        this.pushToken = null;
        this.pushProvider = null;
    }

    /** Back at the club: their old password works again. */
    public void markReturned() {
        this.active = true;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public String getPushToken() {
        return pushToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getPushProvider() {
        return pushProvider;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public LocalDateTime getSetupCodeExpiresAt() {
        return setupCodeExpiresAt;
    }

    /** Replaces the setup code (or old password) with a new hash and clears the setup flag. */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = false;
        this.setupCodeExpiresAt = null;
        this.tokenVersion++; // logs out every other device
    }

    /** New account or forgotten password: a one-time setup code that expires. */
    public void resetToSetupCode(String setupCodeHash, LocalDateTime expiresAt) {
        this.passwordHash = setupCodeHash;
        this.mustChangePassword = true;
        this.setupCodeExpiresAt = expiresAt;
        this.tokenVersion++;
    }

    public void setPushToken(String provider, String pushToken) {
        this.pushProvider = provider;
        this.pushToken = pushToken;
    }
}
