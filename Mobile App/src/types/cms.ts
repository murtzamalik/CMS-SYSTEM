export interface ResponseWrapper<T> {
  responseCode: number;
  responseMessage: string;
  responseBody: T | null;
}

export interface LoginResponse {
  loginId: string;
  fullName: string;
  token: string;
  groupIds: string[];
  relationshipNum?: string;
}

export interface CardInquiryResponse {
  cardTitle: string;
  cardStatusCode: string;
  cardProdStatus: string;
  pan: string;
  createdOn: string;
  cardTypeCode?: string;
  cardTypeName: string;
  expiryDate: string;
  cvv: string;
  cvv2: string;
  pinSet?: boolean;
  /** App-only card: no CMS PIN, no print/export. */
  isVirtual?: boolean;
  /** true when CVV (and virtual full PAN) is present in this response. */
  sensitiveRevealed?: boolean;
}

export interface CardLovResponse {
  type: string;
  lov: Record<string, string>;
}

export interface CardLimitValidateResponse {
  availableLimit: number;
  availableTranCount: number;
  maxLimit: number;
}

export interface CardSpendingSummaryResponse {
  accountNumber: string;
  pan: string;
  cardTitle: string;
  expiryDate: string;
  cardStatusName: string;
  maxLimit: number;
  singleTranLimit: number;
  dailyAvailableSpending: number;
  monthlyAvailableSpending: number;
}

export interface CardTransactionItem {
  id: number;
  pan: string;
  relationshipNum?: string;
  amount: number;
  channelCode: string;
  tranCode: string;
  responseCode: number;
  responseMessage?: string;
  availableAfter?: number;
  authRef?: string;
  createdOn: string;
  approved: boolean;
}

export interface CardTransactionHistoryResponse {
  items: CardTransactionItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CardStatementResponse {
  fromDate: string;
  toDate: string;
  relationshipNum?: string;
  pan?: string;
  totalCount: number;
  approvedCount: number;
  declinedCount: number;
  totalApprovedAmount: number;
  approvedAmountByChannel: Record<string, number>;
  transactions: CardTransactionItem[];
}

export interface SessionCard {
  pan: string;
  relationshipNum: string;
  cardTitle?: string;
  pinSet: boolean;
  accountNumber?: string;
}

export interface SessionUser {
  loginId: string;
  fullName: string;
  token: string;
  relationshipNum: string;
  card?: SessionCard | null;
}
