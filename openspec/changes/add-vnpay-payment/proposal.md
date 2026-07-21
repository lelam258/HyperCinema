## Why

HyperCinema already has a VNPay payment path, but the current integration is incomplete for the newly provided merchant sandbox account. It needs to use the new test merchant configuration, avoid committing payment secrets, and expose a VNPay IPN endpoint so VNPay can update payment state through server-to-server notification instead of relying only on the browser return URL.

## What Changes

- Update VNPay sandbox configuration to support the provided terminal code, payment URL, return URL, and hash secret through environment-backed application properties.
- Add a public VNPay IPN endpoint for server-to-server payment status updates and prepare the IPN URL to send to VNPay.
- Verify both VNPay return and IPN callbacks by checksum, response code, transaction status, booking reference, and payable amount before updating booking/payment state.
- Make VNPay payment creation use the final discounted booking amount and existing booking payment timeout.
- Keep pending bookings unchanged until a valid successful VNPay return/IPN confirms the payment.
- Mark failed or cancelled VNPay responses safely without creating duplicate side effects or re-confirming expired payments.
- Make VNPay the only new booking payment path for both Customer checkout and Staff POS booking flows.
- Remove VietQR, Cash, and any non-VNPay Staff POS payment method from booking payment flows for new transactions.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `booking-management`: Booking checkout supports VNPay online payment with verified return and IPN callbacks that update booking/payment status consistently.
- `booking-management`: Customer booking checkout no longer offers or falls back to VietQR or Cash; non-VNPay customer payment handoff errors must be explicit.
- `booking-payment-timeout`: Pending online payments must remain compatible with existing expiry rules and reject callbacks that arrive after expiry.
- `staff-pos-workspace`: Staff POS booking payment no longer offers Cash, card, VietQR, QR transfer, or other non-VNPay methods for new POS transactions and must hand off supported paid bookings through VNPay.

## Impact

- Affected configuration: `vnpay.*` properties, local sandbox environment variables, and deployment environment variables.
- Affected backend/API: VNPay payment URL generation, VNPay return handler, new VNPay IPN handler, payment confirmation/failure service paths, and booking/POS payment-method validation.
- Affected UI: customer booking payment controls, Staff POS payment controls, VietQR payment page links, Cash labels/actions, and payment-result messaging.
- Affected security: public callback routes, HMAC-SHA512 validation, no hardcoded hash secret or merchant admin credentials in committed files.
- Affected tests: VNPay URL signing, return handling, IPN response contract, invalid checksum rejection, amount mismatch rejection, idempotent repeated callback handling, expired payment handling, and absence of Customer/Staff non-VNPay payment creation.
