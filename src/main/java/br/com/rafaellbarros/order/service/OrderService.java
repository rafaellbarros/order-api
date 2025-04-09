package br.com.rafaellbarros.order.service;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.repository.OrderRepository;
import br.com.rafaellbarros.order.service.helper.OrderFactory;
import br.com.rafaellbarros.order.service.helper.OrderLogger;
import br.com.rafaellbarros.order.service.helper.OrderValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderValidator validator;
    private final OrderFactory factory;
    private final OrderLogger orderLogger;

    public Order receive(final Order request) {
        validator.validate(request);

        orderRepository.findByExternalId(request.getExternalId())
                .ifPresent(o -> {
                    orderLogger.logDuplicate(request.getExternalId());
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido duplicado externalId: " + request.getExternalId());
                });

        Order order = factory.createOrder(request);
        Order saved = orderRepository.save(order);
        orderLogger.logSavedOrder(order);
        return saved;
    }

    public List<Order> receiveAll(@Valid List<Order> listRequest) {

        if (CollectionUtils.isEmpty(listRequest)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lista de pedidos não pode estar vazia");
        }

        List<Order> validOrders = listRequest.stream()
                .map(request -> {
                    try {
                        validator.validate(request);
                        if (orderRepository.findByExternalId(request.getExternalId()).isPresent()) {
                            orderLogger.logDuplicateIgnored(request.getExternalId());
                            return null;
                        }
                        return factory.createOrder(request);
                    } catch (ResponseStatusException ex) {
                        orderLogger.logInvalidIgnored(request.getExternalId(), ex.getReason());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        if (validOrders.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nenhum pedido válido para processar");
        }

        List<Order> savedOrders = orderRepository.saveAll(validOrders);
        log.info("{} pedido(s) salvo(s) com sucesso.", savedOrders.size());
        return savedOrders;
    }

    public Optional<Order> getOrderByEsternalId(final String id) {
        orderLogger.logSearchByExternalId(id);
        return orderRepository.findByExternalId(id)
                .map(order -> {
                    orderLogger.logFound(order.getId());
                    return order;
                });
    }

    public List<Order> getOrdersByStatus(final OrderStatus status) {
        log.info("Buscando pedidos com status: {}", status);
        List<Order> orders = orderRepository.findByStatus(status);
        log.info("{} pedido(s) encontrado(s) com status {}", orders.size(), status);
        return orders;
    }
}