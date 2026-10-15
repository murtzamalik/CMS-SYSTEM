package com.cms.app.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "USM_USER")
public class AppUser {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "LOGIN_ID", length = 50)
    private String loginId;

    @Column(name = "PASSWORD", length = 500)
    private String password;

    @Column(name = "FULL_NAME", length = 255)
    private String fullName;

    @Column(name = "IS_ACTIVE")
    private BigDecimal isActive;

    @Column(name = "WHEN_DELETED")
    private LocalDateTime whenDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public BigDecimal getIsActive() {
        return isActive;
    }

    public void setIsActive(BigDecimal isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getWhenDeleted() {
        return whenDeleted;
    }

    public void setWhenDeleted(LocalDateTime whenDeleted) {
        this.whenDeleted = whenDeleted;
    }
}
