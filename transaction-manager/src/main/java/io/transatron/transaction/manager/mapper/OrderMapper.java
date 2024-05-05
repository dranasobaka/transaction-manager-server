package io.transatron.transaction.manager.mapper;

import io.transatron.transaction.manager.controller.dto.OrderDto;
import io.transatron.transaction.manager.controller.dto.TransactionDto;
import io.transatron.transaction.manager.domain.Order;
import io.transatron.transaction.manager.domain.Transaction;
import org.mapstruct.Mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface OrderMapper {

    OrderDto toDto(Order model);

    TransactionDto toDto(Transaction model);

}
