package com.cms.app.request;

import lombok.Data;

@Data
public class CardStatementRequest {
    private String relationshipNum;
    private String pan;
    /** Optional channel filter. */
    private Long channelCode;
    /**
     * Statement month as yyyy-MM. Defaults to current month.
     * When set, fromDate/toDate are ignored.
     */
    private String month;
    private String fromDate;
    private String toDate;
}
