package pe.inticrunch.inventario.entity;

import jakarta.persistence.*;

/** Receta de un producto. Tabla: recipes */
@Entity
@Table(name = "recipes")
public class Receta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "servings", nullable = false)
    private Integer servings = 1;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
