# DFS Corporate ↔ CMS Card API (Deployed)

**Audience:** DFS Corporate backend (server-to-server). Browser must not call CMS directly.

---

## 1. Base URLs (this environment)

| Service | Public URL | Docker | Use for |
|---------|------------|--------|---------|
| CMS Portal UI | http://46.224.146.158:7071/ | `CMS-Portal-Frontend` `7071→3000` | Humans only |
| **CMS Portal API** | **http://46.224.146.158:7070** | `CMS-Portal-Backend` `7070→8015` | Card list / detail / status (JWT) |
| **CMS App (channel)** | **http://46.224.146.158:8016** | `CMS-App` `8016→8016` | S2S channel: inquiry / unmask / status (`X-Api-Key` + JWT) |
| Swagger (Portal API) | http://46.224.146.158:7070/swagger-ui.html | — | Explore portal APIs |
| OpenAPI JSON | http://46.224.146.158:7070/v3/api-docs | — | Import to Postman |

**PROD:** same host/ports until a separate PROD host is published. Treat this host as current UAT/pre-prod.

---

## 2. Auth (required)

### A) Portal API (`:7070`) — staff JWT

```http
POST http://46.224.146.158:7070/api/auth/login
Content-Type: application/json

{ "loginId": "<cms-user>", "password": "<password>" }
```

Then:

```http
Authorization: Bearer <token>
```

Use a dedicated **service/integration user** (not a personal admin). Request credentials from CMS Security/Admin. Default seed user exists only for local/dev — do not rely on it in this environment.

### B) CMS App (`:8016`) — API key + user JWT (preferred for S2S channel ops)

Every request:

```http
X-Api-Key: <active-cms-api-key>
```

Card routes also need:

```http
Authorization: Bearer <token from POST /user/login>
```

```http
POST http://46.224.146.158:8016/user/login
Content-Type: application/json
X-Api-Key: <key>

{ "username": "<app-user>", "password": "<password>" }
```

API keys are issued/rotated in CMS Portal → Security → API Keys (`POST /api/api-keys/rotate` on `:7070` with portal JWT).

---

## 3. DFS desired API → what to call today

| DFS Corporate (your side) | Call CMS now | Notes |
|---------------------------|--------------|-------|
| `GET /api/cards` | `POST :7070/api/cards/search` | Masked list, paged |
| `GET /api/cards/{id}` | `GET :7070/api/cards/{id}` | Masked detail |
| `GET /api/accounts/{accountNo}/cards` | **Not available** | Gap — see §7 |
| `POST /api/cards/{id}/unmask` | `POST :8016/card/inquiry` + PIN | Unmasks PAN/CVV; audit on CMS App is limited |
| Status change | `PUT :7070/api/cards/{id}` or `POST :8016/card/update-status` | Different bodies |
| Crypto keys / decrypt | **Not available** | CMS decrypts internally (AES-256-GCM) |

**Recommended DFS proxy mapping:**

```
DFS GET  /api/cards              → CMS POST /api/cards/search   (:7070)
DFS GET  /api/cards/{id}         → CMS GET  /api/cards/{id}     (:7070)
DFS POST /api/cards/{id}/unmask  → CMS POST /card/inquiry       (:8016)
```

---

## 4. Endpoints & samples

### 4.1 List / search cards (masked) — Portal API

```http
POST http://46.224.146.158:7070/api/cards/search
Authorization: Bearer <token>
Content-Type: application/json

{
  "relationshipNum": optional,
  "pan": "last4 optional",
  "cardStatusCode": "ACTIVE",
  "productCode": optional,
  "page": 0,
  "size": 20,
  "sort": "createdOn",
  "sortDir": "desc"
}
```

**Success:**

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1001,
        "cardId": 1001,
        "panMasked": "123456******4821",
        "relationshipNum": "REL-001",
        "cardTitle": "Karachi Retail Hub",
        "expiryDate": "2028-09-30T00:00:00",
        "cardTypeCode": "DEBIT_STD",
        "cardTypeName": "...",
        "cardStatusCode": "ACTIVE",
        "cardStatusName": "Active",
        "productCode": "DEBIT",
        "productName": "Debit",
        "branchCode": "...",
        "limitProfile": "...",
        "createdOn": "2026-09-01T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1
  }
}
```

No full PAN/CVV in list. No `accountNumber` filter yet.

Also: `GET http://46.224.146.158:7070/api/cards` (all, masked).

---

### 4.2 Card detail (masked) — Portal API

```http
GET http://46.224.146.158:7070/api/cards/1001
Authorization: Bearer <token>
```

Same `CardResponse` fields as above. Never returns full PAN/CVV/PIN.

---

### 4.3 Unmask / reveal — CMS App

```http
POST http://46.224.146.158:8016/card/inquiry
X-Api-Key: <key>
Authorization: Bearer <app-jwt>
Content-Type: application/json

{
  "relationshipNum": "REL-001",
  "pin": "<user-mpin-or-card-pin>"
}
```

- Without `pin` → masked PAN (and no clear CVV path).
- With valid `pin` → full PAN + CVV in `responseBody` (treat as reveal).

**Success (masked):**

```json
{
  "responseCode": 0,
  "responseMessage": "SUCCESS",
  "responseBody": {
    "cardTitle": "...",
    "cardStatusCode": "ACTIVE",
    "pan": "************4821",
    "cvv": null,
    "expiryDate": "...",
    "pinSet": true
  }
}
```

**DFS must:** log `requesterId`, `reason`, `cardId`/`relationshipNum`, fields, timestamp on the Corporate backend before/after calling CMS.

---

### 4.4 Status change

**Portal:**

```http
PUT http://46.224.146.158:7070/api/cards/1001
Authorization: Bearer <token>
Content-Type: application/json

{ "cardStatusCode": "INACTIVE", "..." }
```

**CMS App:**

```http
POST http://46.224.146.158:8016/card/update-status
X-Api-Key: <key>
Authorization: Bearer <app-jwt>
Content-Type: application/json

{ "pan": "<pan-or-masked-as-accepted>", "statusCode": "003" }
```

Hot/`003` blocks further changes.

---

### 4.5 Error shapes

**Portal (`:7070`):**

```json
{ "success": false, "message": "Card not found", "data": null }
```

**CMS App (`:8016`):**

```json
{ "responseCode": 1, "responseMessage": "Invalid or revoked API key", "responseBody": null }
```

HTTP 401 if missing/invalid `X-Api-Key` on `:8016`.

---

## 5. Field dictionary (live CMS)

| Field | Values / notes |
|-------|----------------|
| `cardStatusCode` | `ACTIVE`, `INACTIVE`, `CLOSED`; also Cold/Warm/Hot `001`/`002`/`003` in some flows |
| Production status | `000` Pending, `001` Issued, `002` Exported |
| `productCode` (seed) | `DEBIT`, `CREDIT` |
| `cardTypeCode` (seed) | `DEBIT_STD`, `CREDIT_STD` |
| Masked PAN | First 6 + last 4 (middle stars) |
| Parent/child partner IDs | **Not in CMS card model** — scope on DFS Corporate side |

LOVs on channel: `GET :8016/card/lov/{slug}` (products / types / statuses).  
Dropdowns on portal: `GET :7070/api/cards/dropdowns`.

---

## 6. Encryption / security notes

| Topic | Behavior |
|-------|----------|
| At rest | AES-256-GCM (`CARD_ENCRYPTION_KEY`) |
| List/detail | Masked only — no ciphertext blobs for partner decrypt |
| Partner scoped crypto keys | **Not exposed** — do not implement client-side decrypt |
| Reveal | Via `:8016/card/inquiry` + PIN only |
| Audit | Portal mutating APIs audited; reveal audit → implement on **DFS Corporate** proxy |
| Rate limit reveal | Implement on DFS Corporate side |

---

## 7. Gaps (do not block inventory UI)

| Need | Status |
|------|--------|
| Cards by account number | DB supports many cards per account; **no API** yet |
| `accountNumberHash` | Not exposed |
| `POST /api/v1/cards/{id}/reveal` field-scoped | Use inquiry workaround |
| `/api/v1/crypto/*` | Out of scope / not built |
| ParentId / childId on card | Not in CMS |

**Same-account multi-card:** once account→cards API ships, return all cards for one `ACCOUNT_NUM` so DFS can flag different PANs on same account. Until then, inventory is by relationship / card id only.

---

## 8. DFS Corporate wiring checklist

1. Corporate backend base config:
   - `CMS_PORTAL_API=http://46.224.146.158:7070`
   - `CMS_APP_API=http://46.224.146.158:8016`
2. Store portal JWT + refresh; store `X-Api-Key` + app JWT for `:8016`.
3. Proxy:
   - list/detail → `:7070`
   - unmask → `:8016` + Corporate audit log
4. Never put CMS keys or PAN in the browser (`:8060` frontend).
5. Import Postman: `docs/DFS-Corporate-CMS-API.postman_collection.json`

---

## 9. Quick curl smoke

```bash
# Portal login
curl -s -X POST http://46.224.146.158:7070/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"YOUR_USER","password":"YOUR_PASS"}'

# Search cards
curl -s -X POST http://46.224.146.158:7070/api/cards/search \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":20}'

# CMS App (needs key)
curl -s -X POST http://46.224.146.158:8016/user/login \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_KEY" \
  -d '{"username":"USER","password":"PASS"}'
```
