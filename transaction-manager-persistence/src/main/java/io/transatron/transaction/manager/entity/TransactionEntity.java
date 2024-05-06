package io.transatron.transaction.manager.entity;

import io.transatron.transaction.manager.domain.TransactionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.sql.Timestamp;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.TemporalType.TIMESTAMP;

@Data
@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @Column
    @UuidGenerator
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "tx_id", nullable = false)
    private String txId;

    @Column(name = "to_address", nullable = false, columnDefinition = "BYTEA")
    private byte[] toAddress;
    
    @Column(name = "tx_amount")
    private Long txAmount;      // in TRX

    @Column(name = "rawTransaction", nullable = false, columnDefinition = "TEXT")
    private String rawTransaction;

    @Enumerated(STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Temporal(TIMESTAMP)
    @Column(name = "created_at")
    private Timestamp createdAt;

}
