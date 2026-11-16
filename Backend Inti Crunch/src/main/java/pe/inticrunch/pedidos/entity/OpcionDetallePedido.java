package pe.inticrunch.pedidos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Opción elegida para un producto del pedido. Tabla: order_line_options */
@Entity
@Table(name = "order_line_options")
@IdClass(OpcionDetallePedido.Clave.class)
public class OpcionDetallePedido {
    @Id
    @Column(name = "order_line_id", nullable = false)
    private Long orderLineId;

    @Id
    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "extra_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraPrice;

    public Long getOrderLineId() { return orderLineId; }
    public void setOrderLineId(Long orderLineId) { this.orderLineId = orderLineId; }
    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }
    public String getOptionName() { return optionName; }
    public void setOptionName(String optionName) { this.optionName = optionName; }
    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }

    /** Clave compuesta de la tabla order_line_options. */
    public static class Clave implements java.io.Serializable {
        private Long orderLineId;
        private Long optionId;
        public Clave() {}
        public Clave(Long orderLineId, Long optionId) { this.orderLineId = orderLineId; this.optionId = optionId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(orderLineId, c.orderLineId) && java.util.Objects.equals(optionId, c.optionId); }
        @Override public int hashCode() { return java.util.Objects.hash(orderLineId, optionId); }
    }
}
