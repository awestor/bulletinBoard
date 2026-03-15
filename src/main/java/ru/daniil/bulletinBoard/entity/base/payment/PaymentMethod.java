package ru.daniil.bulletinBoard.entity.base.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_payment_method")
@Data
@Builder
@AllArgsConstructor
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String type;

    @Column
    private String description;

    @Column
    private LocalDateTime createdAt;

    public PaymentMethod() {
    }

    public PaymentMethod(String type, String description) {
        this();
        this.type = type;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
