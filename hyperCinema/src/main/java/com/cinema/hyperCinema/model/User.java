package com.cinema.hyperCinema.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "User")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    /**
     * Chi nhánh mà người dùng thuộc về.
     *
     * <p>NULL với Customer hoặc Manager chưa được gán chi nhánh; bắt buộc với
     * Staff đã được gán. Trace requirement: REQ 1.4 — xem
     * {@code .kiro/specs/branch-management/design.md} mục A.4.2.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    /**
     * Quản lý trực tiếp của người dùng (self-reference).
     *
     * <p>NULL với Admin / Manager / Customer; bắt buộc với Staff đã được gán
     * vào một chi nhánh. Trace requirement: REQ 1.4 — xem
     * {@code .kiro/specs/branch-management/design.md} mục A.4.2.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(nullable = false, length = 50)
    private String status = "Active";

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "forgot_password_code", length = 20)
    private String forgotPasswordCode;

    @Column(name = "forgot_password_code_expire")
    private LocalDateTime forgotPasswordCodeExpire;

    @Column(name = "active_code", length = 20)
    private String activeCode;

    @Column(name = "active_code_expire")
    private LocalDateTime activeCodeExpire;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
