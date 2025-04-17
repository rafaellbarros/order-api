package br.com.rafaellbarros.order.processor;

import br.com.rafaellbarros.order.domain.OrderStatus;
import br.com.rafaellbarros.order.logger.OrderProcessorLogger;
import br.com.rafaellbarros.order.repository.OrderRepository;
import br.com.rafaellbarros.order.service.OrderProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class OrderProcessor {

    private final OrderProcessorService orderProcessorService;
    private final OrderRepository repository;
    private final OrderProcessorLogger logger;

    @Scheduled(cron = "${order.processor.schedule}")
    public void processScheduledOrders() {

        final var receivedOrders = repository.findByStatus(OrderStatus.RECEIVED);

        if (receivedOrders.isEmpty()) {
            logger.notStatusFound();
            return;
        }

        int size = receivedOrders.size();
        logger.startProcessing(size);

        final var processedOrders = orderProcessorService.processOrders(receivedOrders);

        repository.saveAll(processedOrders);

        logger.processingCompleted(size);
    }
}
