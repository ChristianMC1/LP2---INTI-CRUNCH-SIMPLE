package pe.inticrunch.preparacion.controller;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.preparacion.service.PreparacionService;

@RestController
@RequestMapping("/admin/delivery")
public class PreparacionController {

    @GetMapping("/couriers/{id}/coverage")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> currentCoverage(@PathVariable long id) {
        return servicio.currentCoverage(id);
    }

    private final PreparacionService servicio;

    public PreparacionController(PreparacionService servicio) {
        this.servicio=servicio;
    }

    @GetMapping("/zones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES','CAJERO')")
    public List<Map<String,Object>> zones() {
        return servicio.zones();
    }

    @PostMapping("/zones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> zone(@RequestBody Map<String,Object> data) {
        return servicio.createZone(data);
    }

    @GetMapping("/couriers")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public List<Map<String,Object>> couriers() {
        return servicio.couriers();
    }

    @PutMapping("/couriers/{id}/coverage")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> coverage(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.courierCoverage(id,data);
    }

    @GetMapping("/dispatches/{id}/events")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES','CAJERO','REPARTIDOR')")
    public List<Map<String,Object>> events(@PathVariable long id) {
        return servicio.history(id);
    }

    @GetMapping("/incidents")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public List<Map<String,Object>> incidents() {
        return servicio.incidents();
    }

    @PostMapping("/incidents")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> incident(@RequestBody Map<String,Object> data,Authentication auth) {
        return servicio.createIncident(data,pe.inticrunch.identidad.Actor.name(auth));
    }

    @PatchMapping("/incidents/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> resolve(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.resolveIncident(id,data);
    }

    @GetMapping("/dispatches")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES','CAJERO','REPARTIDOR')")
    public List<Map<String,Object>> dispatches() {
        return servicio.dispatches();
    }

    @PatchMapping("/dispatches/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES','REPARTIDOR')")
    public Map<String,Object> dispatchStatus(@PathVariable long id,@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.updateDispatch(id,body,pe.inticrunch.identidad.Actor.name(auth));
    }

    @PostMapping("/dispatches/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> assign(@PathVariable long id,@RequestBody Map<String,Object> body) {
        return servicio.assignCourier(id,body);
    }

    @GetMapping("/kitchen")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public List<Map<String,Object>> kitchen() {
        return servicio.kitchenTickets();
    }

    @PatchMapping("/kitchen/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> kitchenStatus(@PathVariable long id,@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.updateKitchen(id,body,pe.inticrunch.identidad.Actor.name(auth));
    }

    @PatchMapping("/zones/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> updateZone(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.updateZone(id,data);
    }

    @DeleteMapping("/zones/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> deleteZone(@PathVariable long id) {
        return servicio.deleteZone(id);
    }

    @PostMapping("/couriers")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> createCourier(@RequestBody Map<String,Object> data) {
        return servicio.createCourier(data);
    }

    @PatchMapping("/couriers/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> updateCourier(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.updateCourier(id,data);
    }

    @DeleteMapping("/couriers/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> deleteCourier(@PathVariable long id) {
        return servicio.deleteCourier(id);
    }
}
