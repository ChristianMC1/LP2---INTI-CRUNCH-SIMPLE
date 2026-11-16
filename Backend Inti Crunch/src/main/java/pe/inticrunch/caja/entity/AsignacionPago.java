package pe.inticrunch.caja.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Pedido al que se aplica un pago. Tabla: payment_allocations */
@Entity
@Table(name = "payment_allocations")
@IdClass(AsignacionPago.Clave.class)
public class AsignacionPago {
    @Id
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Id
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    /** Clave compuesta de la tabla payment_allocations. */
    public static class Clave implements java.io.Serializable {
        private Long paymentId;
        private Long orderId;
        public Clave() {}
        public Clave(Long paymentId, Long orderId) { this.paymentId = paymentId; this.orderId = orderId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(paymentId, c.paymentId) && java.util.Objects.equals(orderId, c.orderId); }
        @Override public int hashCode() { return java.util.Objects.hash(paymentId, orderId); }
    }
}
