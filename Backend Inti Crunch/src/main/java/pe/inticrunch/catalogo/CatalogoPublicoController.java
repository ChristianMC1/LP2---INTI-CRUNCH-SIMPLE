package pe.inticrunch.catalogo;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/catalog")
public class CatalogoPublicoController {

    private final ProductoRepository products;

    public CatalogoPublicoController(ProductoRepository products) {
        this.products=products;
    }

    @GetMapping("/products")
    public List<Producto> products() {
        return products.findByActivoTrueAndDisponibleTrueOrderByCategoriaAscNombreAsc();
    }
}
