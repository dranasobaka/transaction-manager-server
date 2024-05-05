package io.transatron.transaction.manager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Data
@Entity
@Table(name = "resource_addresses")
public class ResourceAddressEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "address", nullable = false)
    private byte[] address;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "manager_address")
    private byte[] managerAddress;

    @Column(name = "permission_id")
    private Integer permissionId;

    @Column(name = "active", nullable = false)
    private boolean active;

}
