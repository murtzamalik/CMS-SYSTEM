package com.cms.app.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    /** Bound into the JWT so card APIs can verify ownership. */
    private String relationshipNum;
}
