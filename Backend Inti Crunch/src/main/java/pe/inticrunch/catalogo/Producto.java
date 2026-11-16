package pe.inticrunch.catalogo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "products")
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "name", nullable = false, length = 120) private String nombre;
    @Column(name = "description", nullable = false, length = 500) private String descripcion;
    @Column(name = "price", nullable = false, precision = 10, scale = 2) private BigDecimal precio;
    @Column(name = "category", nullable = false, length = 40) private String categoria;
    @Column(name = "image_url", length = 500) private String urlImagen;
    @Column(name = "active", nullable = false) private boolean activo = true;
    @Column(name = "available", nullable = false) private boolean disponible = true;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant fechaCreacion = Instant.now();
    protected Producto() {}
    public Producto(String nombre, String descripcion, BigDecimal precio, String categoria, String urlImagen) {
        this.nombre=nombre; this.descripcion=descripcion; this.precio=precio; this.categoria=categoria; this.urlImagen=urlImagen;
    }
    public Long getId(){return id;} public String getName(){return nombre;} public String getDescription(){return descripcion;}
    public BigDecimal getPrice(){return precio;} public String getCategory(){return categoria;} public String getImageUrl(){return urlImagen;}
    public boolean isActive(){return activo;} public boolean isAvailable(){return disponible;}
    /** Cambia los datos del producto desde el panel. */
    public void actualizar(String nombre, String descripcion, BigDecimal precio, String categoria, String urlImagen, boolean activo, boolean disponible) {
        this.nombre=nombre; this.descripcion=descripcion; this.precio=precio; this.categoria=categoria; this.urlImagen=urlImagen; this.activo=activo; this.disponible=disponible;
    }
    /** Retira el producto de la carta conservando su historial de ventas. */
    public void retirar() { this.activo=false; this.disponible=false; }
}
