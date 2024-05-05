package io.transatron.transaction.manager.logic.mapper;

import io.transatron.transaction.manager.domain.Order;
import io.transatron.transaction.manager.domain.Transaction;
import io.transatron.transaction.manager.entity.OrderEntity;
import io.transatron.transaction.manager.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface OrderMapper {

    @Mappings({
        @Mapping(target = "walletAddress", expression = "java(io.transatron.transaction.manager.web3.utils.TronAddressUtils.toBase58(entity.getWalletAddress()))")
    })
    Order toModel(OrderEntity entity);

    @Mappings({
        @Mapping(source = "txId", target = "id"),
        @Mapping(target = "to", expression = "java(io.transatron.transaction.manager.web3.utils.TronAddressUtils.toBase58(entity.getToAddress()))"),
        @Mapping(source = "txAmount", target = "amount")
    })
    Transaction toModel(TransactionEntity entity);

}
