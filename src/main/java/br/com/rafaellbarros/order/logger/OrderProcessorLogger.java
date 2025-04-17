package br.com.rafaellbarros.order.logger;

import br.com.rafaellbarros.order.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderProcessorLogger {

    public void processedSuccessfully(final Order order) {
        log.info("[{}] Pedido {} processado com sucesso. Total R$ {}",
                order.getTraceId(), order.getExternalId(), order.getTotalAmount());
    }

    public void errorProcessing(final Order order, final Exception ex) {
        log.error("[{}] Erro ao processar pedido {}: {}",
                order.getTraceId(), order.getExternalId(), ex.getMessage(), ex);
    }


    public void notStatusFound() {
        log.info("Nenhum pedido com status RECEIVED encontrado.");
    }

    public void startProcessing(final int size) {
        log.info("Iniciando processamento de {} pedidos RECEIVED", size);
    }

    public void processingCompleted(final int size) {
        log.info("Finalizado processamento de {} pedidos.", size);
    }

}
