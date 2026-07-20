## Context

The application already has VNPay-related classes and routes:

- `VNPayProperties` binds `vnpay.*` configuration.
- `VNPayService` creates VNPay payment URLs and verifies return parameters.
- `VNPayController` handles `GET /vnpay-return` and `/api/payment/vnpay-return`.
- `BookingPaymentService` owns payment confirmation, failure, and expiry behavior.
- `application.properties` currently contains sandbox VNPay values directly.

The new merchant sandbox data should be applied without committing secrets. VNPay also requires a merchant IPN URL for server-to-server updates, which is more authoritative than a browser redirect and must return VNPay-compatible response payloads.

## Goals / Non-Goals

**Goals:**

- Configure the new VNPay sandbox terminal code and payment URL using environment-backed properties.
- Add a VNPay IPN endpoint that can be shared with VNPay for test registration.
- Validate VNPay callback signatures with the configured hash secret before any state change.
- Validate transaction success, booking reference, amount, pending state, and expiry before confirming payment.
- Make VNPay callback handling idempotent so duplicate return/IPN calls do not double-award points or mutate completed bookings incorrectly.
- Remove VietQR and Cash as Customer checkout/payment paths so Customer payments consistently go through VNPay.
- Remove Cash, card, VietQR, QR transfer, and other non-VNPay methods as Staff POS booking payment paths so Staff-created booking payments also consistently go through VNPay.
- Keep secrets out of git and avoid storing merchant admin login data in source files.

**Non-Goals:**

- Add production VNPay credentials.
- Remove historical VietQR or Cash payment records from reports or existing booking/payment history.
- Remove non-booking cash reconciliation/reporting data if it exists outside booking payment flows.
- Change voucher or membership discount rules.
- Add refunds or reconciliation reports beyond the callback data needed for booking payment state.
- Build a customer-facing payment method redesign unless required to expose the existing VNPay option correctly.

## Decisions

1. Externalize VNPay credentials with property placeholders.
   - Rationale: the supplied sandbox hash secret is sensitive and should be provided through environment variables such as `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAY_URL`, `VNPAY_RETURN_URL`, and `VNPAY_IPN_URL`.
   - Alternative considered: update `application.properties` with the raw sandbox secret. Rejected because committed secrets are risky even for sandbox.

2. Add a dedicated IPN route, for example `GET /api/payment/vnpay-ipn`.
   - Rationale: VNPay IPN is a server callback and should return a machine-readable response independent of browser redirects and flash messages.
   - Alternative considered: reuse `/vnpay-return`. Rejected because return URL behavior is browser-oriented and redirects to UI pages.

3. Share checksum parsing in `VNPayService`.
   - Rationale: return and IPN use the same signed parameter model and should avoid divergent hash logic.
   - Alternative considered: duplicate checksum validation in the controller. Rejected because it increases drift risk.

4. Confirm payment only through `BookingPaymentService`.
   - Rationale: existing service methods already manage booking status, payment status, ticket state, expiry checks, and loyalty side effects.
   - Alternative considered: mutate `Payment` directly in the VNPay controller. Rejected because it would bypass existing invariants.

5. Validate amount before confirming.
   - Rationale: VNPay sends `vnp_Amount` in VND x 100. The callback amount must match the pending payment amount or final discounted booking amount to prevent confirming a tampered or mismatched transaction.
   - Alternative considered: trust success response code only. Rejected because checksum validity alone does not prove the amount matches this booking.

6. Treat successful duplicate callbacks as idempotent success.
   - Rationale: return and IPN can both arrive, or VNPay may retry IPN. A completed payment for the same booking and matching transaction should not fail noisy or repeat side effects.
   - Alternative considered: reject all callbacks for non-pending payments. Rejected because VNPay retries expect stable responses.

7. Remove VietQR and Cash from Customer checkout and fallback behavior.
   - Rationale: the selected product direction is one online gateway, VNPay, so Customer checkout should not display VietQR or Cash, create new Customer payments with method `VietQR`/`Cash`, or silently fall back to `/payment/vietqr/{bookingId}` when VNPay handoff fails.
   - Alternative considered: keep VietQR or Cash as backup paths. Rejected because they create inconsistent payment handling, manual confirmation risk, and contradict the decision to standardize on VNPay.

8. Remove non-VNPay methods from Staff POS booking payment.
   - Rationale: Staff-created booking payments should follow the same authoritative VNPay confirmation path as Customer checkout, avoiding manual paid-state changes from cashier cash collection or QR transfer confirmation.
   - Alternative considered: keep Cash for Staff POS only. Rejected because the new decision explicitly removes the cash flow for both Staff and Customer.

9. Keep historical non-VNPay records readable.
   - Rationale: existing bookings/payments may still contain `Cash` or `VietQR`; reports, filters, and detail pages should not break while new transaction creation stops using those methods.
   - Alternative considered: migrate old payment methods to VNPay. Rejected because it would misrepresent how historical payments were actually collected.

## Risks / Trade-offs

- Public IPN route broadens the anonymous surface area -> Permit only the exact callback routes and require valid VNPay checksum for any state mutation.
- Existing `BookingPaymentService.confirmPayment` may not expose enough detail for idempotent callbacks -> Add a small callback-oriented method or result object if needed instead of overloading controller logic.
- Local testing needs a public URL for VNPay sandbox -> Document that local IPN testing needs tunneling or a deployed test host, while return URL can still be tested through localhost only when browser redirection originates locally.
- Current committed sandbox properties include a hash secret -> Replace raw values with placeholders and provide local example values outside tracked secrets.
- Existing VietQR routes/templates, Cash POS controls, and other non-VNPay POS controls may still be reachable directly -> Gate or retire new transaction access paths and update redirects/tests so no new Customer or Staff booking lands on a non-VNPay method.
- Staff POS may currently assume immediate paid booking after cashier collection -> Introduce a VNPay pending handoff/result flow or explicit unsupported-method rejection so POS does not mark payments completed before VNPay confirmation.

## Migration Plan

No database migration is required unless the implementation chooses to persist VNPay transaction identifiers for reconciliation. Deploy in three steps: first externalize configuration and preserve current VNPay return behavior, then add and test IPN, then remove Customer/Staff Cash and VietQR selection/fallbacks after VNPay handoff is verified. After deployment, provide VNPay with the public IPN URL and use the sandbox test case portal to validate IPN scenarios.

## Open Questions

- What public base URL should be used for VNPay sandbox return/IPN during SIT testing?
- Should VNPay transaction metadata such as `vnp_TransactionNo`, `vnp_BankCode`, and `vnp_PayDate` be persisted on `Payment` now, or only logged for this change?
