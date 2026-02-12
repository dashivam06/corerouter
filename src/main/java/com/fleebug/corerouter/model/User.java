package com.fleebug.corerouter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fleebug.corerouter.enums.user.UserRole;
import com.fleebug.corerouter.enums.user.UserStatus;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @NotBlank(message = "Username is required")
    @Column(nullable = false, length = 50)
    private String username;

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String profileImage;

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid balance format")
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column
    private LocalDateTime lastLogin;

    @Column(nullable = true)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private boolean emailSubscribed = true;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}
