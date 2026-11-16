package pe.inticrunch.catalogo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/catalog/products")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
public class ProductoAdministracionController {

    private final ProductoRepository products;

    public ProductoAdministracionController(ProductoRepository products) {
        this.products=products;
    }

    @GetMapping
    public List<Producto> list() {
        return products.findAll().stream().filter(Producto::isActive).sorted(java.util.Comparator.comparing(Producto::getId)).toList();
    }

    @PostMapping
    public Producto create(@Valid @RequestBody ProductRequest request) {
        return products.save(new Producto(request.name(), request.description(), request.price(), request.category(), request.imageUrl()));
    }

    public record ProductRequest(@NotBlank @Size(max=120) String name, @NotBlank @Size(max=500) String description,
      @NotNull @DecimalMin("0.01") BigDecimal price, @NotBlank @Size(max=40) String category, String imageUrl) {
    }
}
