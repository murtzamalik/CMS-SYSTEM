package com.cms.dto.request;

public class CardProductUpdateRequest {

    private String productName;
    private Boolean isActive;
    private Integer bin;

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Integer getBin() { return bin; }
    public void setBin(Integer bin) { this.bin = bin; }
}
