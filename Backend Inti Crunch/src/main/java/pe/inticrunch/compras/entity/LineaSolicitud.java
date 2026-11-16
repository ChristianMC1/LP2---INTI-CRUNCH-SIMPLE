package pe.inticrunch.compras.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Insumo pedido en una solicitud. Tabla: purchase_request_lines */
@Entity
@Table(name = "purchase_request_lines")
@IdClass(LineaSolicitud.Clave.class)
public class LineaSolicitud {
    @Id
    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Id
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    /** Clave compuesta de la tabla purchase_request_lines. */
    public static class Clave implements java.io.Serializable {
        private Long requestId;
        private Long ingredientId;
        public Clave() {}
        public Clave(Long requestId, Long ingredientId) { this.requestId = requestId; this.ingredientId = ingredientId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(requestId, c.requestId) && java.util.Objects.equals(ingredientId, c.ingredientId); }
        @Override public int hashCode() { return java.util.Objects.hash(requestId, ingredientId); }
    }
}
