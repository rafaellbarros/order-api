package br.com.rafaellbarros.order.service;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderItem;
import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.repository.OrderItemRepository;
import br.com.rafaellbarros.order.repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;

    public Order receive(final Order request) {
        validateRequest(request);

        if (orderRepository.findByExternalId(request.getExternalId()).isPresent()) {
            log.warn("Pedido duplicado externalId: {}", request.getExternalId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido duplicado externalId: " + request.getExternalId());
        }

        Order order = buildOrder(request);
        Order saved = orderRepository.save(order);
        log.info("[{}] Pedido salvo: {}", order.getTraceId(), saved.getExternalId());
        return saved;
    }

    public List<Order> receiveAll(@Valid List<Order> listRequest) {
        if (listRequest == null || listRequest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lista de pedidos não pode estar vazia");
        }

        List<Order> validOrders = new ArrayList<>();

        for (Order request : listRequest) {
            try {
                validateRequest(request);
                if (orderRepository.findByExternalId(request.getExternalId()).isPresent()) {
                    log.warn("Pedido duplicado ignorado externalId: {}", request.getExternalId());
                    continue;
                }
                validOrders.add(buildOrder(request));
            } catch (ResponseStatusException ex) {
                log.warn("Pedido inválido ignorado externalId: {} - Motivo: {}", request.getExternalId(), ex.getReason());
            }
        }

        if (validOrders.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nenhum pedido válido para processar");
        }

        List<Order> savedOrders = orderRepository.saveAll(validOrders);
        log.info("{} pedidos salvos com sucesso.", savedOrders.size());
        return savedOrders;
    }

    public Optional<Order> getOrderByEsternalId(final String id) {
        log.info("🔍 Buscando pedido pelo externalId: {}", id);
        Optional<Order> order = orderRepository.findByExternalId(id);
        if (order.isPresent()) {
            log.info("✅ Pedido encontrado: {}", order.get().getId());
        } else {
            log.warn("⚠️ Nenhum pedido encontrado com externalId: {}", id);
        }
        return order;
    }

    public List<Order> getOrdersByStatus(final OrderStatus status) {
        log.info("🔍 Buscando pedidos com status: {}", status);
        List<Order> orders = orderRepository.findByStatus(status);
        log.info("📦 {} pedido(s) encontrado(s) com status {}", orders.size(), status);
        return orders;
    }

    private Order buildOrder(Order request) {
        Order order = new Order();
        order.setExternalId(request.getExternalId());
        order.setStatus(OrderStatus.RECEIVED);
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> savedItems = itemRepository.saveAll(request.getItems());

        order.setItems(savedItems);
        String traceId = UUID.randomUUID().toString();

        order.setTraceId(traceId);
        order.setProcessingMessage("Order received successfully");

        return order;
    }

    private void validateRequest(Order request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required items");
        }
    }

}
