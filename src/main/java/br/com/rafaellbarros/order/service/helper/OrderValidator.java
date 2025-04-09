package br.com.rafaellbarros.order.service.helper;

import br.com.rafaellbarros.order.domain.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OrderValidator {

    public void validate(final Order request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itens obrigatórios");
        }
    }
}