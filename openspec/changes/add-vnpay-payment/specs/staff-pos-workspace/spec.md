# Staff POS Workspace Specification

## MODIFIED Requirements

### Requirement: POS payment method selection

The system SHALL allow staff to use VNPay as the only supported booking payment method for the current POS transaction.

#### Scenario: Staff chooses a POS booking payment method

- **GIVEN** a Staff user has a valid POS transaction
- **WHEN** the user chooses a payment method
- **THEN** the system SHALL allow VNPay as the supported booking payment method
- **AND** the system SHALL NOT offer Cash, card, VietQR, QR transfer, or any other non-VNPay method for new POS booking payments
- **AND** the submitted form SHALL include `VNPay` as the selected payment method when the transaction is submitted for payment

#### Scenario: Staff has not chosen a supported payment method

- **GIVEN** a Staff user selected seats
- **AND** no supported VNPay payment method is selected
- **WHEN** the user views the payment slip
- **THEN** the final payment action SHALL remain disabled or show an explicit unsupported payment method error

### Requirement: POS transaction submission

The system SHALL submit POS transactions through server-side booking/payment validation and VNPay payment handoff.

#### Scenario: Staff submits a valid ticket transaction

- **GIVEN** an authenticated Staff user selected a showtime in the user's branch
- **AND** the user selected at least one available seat
- **AND** the transaction uses VNPay as the payment method
- **WHEN** the user submits the POS transaction
- **THEN** the system SHALL create the booking using authoritative server-side prices and availability checks
- **AND** the system SHALL apply valid F&B selections and voucher code when present
- **AND** the system SHALL create a pending VNPay payment for the final payable total
- **AND** the system SHALL hand off the payment to VNPay instead of marking the booking as paid immediately

#### Scenario: Staff submits a removed payment method

- **GIVEN** non-VNPay methods have been removed from POS booking payments
- **WHEN** a Staff user submits a POS booking transaction with method `Cash`, `Card`, `VietQR`, a QR transfer alias, or any other non-VNPay value
- **THEN** the system SHALL reject the transaction with a clear unsupported payment method error
- **AND** the system SHALL NOT create a completed payment for that booking
- **AND** the system SHALL NOT mark tickets, food orders, or booking state as paid by that unsupported method
