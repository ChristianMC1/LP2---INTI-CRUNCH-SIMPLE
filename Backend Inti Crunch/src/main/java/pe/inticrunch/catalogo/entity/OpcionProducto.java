package pe.inticrunch.catalogo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/** Extra o cambio que se puede pedir con un producto. Tabla: product_options */
@Entity
@Table(name = "product_options")
public class OpcionProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "extra_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_option_links", joinColumns = @JoinColumn(name = "option_id"))
    @Column(name = "product_id", nullable = false)
    private Set<Long> productIds = new LinkedHashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Set<Long> getProductIds() { return productIds; }
    public void setProductIds(Set<Long> productIds) { this.productIds = productIds; }
}
