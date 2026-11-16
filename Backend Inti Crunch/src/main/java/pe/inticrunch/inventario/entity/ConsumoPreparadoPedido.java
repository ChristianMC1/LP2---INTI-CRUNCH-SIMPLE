package pe.inticrunch.inventario.entity;

import jakarta.persistence.*;

/** Porciones preparadas usadas en un pedido. Tabla: order_prepared_consumptions */
@Entity
@Table(name = "order_prepared_consumptions")
public class ConsumoPreparadoPedido {
    @Id
    @Column(name = "order_line_id", nullable = false)
    private Long orderLineId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public Long getOrderLineId() { return orderLineId; }
    public void setOrderLineId(Long orderLineId) { this.orderLineId = orderLineId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
