package com.cms.app.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CardInquiryRequest {
    @NotNull
    private String relationshipNum;

    /** When set, only this card type is returned. When empty, every type on the relationship is returned. */
    private String cardType;

    /**
     * Optional AES-encrypted PIN (same as generate-pin / change-pin).
     * For physical cards: CVV is returned only when this matches the stored PIN.
     * Ignored for virtual cards (no CMS PIN).
     */
    private String pin;
}
