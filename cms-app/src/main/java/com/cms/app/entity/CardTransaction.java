package com.cms.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CARD_TRANSACTION")
public class CardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "card_txn_seq_gen")
    @SequenceGenerator(name = "card_txn_seq_gen", sequenceName = "CARD_TRANSACTION_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PAN", length = 50, nullable = false)
    private String pan;

    @Column(name = "PAN_HASH", length = 128)
    private String panHash;

    @Column(name = "RELATIONSHIP_NUM", length = 50)
    private String relationshipNum;

    @Column(name = "AMOUNT", precision = 17, scale = 3, nullable = false)
    private BigDecimal amount;

    @Column(name = "CHANNEL_CODE", length = 30, nullable = false)
    private String channelCode;

    @Column(name = "TRAN_CODE", length = 30, nullable = false)
    private String tranCode;

    @Column(name = "RESPONSE_CODE", nullable = false)
    private Integer responseCode;

    @Column(name = "RESPONSE_MESSAGE", length = 255)
    private String responseMessage;

    @Column(name = "AVAILABLE_AFTER", precision = 17, scale = 3)
    private BigDecimal availableAfter;

    @Column(name = "AUTH_REF", length = 100)
    private String authRef;

    @Column(name = "CREATED_ON", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "CREATED_BY", length = 50, nullable = false)
    private String createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }
    public String getPanHash() { return panHash; }
    public void setPanHash(String panHash) { this.panHash = panHash; }
    public String getRelationshipNum() { return relationshipNum; }
    public void setRelationshipNum(String relationshipNum) { this.relationshipNum = relationshipNum; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public String getTranCode() { return tranCode; }
    public void setTranCode(String tranCode) { this.tranCode = tranCode; }
    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }
    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
    public BigDecimal getAvailableAfter() { return availableAfter; }
    public void setAvailableAfter(BigDecimal availableAfter) { this.availableAfter = availableAfter; }
    public String getAuthRef() { return authRef; }
    public void setAuthRef(String authRef) { this.authRef = authRef; }
    public LocalDateTime getCreatedOn() { return createdOn; }
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
