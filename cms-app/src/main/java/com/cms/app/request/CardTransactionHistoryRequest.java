package com.cms.app.request;

import lombok.Data;

@Data
public class CardTransactionHistoryRequest {
    /** Preferred ownership key when JWT is bound. */
    private String relationshipNum;
    /** Optional PAN filter (masked or full — resolved via hash when needed). */
    private String pan;
    /** Optional: 1=ATM, 2=POS, 3=e-commerce */
    private Long channelCode;
    /** Inclusive start date yyyy-MM-dd. Defaults to 30 days ago. */
    private String fromDate;
    /** Inclusive end date yyyy-MM-dd. Defaults to today. */
    private String toDate;
    /** 0-based page. Default 0. */
    private Integer page;
    /** Page size. Default 20, max 100. */
    private Integer size;
}
