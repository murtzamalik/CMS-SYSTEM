package com.cms.dal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CMS_API_KEY")
public class CmsApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cms_api_key_seq")
    @SequenceGenerator(name = "cms_api_key_seq", sequenceName = "CMS_API_KEY_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "KEY_PREFIX", length = 32, nullable = false)
    private String keyPrefix;

    @Column(name = "KEY_HASH", length = 128, nullable = false)
    private String keyHash;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "REVOKED_BY", length = 50)
    private String revokedBy;

    @Column(name = "REVOKED_AT")
    private LocalDateTime revokedAt;

    @Column(name = "LAST_USED_AT")
    private LocalDateTime lastUsedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
