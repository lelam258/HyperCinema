## 1. Configuration

- [x] 1.1 Replace committed VNPay sandbox values in `application.properties` with environment-backed placeholders and safe defaults.
- [x] 1.2 Configure the new sandbox terminal code through local/deployment environment variables.
- [x] 1.3 Keep `vnpay.pay-url` pointed at the VNPay sandbox payment URL for test mode.
- [x] 1.4 Add a configurable `vnpay.ipn-url` or document the generated public IPN URL to send to VNPay.
- [x] 1.5 Ensure merchant admin username/password and hash secret are not committed to source files.

## 2. VNPay Service Contract

- [x] 2.1 Refactor callback verification into reusable methods for signed VNPay return and IPN parameters.
- [x] 2.2 Validate callback success using both `vnp_ResponseCode` and `vnp_TransactionStatus`.
- [x] 2.3 Parse `vnp_TxnRef` into the booking/payment reference and reject invalid references safely.
- [x] 2.4 Validate `vnp_Amount` against the pending payment amount or final discounted booking amount using VNPay's x100 amount format.
- [x] 2.5 Add focused unit tests for URL signing, callback checksum validation, invalid checksum rejection, and amount validation.

## 3. Return URL Handling

- [x] 3.1 Keep `GET /vnpay-return` and `/api/payment/vnpay-return` as browser return routes.
- [x] 3.2 Confirm successful pending payments through `BookingPaymentService` after checksum and amount validation.
- [x] 3.3 Fail or leave pending payments according to existing rules when VNPay returns cancelled/failed status.
- [x] 3.4 Redirect customers back to the existing booking result or retry flow with clear success/failure messages.
- [x] 3.5 Verify expired payments cannot be confirmed by late VNPay return callbacks.

## 4. IPN Handling

- [x] 4.1 Add a public VNPay IPN route such as `GET /api/payment/vnpay-ipn`.
- [x] 4.2 Return VNPay-compatible response codes/messages for success, invalid checksum, missing booking, amount mismatch, already processed, and unexpected errors.
- [x] 4.3 Make successful IPN confirmation idempotent when the same booking/payment is already completed.
- [x] 4.4 Ensure failed or cancelled IPN responses do not create duplicate side effects.
- [x] 4.5 Add MVC tests for valid IPN success, duplicate IPN, invalid checksum, amount mismatch, and unknown booking reference.

## 5. Booking Payment Flow

- [x] 5.1 Confirm customer checkout can choose VNPay and receive a signed sandbox payment URL.
- [x] 5.2 Ensure VNPay payment records use method `VNPay`, status `Pending`, final discounted amount, and the existing payment expiry deadline.
- [x] 5.3 Remove VietQR and Cash from Customer checkout payment method controls and submitted default/fallback behavior.
- [x] 5.4 Confirm booking detail and payment history show VNPay payment status consistently after return/IPN updates.
- [x] 5.5 Update Customer booking creation so online checkout creates VNPay payments or returns an explicit payment handoff error instead of redirecting to `/payment/vietqr/{bookingId}` or creating `Cash`.
- [x] 5.6 Retire or block Customer-facing VietQR and Cash payment entry points.
- [x] 5.7 Update Customer-facing copy and error messages so they no longer instruct users to choose VietQR or Cash when VNPay is unavailable.
- [x] 5.8 Remove Cash, card, VietQR, QR transfer, and any other non-VNPay choices from Staff POS booking payment controls.
- [x] 5.9 Update Staff POS booking submission so new POS bookings create pending VNPay payments and hand off to VNPay, or reject unsupported payment methods without creating paid non-VNPay payments.
- [x] 5.10 Keep historical `Cash` and `VietQR` payment records readable in booking/payment detail, list, and reports while preventing new creation through booking flows.

## 6. Verification

- [ ] 6.1 Run focused backend tests for VNPay service, controller callbacks, and booking payment state transitions.
- [ ] 6.2 Run application build or test suite required by the project.
- [ ] 6.3 Manually test the VNPay sandbox card flow with the provided NCB test card data.
- [ ] 6.4 Manually test VNPay SIT IPN cases after a public IPN URL is available.
- [x] 6.5 Record the exact IPN URL that should be sent to VNPay for sandbox verification.
- [x] 6.6 Add/update regression coverage that Customer checkout never falls back to VietQR or Cash when VNPay is selected or when VNPay configuration is incomplete.
- [ ] 6.7 Add/update regression coverage that Customer booking pricing breakdown remains visible through VNPay handoff/result flows after VietQR/Cash removal.
- [x] 6.8 Add/update regression coverage that Staff POS cannot create new booking payments with method `Cash`, `Card`, `VietQR`, QR transfer aliases, or any non-VNPay value.
- [ ] 6.9 Manually submit a Staff POS booking and confirm the only supported payment path is VNPay.

IPN URL to send to VNPay for the sandbox test host: `${VNPAY_IPN_URL}`. By default this resolves to `${app.base-url}/api/payment/vnpay-ipn`.
