package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CardProductCreateRequest {

    @NotBlank(message = "productCode is required")
    private String productCode;

    private String productName;

    /** true = active, false = inactive. Default true if null. */
    private Boolean isActive;

    /** Required BIN for this product (used for all card types under it). */
    @NotNull(message = "bin is required")
    private Integer bin;

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Integer getBin() { return bin; }
    public void setBin(Integer bin) { this.bin = bin; }
}
