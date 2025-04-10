package br.com.rafaellbarros.order.logger;

import br.com.rafaellbarros.order.domain.Order;
import br.com.rafaellbarros.order.domain.OrderItem;
import br.com.rafaellbarros.order.domain.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
public class OrderLogger {

    public void created(final Order order) {
        log.info("Pedido criado: {}", order);
    }

    public void sevedItems(final List<OrderItem> items) {
        log.info("{} itens salvo(s) com sucesso.",  items.size());
    }

    public void saved(final Order order) {
        log.info("[{}] Pedido salvo: {}", order.getTraceId(), order.getExternalId());
    }

    public void savedOrders(final List<Order> orders) {
        log.info("{} pedido(s) salvo(s) com sucesso.", orders.size());
    }

    public void duplicated(final String externalId) {
        log.warn("Pedido duplicado externalId: {}", externalId);
    }

    public void duplicatedIgnored(final String externalId) {
        log.warn("Pedido duplicado ignorado externalId: {}", externalId);
    }

    public void invalidIgnored(final String externalId, final String reason) {
        log.warn("Pedido inválido ignorado externalId: {} - Motivo: {}", externalId, reason);
    }

    public void searchByExternalId(final String id) {
        log.info("Buscando pedido pelo externalId: {}", id);
    }

    public void found(final String orderId) {
        log.info("Pedido encontrado: {}", orderId);
    }

    public void searchByStatus(final OrderStatus status) {
        log.info("Pedido encontrado pelo status: {}", status);
    }


}