package com.cms.app.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class CardStatementResponse {
    private String fromDate;
    private String toDate;
    private String relationshipNum;
    private String pan;
    private long totalCount;
    private long approvedCount;
    private long declinedCount;
    private BigDecimal totalApprovedAmount;
    private Map<String, BigDecimal> approvedAmountByChannel;
    private List<CardTransactionItemResponse> transactions;
}
