package pe.inticrunch.compras.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Precio de un insumo según el proveedor. Tabla: supplier_ingredients */
@Entity
@Table(name = "supplier_ingredients")
@IdClass(PrecioProveedor.Clave.class)
public class PrecioProveedor {
    @Id
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Id
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "supplier_sku", length = 80)
    private String supplierSku;

    @Column(name = "last_price", precision = 12, scale = 2)
    private BigDecimal lastPrice;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public String getSupplierSku() { return supplierSku; }
    public void setSupplierSku(String supplierSku) { this.supplierSku = supplierSku; }
    public BigDecimal getLastPrice() { return lastPrice; }
    public void setLastPrice(BigDecimal lastPrice) { this.lastPrice = lastPrice; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /** Clave compuesta de la tabla supplier_ingredients. */
    public static class Clave implements java.io.Serializable {
        private Long supplierId;
        private Long ingredientId;
        public Clave() {}
        public Clave(Long supplierId, Long ingredientId) { this.supplierId = supplierId; this.ingredientId = ingredientId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(supplierId, c.supplierId) && java.util.Objects.equals(ingredientId, c.ingredientId); }
        @Override public int hashCode() { return java.util.Objects.hash(supplierId, ingredientId); }
    }
}
