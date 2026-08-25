package com.cms.app.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CardInquiryRequest {
    @NotNull
    private String relationshipNum;

    /**
     * Optional AES-encrypted MPIN (same as generate-pin / change-pin).
     * When present and valid, inquiry returns unmasked PAN and CVV.
     * When absent, response is unchanged from the existing flow.
     */
    private String pin;
}
