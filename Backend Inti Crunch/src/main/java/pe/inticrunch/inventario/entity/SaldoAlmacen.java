package pe.inticrunch.inventario.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Existencia de un insumo en un almacén. Tabla: warehouse_balances */
@Entity
@Table(name = "warehouse_balances")
@IdClass(SaldoAlmacen.Clave.class)
public class SaldoAlmacen {
    @Id
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Id
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    /** Clave compuesta de la tabla warehouse_balances. */
    public static class Clave implements java.io.Serializable {
        private Long warehouseId;
        private Long ingredientId;
        public Clave() {}
        public Clave(Long warehouseId, Long ingredientId) { this.warehouseId = warehouseId; this.ingredientId = ingredientId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(warehouseId, c.warehouseId) && java.util.Objects.equals(ingredientId, c.ingredientId); }
        @Override public int hashCode() { return java.util.Objects.hash(warehouseId, ingredientId); }
    }
}
