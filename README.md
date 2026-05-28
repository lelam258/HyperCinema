# HyperCinema

## Authentication seed account

Login uses Spring Security with BCrypt password hashes stored in the `User.password_hash` column. Seed or create accounts with an encoded password, not plaintext.

Minimal SQL shape:

```sql
INSERT INTO Role (name) VALUES ('Customer');

INSERT INTO User (username, password_hash, full_name, email, phone, role_id, status, email_verified)
VALUES ('member', '<bcrypt-hash>', 'Hyper Member', 'member@example.com', '0901234567', 1, 'Active', 1);
```

Generate `<bcrypt-hash>` through the application's `PasswordEncoder` bean or another BCrypt-compatible tool before inserting the row.

## Admin user management and account email

Admin user management supports account statuses `Active`, `Inactive`, and `Banned`. Only `Active` users can authenticate through the existing login flow.

Account-created, password-reset, and email-verification messages use Spring Mail plus Thymeleaf templates. Configure these environment variables before sending real email:

```properties
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-smtp-user
SPRING_MAIL_PASSWORD=your-smtp-password
HYPERCINEMA_MAIL_FROM=no-reply@your-domain.example
HYPERCINEMA_APP_BASE_URL=https://your-domain.example
```
