package pe.inticrunch.compras.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Cantidad recibida de cada insumo. Tabla: purchase_receipt_lines */
@Entity
@Table(name = "purchase_receipt_lines")
@IdClass(LineaRecepcion.Clave.class)
public class LineaRecepcion {
    @Id
    @Column(name = "receipt_id", nullable = false)
    private Long receiptId;

    @Id
    @Column(name = "purchase_line_id", nullable = false)
    private Long purchaseLineId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "observed_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal observedQuantity = BigDecimal.ZERO;

    @Column(name = "note", length = 300)
    private String note;

    public Long getReceiptId() { return receiptId; }
    public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
    public Long getPurchaseLineId() { return purchaseLineId; }
    public void setPurchaseLineId(Long purchaseLineId) { this.purchaseLineId = purchaseLineId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getObservedQuantity() { return observedQuantity; }
    public void setObservedQuantity(BigDecimal observedQuantity) { this.observedQuantity = observedQuantity; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    /** Clave compuesta de la tabla purchase_receipt_lines. */
    public static class Clave implements java.io.Serializable {
        private Long receiptId;
        private Long purchaseLineId;
        public Clave() {}
        public Clave(Long receiptId, Long purchaseLineId) { this.receiptId = receiptId; this.purchaseLineId = purchaseLineId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(receiptId, c.receiptId) && java.util.Objects.equals(purchaseLineId, c.purchaseLineId); }
        @Override public int hashCode() { return java.util.Objects.hash(receiptId, purchaseLineId); }
    }
}
