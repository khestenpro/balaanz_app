package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.OrderItem;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderItemRepository extends CrudRepository<OrderItem, Long> {
  List<OrderItem> findItemsByShoppingCartId(Long id);
}
