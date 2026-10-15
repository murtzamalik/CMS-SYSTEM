package com.cms.app.request;

import lombok.Data;

@Data
public class ForgotPin {
    private String pan;
    private String relationshipNum;
    private String pin;
    private String confirmPin;
    private String flag;
    /**
     * Required when {@code flag=F} (forgot PIN).
     * LasPay verifies OTP, then sends this proof (signed payload.signature or opaque otpRef).
     * CMS does not verify the SMS OTP itself.
     */
    private String otpProof;
}
