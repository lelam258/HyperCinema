package com.cinema.hyperCinema.service.impl;

import com.cinema.hyperCinema.dto.auth.ChangePasswordRequestDTO;
import com.cinema.hyperCinema.dto.auth.PasswordPolicy;
import com.cinema.hyperCinema.dto.auth.RegisterRequestDTO;
import com.cinema.hyperCinema.dto.auth.ResetPasswordRequestDTO;
import com.cinema.hyperCinema.model.Role;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.repository.RoleRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.AuthService;
import com.cinema.hyperCinema.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int CODE_EXPIRE_MINUTES = 15;
    private static final int ACTIVE_CODE_EXPIRE_HOURS = 24;
    private static final String CUSTOMER_ROLE_NAME = "Customer";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public User registerCustomer(RegisterRequestDTO dto) {
        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim();
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Ten dang nhap da ton tai");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email da duoc su dung");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mat khau xac nhan khong khop");
        }

        Role customerRole = getOrCreateCustomerRole();

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        newUser.setFullName(dto.getFullName().trim());
        newUser.setPhone(phone);
        newUser.setRole(customerRole);
        newUser.setStatus("Inactive");
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        newUser.setEmailVerified(false);
        newUser.setActiveCode(generateCode());
        newUser.setActiveCodeExpire(LocalDateTime.now().plusHours(ACTIVE_CODE_EXPIRE_HOURS));

        User savedUser = userRepository.save(newUser);
        try {
            emailService.sendActivationCode(savedUser.getEmail(), savedUser.getActiveCode());
        } catch (RuntimeException e) {
            log.error("Could not send activation email to {}", savedUser.getEmail(), e);
        }
        return savedUser;
    }

    @Override
    @Transactional
    public void requestForgotPassword(String email) {
        String normalizedEmail = email == null ? "" : email.trim();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            return;
        }

        user.setForgotPasswordCode(generateCode());
        user.setForgotPasswordCodeExpire(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        userRepository.save(user);

        emailService.sendForgotPasswordCode(user.getEmail(), user.getForgotPasswordCode());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO dto) {
        String email = dto.getEmail() == null ? "" : dto.getEmail().trim();
        String code = dto.getCode() == null ? "" : dto.getCode().trim();
        String password = dto.getPassword();

        if (!PasswordPolicy.isValid(password)) {
            throw new IllegalArgumentException(PasswordPolicy.MIN_LENGTH_MESSAGE);
        }
        if (!password.equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mat khau xac nhan khong khop");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email khong ton tai trong he thong"));

        if (user.getForgotPasswordCode() == null
                || !user.getForgotPasswordCode().equals(code)
                || user.getForgotPasswordCodeExpire() == null
                || user.getForgotPasswordCodeExpire().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ma xac nhan khong dung hoac da het han");
        }
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mat khau moi phai khac mat khau hien tai");
        }

        user.setPasswordHash(passwordEncoder.encode(password));
        user.setForgotPasswordCode(null);
        user.setForgotPasswordCodeExpire(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(User actor, ChangePasswordRequestDTO dto) {
        if (actor == null || actor.getUserId() == null) {
            throw new IllegalArgumentException("Nguoi dung khong hop le");
        }
        User user = userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Nguoi dung khong ton tai"));
        String currentPassword = dto.getCurrentPassword();
        String newPassword = dto.getNewPassword();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mat khau hien tai khong dung");
        }
        if (!PasswordPolicy.isValid(newPassword)) {
            throw new IllegalArgumentException(PasswordPolicy.MIN_LENGTH_MESSAGE);
        }
        if (!newPassword.equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mat khau xac nhan khong khop");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mat khau moi phai khac mat khau hien tai");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateAccount(String email, String code) {
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Email khong ton tai trong he thong"));

        if (user.getActiveCode() == null
                || !user.getActiveCode().equals(code)
                || user.getActiveCodeExpire() == null
                || user.getActiveCodeExpire().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ma kich hoat khong dung hoac da het han");
        }

        user.setEmailVerified(true);
        user.setStatus("Active");
        user.setActiveCode(null);
        user.setActiveCodeExpire(null);
        userRepository.save(user);
    }

    private Role getOrCreateCustomerRole() {
        return roleRepository.findByNameIgnoreCase(CUSTOMER_ROLE_NAME)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(CUSTOMER_ROLE_NAME);
                    return roleRepository.save(role);
                });
    }

    private String generateCode() {
        return String.valueOf(100000 + secureRandom.nextInt(900000));
    }
}
