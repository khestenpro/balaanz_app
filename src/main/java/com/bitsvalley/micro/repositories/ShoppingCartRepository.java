package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.OrderItem;
import com.bitsvalley.micro.domain.ShoppingCart;
import org.springframework.data.repository.CrudRepository;

public interface ShoppingCartRepository extends CrudRepository<ShoppingCart, Long> {

}
