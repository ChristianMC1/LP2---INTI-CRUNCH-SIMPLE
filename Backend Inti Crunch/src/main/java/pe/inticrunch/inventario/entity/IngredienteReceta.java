package pe.inticrunch.inventario.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Insumo que lleva una receta. Tabla: recipe_ingredients */
@Entity
@Table(name = "recipe_ingredients")
@IdClass(IngredienteReceta.Clave.class)
public class IngredienteReceta {
    @Id
    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Id
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    /** Clave compuesta de la tabla recipe_ingredients. */
    public static class Clave implements java.io.Serializable {
        private Long recipeId;
        private Long ingredientId;
        public Clave() {}
        public Clave(Long recipeId, Long ingredientId) { this.recipeId = recipeId; this.ingredientId = ingredientId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(recipeId, c.recipeId) && java.util.Objects.equals(ingredientId, c.ingredientId); }
        @Override public int hashCode() { return java.util.Objects.hash(recipeId, ingredientId); }
    }
}
