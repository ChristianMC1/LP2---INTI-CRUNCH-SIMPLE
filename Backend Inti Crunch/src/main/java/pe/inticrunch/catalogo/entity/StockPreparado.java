package pe.inticrunch.catalogo.entity;

import jakarta.persistence.*;

/** Porciones preparadas por adelantado de un producto. Tabla: prepared_stock */
@Entity
@Table(name = "prepared_stock")
public class StockPreparado {
    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
