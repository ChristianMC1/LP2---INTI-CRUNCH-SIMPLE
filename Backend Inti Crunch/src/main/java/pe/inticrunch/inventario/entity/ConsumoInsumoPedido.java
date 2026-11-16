package pe.inticrunch.inventario.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Insumo descontado al preparar un pedido. Tabla: order_inventory_consumptions */
@Entity
@Table(name = "order_inventory_consumptions")
@IdClass(ConsumoInsumoPedido.Clave.class)
public class ConsumoInsumoPedido {
    @Id
    @Column(name = "order_line_id", nullable = false)
    private Long orderLineId;

    @Id
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "consumed_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal consumedQuantity;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt = Instant.now();

    public Long getOrderLineId() { return orderLineId; }
    public void setOrderLineId(Long orderLineId) { this.orderLineId = orderLineId; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public BigDecimal getConsumedQuantity() { return consumedQuantity; }
    public void setConsumedQuantity(BigDecimal consumedQuantity) { this.consumedQuantity = consumedQuantity; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }

    /** Clave compuesta de la tabla order_inventory_consumptions. */
    public static class Clave implements java.io.Serializable {
        private Long orderLineId;
        private Long ingredientId;
        public Clave() {}
        public Clave(Long orderLineId, Long ingredientId) { this.orderLineId = orderLineId; this.ingredientId = ingredientId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(orderLineId, c.orderLineId) && java.util.Objects.equals(ingredientId, c.ingredientId); }
        @Override public int hashCode() { return java.util.Objects.hash(orderLineId, ingredientId); }
    }
}
