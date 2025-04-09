package br.com.rafaellbarros.order.repository;

import br.com.rafaellbarros.order.domain.OrderItem;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderItemRepository extends MongoRepository<OrderItem, String> {
}
