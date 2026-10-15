package com.cms.app.response;

import lombok.Data;

import java.util.List;

@Data
public class CardTransactionHistoryResponse {
    private List<CardTransactionItemResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
