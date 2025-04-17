package br.com.rafaellbarros.order.processor;

import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.repository.OrderRepository;
import br.com.rafaellbarros.order.service.OrderProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessor {

    private final OrderProcessorService orderProcessorService;
    private final OrderRepository repository;

    @Scheduled(cron = "${order.processor.schedule}")
    public void processScheduledOrders() {

        final var receivedOrders = repository.findByStatus(OrderStatus.RECEIVED);

        if (receivedOrders.isEmpty()) {
            log.info("Nenhum pedido com status RECEIVED encontrado.");
            return;
        }

        log.info("Iniciando processamento de {} pedidos RECEIVED", receivedOrders.size());

        final var processedOrders = orderProcessorService.processOrders(receivedOrders);

        repository.saveAll(processedOrders);

        log.info("Finalizado processamento de {} pedidos.", processedOrders.size());
    }
}
