package br.com.rafaellbarros.order.factory;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.logger.OrderLogger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Component
@RequiredArgsConstructor
public class OrderFactory {

    private final OrderLogger logger;

    public Order createFrom(final Order request) {
        var order = Order.builder()
                .externalId(request.getExternalId())
                .status(OrderStatus.RECEIVED)
                .createdAt(LocalDateTime.now())
                .items(request.getItems())
                .traceId(UUID.randomUUID().toString())
                .processingMessage("Order received successfully")
                .build();
        logger.created(order);
        return order;
    }

}
