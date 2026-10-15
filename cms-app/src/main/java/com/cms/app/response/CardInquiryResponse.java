package com.cms.app.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CardInquiryResponse {
    private String cardTitle;
    private String cardStatusCode;
    private String cardProdStatus;
    private String pan;
    private LocalDateTime createdOn;
    private String cardTypeCode;
    private String cardTypeName;
    private LocalDateTime expiryDate;
    private String cvv;
    private String cvv2;
    /** true when card already has a PIN stored (pinOffset). */
    private Boolean pinSet;
    /** true when card type is virtual (app-only, no CMS PIN, no print). */
    private Boolean isVirtual;
    /**
     * true when CVV (and for virtual, full PAN) is included in this response.
     * Physical without a matching PIN returns false and null CVV fields.
     */
    private Boolean sensitiveRevealed;
}
