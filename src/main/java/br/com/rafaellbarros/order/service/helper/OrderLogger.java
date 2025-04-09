package br.com.rafaellbarros.order.service.helper;

import br.com.rafaellbarros.order.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderLogger {

    public void logSavedOrder(final Order order) {
        log.info("[{}] Pedido salvo: {}", order.getTraceId(), order.getExternalId());
    }

    public void logDuplicate(final String externalId) {
        log.warn("Pedido duplicado externalId: {}", externalId);
    }

    public void logDuplicateIgnored(final String externalId) {
        log.warn("Pedido duplicado ignorado externalId: {}", externalId);
    }

    public void logInvalidIgnored(String externalId, String reason) {
        log.warn("Pedido inválido ignorado externalId: {} - Motivo: {}", externalId, reason);
    }

    public void logSearchByExternalId(final String id) {
        log.info("Buscando pedido pelo externalId: {}", id);
    }

    public void logFound(final String orderId) {
        log.info("Pedido encontrado: {}", orderId);
    }
}