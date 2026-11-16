package pe.inticrunch.catalogo;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.configuracion.ConfiguracionEmpresa;

@RestController
public
class CatalogoOpcionesController {

    private final ConfiguracionEmpresa company;

    private final CatalogoService catalogo;

    public CatalogoOpcionesController(ConfiguracionEmpresa company, CatalogoService catalogo) {
        this.company = company;
        this.catalogo = catalogo;
    }

    @GetMapping("/public/options")
    public List<Map<String, Object>> options() {
        return catalogo.publicOptions();
    }

    @GetMapping("/public/zones")
    public List<Map<String, Object>> zones() {
        return catalogo.publicZones();
    }

    @GetMapping("/public/company")
    public Map<String, Object> company() {
        return company.publicInfo();
    }

    @GetMapping("/admin/catalog/options")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public List<Map<String, Object>> allOptions() {
        return catalogo.allOptions();
    }

    @PostMapping("/admin/catalog/options")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String, Object> create(@RequestBody Map<String, Object> data) {
        return catalogo.createOption(data);
    }

    @PatchMapping("/admin/catalog/options/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String, Object> updateOption(@PathVariable long id, @RequestBody Map<String, Object> data) {
        return catalogo.updateOption(id, data);
    }

    @PatchMapping("/admin/catalog/products/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String, Object> update(@PathVariable long id, @RequestBody Map<String, Object> data) {
        return catalogo.updateProduct(id, data);
    }

    @DeleteMapping("/admin/catalog/options/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String, Object> deleteOption(@PathVariable long id) {
        return catalogo.deleteOption(id);
    }

    @DeleteMapping("/admin/catalog/products/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String, Object> deleteProduct(@PathVariable long id) {
        return catalogo.deleteProduct(id);
    }
}
