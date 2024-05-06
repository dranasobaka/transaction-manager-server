package io.transatron.transaction.manager.entity;

import io.transatron.transaction.manager.domain.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.TemporalType.TIMESTAMP;

@Data
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column
    @UuidGenerator
    private UUID id;

    @Column(name = "wallet_address", columnDefinition = "BYTEA")
    private byte[] walletAddress;

    @Temporal(TIMESTAMP)
    @Column(name = "fulfill_from")
    private Timestamp fulfillFrom;

    @Temporal(TIMESTAMP)
    @Column(name = "fulfill_to")
    private Timestamp fulfillTo;

    @Column(name = "own_energy")
    private Long ownEnergy;

    @Column(name = "external_energy")
    private Long externalEnergy;

    @Column(name = "own_bandwidth")
    private Long ownBandwidth;

    @Enumerated(STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Temporal(TIMESTAMP)
    @Column(name = "created_at")
    private Timestamp createdAt;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private List<TransactionEntity> transactions;

}
