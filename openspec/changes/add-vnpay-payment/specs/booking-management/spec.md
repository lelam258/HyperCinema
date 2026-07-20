# Booking Management Specification

## ADDED Requirements

### Requirement: Booking checkout supports verified VNPay online payment

The system MUST allow a booking checkout to create a pending VNPay online payment and redirect the customer to a signed VNPay sandbox payment URL.

#### Scenario: Customer starts a VNPay payment

- **GIVEN** an authenticated Customer submits a valid booking checkout with VNPay as the selected payment method
- **WHEN** the system creates the booking and payment
- **THEN** the payment MUST be created with method `VNPay`, status `Pending`, and amount equal to the final payable booking total
- **AND** the payment MUST have the configured pending payment expiry deadline
- **AND** the system MUST redirect the customer to a VNPay payment URL signed with the configured hash secret

#### Scenario: VNPay configuration is incomplete

- **GIVEN** VNPay payment is selected during checkout
- **AND** the VNPay terminal code, hash secret, payment URL, or return URL is not configured
- **WHEN** the system attempts to create the VNPay payment URL
- **THEN** the system MUST reject the checkout with a user-facing payment configuration error
- **AND** the system MUST NOT expose secret configuration values in the error
- **AND** the system MUST NOT redirect the customer to a VietQR payment page as a fallback

### Requirement: Customer checkout uses VNPay instead of VietQR or Cash

The system MUST make VNPay the Customer online payment method and must not create new Customer checkout payments through VietQR or Cash.

#### Scenario: Customer views booking checkout payment methods

- **GIVEN** an authenticated Customer is completing booking checkout
- **WHEN** the payment method choices are displayed
- **THEN** the checkout MUST offer VNPay for online payment
- **AND** the checkout MUST NOT offer VietQR as a Customer payment method
- **AND** the checkout MUST NOT offer Cash as a Customer payment method

#### Scenario: Customer checkout submits without an explicit supported method

- **GIVEN** an authenticated Customer submits a valid booking checkout
- **AND** the submitted payment method is missing, blank, or no longer supported for Customer checkout
- **WHEN** the system prepares the online payment handoff
- **THEN** the system MUST either use the supported VNPay path according to the current checkout default or reject the checkout with a user-facing payment method error
- **AND** the system MUST NOT create a new Customer payment with method `VietQR`
- **AND** the system MUST NOT create a new Customer payment with method `Cash`
- **AND** the system MUST NOT redirect the customer to `/payment/vietqr/{bookingId}`

#### Scenario: Customer opens a retired VietQR payment URL

- **GIVEN** VietQR has been removed from Customer checkout
- **WHEN** a Customer requests a Customer-facing VietQR payment page directly
- **THEN** the system MUST prevent a new Customer VietQR payment confirmation flow
- **AND** the customer MUST see a safe payment unavailable, retry, or booking detail response

#### Scenario: Customer attempts to use Cash payment

- **GIVEN** Cash has been removed from Customer checkout
- **WHEN** a Customer submits or requests a Cash payment flow for a booking
- **THEN** the system MUST reject the Cash payment method for new Customer booking payments
- **AND** the system MUST NOT mark the booking/payment as paid through manual Cash confirmation

### Requirement: VNPay return callback updates booking payment safely

The system MUST verify VNPay browser return parameters before updating booking or payment state.

#### Scenario: VNPay return confirms a pending payment

- **GIVEN** a booking has a pending VNPay payment
- **AND** VNPay redirects to the return URL with a valid secure hash
- **AND** `vnp_ResponseCode` and `vnp_TransactionStatus` are both `00`
- **AND** `vnp_TxnRef` identifies the booking
- **AND** `vnp_Amount` matches the pending payment amount using VNPay's x100 amount format
- **WHEN** the return callback is processed before payment expiry
- **THEN** the system MUST mark the payment as `Completed`
- **AND** the system MUST update booking and ticket state according to existing successful payment rules
- **AND** the customer MUST see a successful VNPay payment result

#### Scenario: VNPay return has invalid checksum

- **GIVEN** VNPay redirects to the return URL with missing or invalid secure hash
- **WHEN** the return callback is processed
- **THEN** the system MUST reject the callback
- **AND** the system MUST NOT confirm or fail any booking/payment based on that callback

#### Scenario: VNPay return reports failed or cancelled payment

- **GIVEN** a booking has a pending VNPay payment
- **AND** VNPay redirects to the return URL with a valid secure hash
- **AND** `vnp_ResponseCode` or `vnp_TransactionStatus` is not `00`
- **WHEN** the return callback is processed
- **THEN** the system MUST mark the VNPay payment as failed or keep it pending according to the existing retry policy
- **AND** the customer MUST see a clear failure or retry message

#### Scenario: VNPay return arrives after payment expiry

- **GIVEN** a VNPay payment was expired by the booking payment timeout process
- **WHEN** a later successful VNPay return callback arrives for that booking
- **THEN** the system MUST NOT re-confirm the expired booking/payment
- **AND** the customer MUST see a message that the payment window has expired

### Requirement: VNPay IPN updates booking payment through server callback

The system MUST expose a public VNPay IPN endpoint that validates signed server-to-server callbacks and returns VNPay-compatible response payloads.

#### Scenario: VNPay IPN confirms a pending payment

- **GIVEN** a booking has a pending VNPay payment
- **AND** VNPay sends an IPN request with a valid secure hash
- **AND** `vnp_ResponseCode` and `vnp_TransactionStatus` are both `00`
- **AND** `vnp_TxnRef` identifies the booking
- **AND** `vnp_Amount` matches the pending payment amount using VNPay's x100 amount format
- **WHEN** the IPN callback is processed before payment expiry
- **THEN** the system MUST mark the payment as `Completed`
- **AND** the system MUST update booking and ticket state according to existing successful payment rules
- **AND** the IPN response MUST indicate successful processing to VNPay

#### Scenario: VNPay IPN is duplicated after successful processing

- **GIVEN** a VNPay payment was already completed by a previous valid return or IPN callback
- **WHEN** VNPay sends the same valid successful IPN callback again
- **THEN** the system MUST NOT duplicate loyalty points, tickets, food order transitions, or other side effects
- **AND** the IPN response MUST indicate the order was already confirmed or successfully handled

#### Scenario: VNPay IPN has invalid checksum

- **GIVEN** VNPay sends an IPN request with missing or invalid secure hash
- **WHEN** the IPN callback is processed
- **THEN** the system MUST NOT update booking or payment state
- **AND** the IPN response MUST indicate checksum validation failure

#### Scenario: VNPay IPN amount does not match booking payment

- **GIVEN** a booking has a pending VNPay payment
- **AND** VNPay sends an IPN request with a valid secure hash
- **AND** `vnp_Amount` does not match the pending payment amount
- **WHEN** the IPN callback is processed
- **THEN** the system MUST NOT confirm the booking/payment
- **AND** the IPN response MUST indicate invalid amount

#### Scenario: VNPay IPN references an unknown booking

- **GIVEN** VNPay sends an IPN request with a valid secure hash
- **AND** `vnp_TxnRef` does not identify an existing booking/payment
- **WHEN** the IPN callback is processed
- **THEN** the system MUST NOT create a booking/payment
- **AND** the IPN response MUST indicate order not found

## MODIFIED Requirements

### Requirement: Booking pricing breakdown remains visible and consistent

The system MUST show a consistent pricing breakdown for bookings that use voucher and membership discounts.

#### Scenario: User views payment information after discounted booking creation

- **GIVEN** a booking was created with voucher or membership discounts
- **WHEN** the user views the VNPay handoff, payment result, booking detail, or payment detail for that booking
- **THEN** the system MUST show the original subtotal, applied voucher discount if any, applied membership discount if any, and final payable total
- **AND** the VNPay payment amount MUST match the final payable total
