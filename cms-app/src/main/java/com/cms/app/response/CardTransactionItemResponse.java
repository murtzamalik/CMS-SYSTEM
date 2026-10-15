package com.cms.app.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CardTransactionItemResponse {
    private Long id;
    private String pan;
    private String relationshipNum;
    private BigDecimal amount;
    private String channelCode;
    private String tranCode;
    private Integer responseCode;
    private String responseMessage;
    private BigDecimal availableAfter;
    private String authRef;
    private LocalDateTime createdOn;
    private Boolean approved;
}
