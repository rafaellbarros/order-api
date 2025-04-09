package br.com.rafaellbarros.order.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank(message = "ExternalId é obrigatório")
    private String externalId;

    @Valid
    @NotEmpty(message = "Lista de itens não pode estar vazia")
    private List<OrderItem> items = new ArrayList<>();

    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;

    private String traceId;
    private String processingMessage;

}
