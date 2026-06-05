package com.kpiso.api.modules.expense;

import com.kpiso.api.modules.house.House;
import com.kpiso.api.modules.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_id", nullable = false)
    private User paidBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "expense_participants",
            joinColumns = @JoinColumn(name = "expense_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_exact_splits", joinColumns = @JoinColumn(name = "expense_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "amount", precision = 10, scale = 2)
    private java.util.Map<UUID, BigDecimal> exactSplits;

    @Column(nullable = false)
    private boolean settled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}