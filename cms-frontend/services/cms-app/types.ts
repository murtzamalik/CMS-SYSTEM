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
}

export interface CardInquiryResponse {
  cardTitle: string;
  cardStatusCode: string;
  cardProdStatus: string;
  pan: string;
  createdOn: string;
  cardTypeName: string;
  expiryDate: string;
  cvv: string;
  cvv2: string;
  pinSet?: boolean;
  isVirtual?: boolean;
  sensitiveRevealed?: boolean;
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

export interface CardLovResponse {
  type: string;
  lov: Record<string, string>;
}

export interface CardLimitValidateResponse {
  availableLimit: number;
  availableTranCount: number;
  maxLimit: number;
}

export interface CardAuthorizeResponse {
  pan: string;
  channelCode: string;
  tranCode: string;
  amount: number;
  maxLimit: number;
  availableLimit: number;
  customizedApplied?: boolean;
  actualRowCreated?: boolean;
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

export type LabScreen = 'login' | 'home' | 'request' | 'status' | 'pin' | 'limits';
