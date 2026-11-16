package pe.inticrunch.inventario.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Insumo consumido en una producción. Tabla: production_run_lines */
@Entity
@Table(name = "production_run_lines")
@IdClass(LineaProduccion.Clave.class)
public class LineaProduccion {
    @Id
    @Column(name = "production_run_id", nullable = false)
    private Long productionRunId;

    @Id
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "consumed_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal consumedQuantity;

    public Long getProductionRunId() { return productionRunId; }
    public void setProductionRunId(Long productionRunId) { this.productionRunId = productionRunId; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public BigDecimal getConsumedQuantity() { return consumedQuantity; }
    public void setConsumedQuantity(BigDecimal consumedQuantity) { this.consumedQuantity = consumedQuantity; }

    /** Clave compuesta de la tabla production_run_lines. */
    public static class Clave implements java.io.Serializable {
        private Long productionRunId;
        private Long ingredientId;
        public Clave() {}
        public Clave(Long productionRunId, Long ingredientId) { this.productionRunId = productionRunId; this.ingredientId = ingredientId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(productionRunId, c.productionRunId) && java.util.Objects.equals(ingredientId, c.ingredientId); }
        @Override public int hashCode() { return java.util.Objects.hash(productionRunId, ingredientId); }
    }
}
