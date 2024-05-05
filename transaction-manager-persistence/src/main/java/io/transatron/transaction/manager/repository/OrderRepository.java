package io.transatron.transaction.manager.repository;

import io.transatron.transaction.manager.domain.OrderStatus;
import io.transatron.transaction.manager.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    @Query("SELECT o FROM OrderEntity o WHERE o.walletAddress = ?1 ORDER BY o.createdAt LIMIT 1")
    Optional<OrderEntity> findLastByWalletAddress(byte[] walletAddress);

    List<OrderEntity> findAllByWalletAddress(byte[] walletAddress);

    @Query("SELECT o FROM OrderEntity o WHERE o.fulfillFrom BETWEEN ?1 AND ?2 ORDER BY o.fulfillFrom LIMIT 1")
    List<OrderEntity> findOrdersFulfillingWithinTimeRange(Timestamp from, Timestamp to);

    List<OrderEntity> findAllByWalletAddressAndStatusIn(byte[] walletAddress, Set<OrderStatus> statuses);
}
