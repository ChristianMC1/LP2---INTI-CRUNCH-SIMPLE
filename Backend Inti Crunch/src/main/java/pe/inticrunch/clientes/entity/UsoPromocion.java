package pe.inticrunch.clientes.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Uso de una promoción en un pedido. Tabla: promotion_redemptions */
@Entity
@Table(name = "promotion_redemptions")
@IdClass(UsoPromocion.Clave.class)
public class UsoPromocion {
    @Id
    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Id
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "redeemed_at", nullable = false)
    private Instant redeemedAt = Instant.now();

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    public Long getPromotionId() { return promotionId; }
    public void setPromotionId(Long promotionId) { this.promotionId = promotionId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Instant getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(Instant redeemedAt) { this.redeemedAt = redeemedAt; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    /** Clave compuesta de la tabla promotion_redemptions. */
    public static class Clave implements java.io.Serializable {
        private Long promotionId;
        private Long orderId;
        public Clave() {}
        public Clave(Long promotionId, Long orderId) { this.promotionId = promotionId; this.orderId = orderId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(promotionId, c.promotionId) && java.util.Objects.equals(orderId, c.orderId); }
        @Override public int hashCode() { return java.util.Objects.hash(promotionId, orderId); }
    }
}
