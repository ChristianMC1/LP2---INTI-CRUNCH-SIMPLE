package pe.inticrunch.clientes.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.clientes.service.ClienteService;

@RestController
@RequestMapping("/public/promotions")
public class PromocionesPublicasController {

    private final ClienteService servicio;

    public PromocionesPublicasController(ClienteService servicio) {
        this.servicio=servicio;
    }

    @GetMapping
    public List<Map<String,Object>> list() {
        return servicio.publicPromotions();
    }

    @PostMapping("/preview")
    public Map<String,Object> preview(@RequestBody Map<String,Object> body) {
        return servicio.preview(body);
    }
}
