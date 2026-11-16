package pe.inticrunch.clientes.controller;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.clientes.service.ClienteService;

@RestController
@RequestMapping("/admin")
public class ClientesController {

    private final ClienteService servicio;

    public ClientesController(ClienteService servicio) {
        this.servicio=servicio;
    }

    @GetMapping("/promotions/{id}/products")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> conditions(@PathVariable long id) {
        return servicio.promotionConditions(id);
    }

    @GetMapping("/customers/{id}/orders")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> history(@PathVariable long id) {
        return servicio.customerHistory(id);
    }

    @PatchMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public Map<String,Object> updateCustomer(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.updateCustomer(id,data);
    }

    @GetMapping("/customers/points")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> points() {
        return servicio.points();
    }

    @GetMapping("/promotions/usages")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> usages() {
        return servicio.usages();
    }

    @PutMapping("/promotions/{id}/products")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> products(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.promotionProducts(id,data);
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> customers() {
        return servicio.customers();
    }

    @PostMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public Map<String,Object> createCustomer(@RequestBody Map<String,Object> body) {
        return servicio.createCustomer(body);
    }

    @GetMapping("/promotions")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> promotions() {
        return servicio.promotions();
    }

    @PostMapping("/promotions")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> createPromotion(@RequestBody Map<String,Object> body) {
        return servicio.createPromotion(body);
    }

    @PatchMapping("/promotions/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> updatePromotion(@PathVariable long id,@RequestBody Map<String,Object> body) {
        return servicio.updatePromotion(id,body);
    }

    @PatchMapping("/promotions/{id}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> updatePromotionStatus(@PathVariable long id,@RequestBody Map<String,Object> body) {
        return servicio.updatePromotionStatus(id,body);
    }

    @PostMapping("/promotions/redeem")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public Map<String,Object> redeem(@RequestBody Map<String,Object> body) {
        return servicio.redeemPromotion(body);
    }

    @DeleteMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> deleteCustomer(@PathVariable long id) {
        return servicio.deleteCustomer(id);
    }

    @DeleteMapping("/promotions/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> deletePromotion(@PathVariable long id) {
        return servicio.deletePromotion(id);
    }

    @PostMapping("/promotions/preview")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO','OPERACIONES')")
    public Map<String,Object> preview(@RequestBody Map<String,Object> body) {
        return servicio.preview(body);
    }
}
