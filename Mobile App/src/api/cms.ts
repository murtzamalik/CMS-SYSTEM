import { encryptPin } from '../lib/aes';
import type {
  CardInquiryResponse,
  CardLimitValidateResponse,
  CardLovResponse,
  CardSpendingSummaryResponse,
  CardStatementResponse,
  CardTransactionHistoryResponse,
  LoginResponse,
} from '../types/cms';
import { apiGet, apiPost, unwrap } from './client';

export async function login(
  username: string,
  password: string,
  relationshipNum?: string,
) {
  const res = await apiPost<LoginResponse>('/user/login', {
    username,
    password,
    ...(relationshipNum ? { relationshipNum } : {}),
  });
  return unwrap(res)!;
}

export async function cardInquiry(
  relationshipNum: string,
  cardType?: string,
  /** Plain PIN — encrypted before send. Reveals CVV for physical cards. */
  pin?: string,
) {
  const res = await apiPost<CardInquiryResponse[]>('/card/inquiry', {
    relationshipNum,
    ...(cardType ? { cardType } : {}),
    ...(pin ? { pin: encryptPin(pin) } : {}),
  });
  if (res.responseCode === 13 || res.responseCode === 3) {
    return { code: res.responseCode, message: res.responseMessage, cards: [] as CardInquiryResponse[] };
  }
  if (res.responseCode !== 0) {
    throw Object.assign(new Error(res.responseMessage), { code: res.responseCode });
  }
  const body = res.responseBody;
  const cards = Array.isArray(body) ? body : [];
  return { code: 0, message: res.responseMessage, cards };
}

export async function getLovs() {
  const res = await apiGet<CardLovResponse[]>('/card/lov/all');
  return unwrap(res) || [];
}

export async function newCardRequest(payload: {
  cardTitle: string;
  accountNumber: string;
  productCode: string;
  cardType: string;
  relationshipNum: string;
  requestTypeId: string;
}) {
  const res = await apiPost<null>('/card/new-request', payload);
  unwrap(res);
}

export async function updateStatus(pan: string, statusCode: string) {
  const res = await apiPost<null>('/card/update-status', { pan, statusCode });
  unwrap(res);
}

export async function generatePin(payload: {
  pan: string;
  relationshipNum: string;
  pin: string;
  confirmPin: string;
  flag?: string;
  /** Required when flag=F — from LasPay after OTP. */
  otpProof?: string;
}) {
  const res = await apiPost<null>('/card/generate-pin', {
    ...payload,
    pin: encryptPin(payload.pin),
    confirmPin: encryptPin(payload.confirmPin),
  });
  unwrap(res);
}

export async function changePin(payload: {
  pan: string;
  relationshipNum: string;
  oldPin: string;
  newPin: string;
  confirmNewPin: string;
}) {
  const res = await apiPost<null>('/card/change-pin', {
    ...payload,
    oldPin: encryptPin(payload.oldPin),
    newPin: encryptPin(payload.newPin),
    confirmNewPin: encryptPin(payload.confirmNewPin),
  });
  unwrap(res);
}

export async function availableLimit(
  pan: string,
  channelCode: number,
  tranCode = 1,
) {
  const res = await apiPost<CardLimitValidateResponse>('/card/limit/available', {
    pan,
    channelCode,
    tranCode,
  });
  return unwrap(res)!;
}

export async function validateLimit(
  pan: string,
  channelCode: number,
  amount: string,
  tranCode = 1,
) {
  const res = await apiPost<CardLimitValidateResponse>('/card/limit/validate', {
    pan,
    channelCode,
    tranCode,
    amount,
  });
  if (res.responseCode === 10) {
    return { exceeded: true as const, message: res.responseMessage, data: res.responseBody };
  }
  if (res.responseCode !== 0) {
    throw Object.assign(new Error(res.responseMessage), { code: res.responseCode });
  }
  return { exceeded: false as const, message: res.responseMessage, data: res.responseBody };
}

export async function spendingSummary(payload: {
  accountNumber?: string;
  pan?: string;
  channelCode?: string;
  tranCode?: string;
}) {
  const res = await apiPost<CardSpendingSummaryResponse[]>(
    '/card/spending-summary',
    payload,
  );
  if (res.responseCode === 3) {
    return [];
  }
  return unwrap(res) || [];
}

export async function transactionHistory(payload: {
  relationshipNum?: string;
  pan?: string;
  channelCode?: number;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}) {
  const res = await apiPost<CardTransactionHistoryResponse>(
    '/card/transaction/history',
    payload,
  );
  return unwrap(res)!;
}

export async function cardStatement(payload: {
  relationshipNum?: string;
  pan?: string;
  channelCode?: number;
  month?: string;
  fromDate?: string;
  toDate?: string;
}) {
  const res = await apiPost<CardStatementResponse>('/card/statement', payload);
  return unwrap(res)!;
}

export function lovMap(
  lovs: CardLovResponse[],
  type: string,
): Record<string, string> {
  return lovs.find((l) => l.type === type)?.lov || {};
}
