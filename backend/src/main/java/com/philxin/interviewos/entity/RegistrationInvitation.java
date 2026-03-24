package com.philxin.interviewos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 邀请注册实体，对应 registration_invitation 表。
 */
@Entity
@Table(
    name = "registration_invitation",
    indexes = {
        @Index(name = "idx_registration_invitation_inviter_created", columnList = "inviter_id,created_at"),
        @Index(name = "idx_registration_invitation_email_used", columnList = "invitee_email,used_at"),
        @Index(name = "idx_registration_invitation_expires_at", columnList = "expires_at")
    }
)
public class RegistrationInvitation {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "inviter_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_registration_invitation_inviter_id")
    )
    private AppUser inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "used_by_user_id",
        foreignKey = @ForeignKey(name = "fk_registration_invitation_used_by_user_id")
    )
    private AppUser usedBy;

    @Column(name = "invitee_email", nullable = false, length = 255)
    private String inviteeEmail;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AppUser getInviter() {
        return inviter;
    }

    public void setInviter(AppUser inviter) {
        this.inviter = inviter;
    }

    public AppUser getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(AppUser usedBy) {
        this.usedBy = usedBy;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public void setInviteeEmail(String inviteeEmail) {
        this.inviteeEmail = inviteeEmail;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
