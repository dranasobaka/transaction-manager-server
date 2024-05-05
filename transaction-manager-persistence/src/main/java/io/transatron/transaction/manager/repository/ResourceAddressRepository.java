package io.transatron.transaction.manager.repository;

import io.transatron.transaction.manager.entity.ResourceAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceAddressRepository extends JpaRepository<ResourceAddressEntity, UUID> {

    Optional<ResourceAddressEntity> findByAddress(byte[] address);

}
