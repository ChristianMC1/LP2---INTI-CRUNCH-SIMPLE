package pe.inticrunch.pedidos;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO','OPERACIONES')")
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO','OPERACIONES')")
    public Map<String, Object> create(@RequestBody Map<String, Object> data, Authentication auth) {
        Map<String, Object> request = new LinkedHashMap<>(data);
        request.remove("_customerId");
        return service.create(request, null);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CLIENTE')")
    public List<Map<String, Object>> mine(Authentication auth) {
        return service.listMine(auth.getName());
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO','OPERACIONES')")
    public List<Map<String, Object>> history(@PathVariable long id) {
        return service.history(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String, Object> cancel(@PathVariable long id, Authentication auth) {
        return service.cancel(id, pe.inticrunch.identidad.Actor.name(auth));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES','CAJERO')")
    public Map<String, Object> status(@PathVariable long id, @RequestBody Map<String, Object> data,
            Authentication auth) {
        return service.changeStatus(id, Objects.toString(data.get("status"), ""),
                pe.inticrunch.identidad.Actor.name(auth));
    }

    @PostMapping("/{id}/discount")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String, Object> discount(@PathVariable long id, @RequestBody Map<String, Object> data,
            Authentication auth) {
        return service.discount(id, data, pe.inticrunch.identidad.Actor.name(auth));
    }
}
