package pe.inticrunch.caja.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Parte de un cobro hecha con un medio (efectivo, Yape...). Tabla: payment_methods */
@Entity
@Table(name = "payment_methods")
public class MedioPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "method", nullable = false, length = 20)
    private String method;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "tip", nullable = false, precision = 12, scale = 2)
    private BigDecimal tip = BigDecimal.ZERO;

    @Column(name = "reference", length = 120)
    private String reference;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getTip() { return tip; }
    public void setTip(BigDecimal tip) { this.tip = tip; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
