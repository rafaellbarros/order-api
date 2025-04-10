package br.com.rafaellbarros.order.service;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.factory.OrderFactory;
import br.com.rafaellbarros.order.logger.OrderLogger;
import br.com.rafaellbarros.order.repository.OrderItemRepository;
import br.com.rafaellbarros.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderFactory orderFactory;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderLogger orderLogger;

    public Order createOrder(final Order request) {

        validateDuplicated(request);

        var savedItems = orderItemRepository.saveAll(request.getItems());
        orderLogger.sevedItems(savedItems);
        request.setItems(savedItems);

        var order = orderFactory.createFrom(request);
        var savedOrder = orderRepository.save(order);
        orderLogger.saved(savedOrder);

        return savedOrder;
    }


    public List<Order> createOrders(final List<Order> requests) {

        if (CollectionUtils.isEmpty(requests)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lista de pedidos não pode estar vazia");
        }

        List<Order> validOrders = requests.stream()
                .map(this::createFromIfValid)
                .flatMap(Optional::stream)
                .toList();

        if (validOrders.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nenhum pedido válido para processar");
        }

        var savedOrders = orderRepository.saveAll(validOrders);
        orderLogger.savedOrders(savedOrders);
        return savedOrders;
    }


    public Optional<Order> createFromIfValid(final Order request) {
        try {
            if (orderRepository.findByExternalId(request.getExternalId()).isPresent()) {
                orderLogger.duplicatedIgnored(request.getExternalId());
                return Optional.empty();
            }

            final var savedItems = orderItemRepository.saveAll(request.getItems());
            orderLogger.sevedItems(savedItems);

            request.setItems(savedItems);

            final var orderCreated = orderFactory.createFrom(request);

            return Optional.of(orderCreated);

        } catch (ResponseStatusException ex) {
            orderLogger.invalidIgnored(request.getExternalId(), ex.getReason());
            return Optional.empty();

        } catch (RuntimeException ex) {
            orderLogger.invalidIgnored(request.getExternalId(), "Erro inesperado: " + ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }


    public Optional<Order> getOrderByExternalId(final String id) {
        return orderRepository.findByExternalId(id)
                .map(order -> {
                    orderLogger.searchByExternalId(id);
                    orderLogger.found(order.getId());
                    return order;
                });
    }


    public List<Order> getOrdersByStatus(final OrderStatus status) {
        orderLogger.searchByStatus(status);
        return orderRepository.findByStatus(status);
    }

    private void validateDuplicated(Order request) {
        orderRepository.findByExternalId(request.getExternalId())
                .ifPresent(o -> {
                    orderLogger.duplicated(request.getExternalId());
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido duplicado externalId: " + request.getExternalId());
                });
    }

}