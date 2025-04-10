package br.com.rafaellbarros.order.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Document(collection = "order_items")
public class OrderItem {

    @Id
    private String id;

    @NotBlank(message = "Nome do item é obrigatório")
    private String name;

    @NotNull(message = "Preço do item é obrigatório")
    @DecimalMin(value = "0.01", inclusive = true, message = "Preço deve ser maior que zero")
    private BigDecimal price;

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantity;

    public OrderItem() { }

    public OrderItem(String name, BigDecimal price, Integer quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
}