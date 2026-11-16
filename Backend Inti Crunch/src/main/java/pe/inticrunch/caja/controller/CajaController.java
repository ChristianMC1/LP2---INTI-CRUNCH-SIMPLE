package pe.inticrunch.caja.controller;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.caja.service.CajaService;

@RestController
@RequestMapping("/admin/cash")
public class CajaController {

    private final CajaService servicio;

    public CajaController(CajaService servicio) {
        this.servicio=servicio;
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> allDocuments() {
        return servicio.documents();
    }

    @GetMapping(value="/documents/{id}/pdf",produces="application/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public org.springframework.http.ResponseEntity<byte[]> pdf(@PathVariable long id) {
        return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=\"inti-crunch-"+id+".pdf\"").body(servicio.document(id));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> movements() {
        return servicio.movements();
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public Map<String,Object> movement(@RequestBody Map<String,Object> data,Authentication auth) {
        return servicio.movement(data,nombreVendedor(auth));
    }

    @GetMapping("/payments/{id}/refundable")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> refundable(@PathVariable long id) {
        return servicio.refundable(id);
    }

    @GetMapping("/refunds")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> refunds() {
        return servicio.refunds();
    }

    @PostMapping("/payments/{id}/refund")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> refund(@PathVariable long id,@RequestBody Map<String,Object> data,Authentication auth) {
        return servicio.refund(id,data,nombreVendedor(auth));
    }

    private String nombreVendedor(Authentication auth) {
        if(auth.getPrincipal() instanceof Jwt jwt){String nombre=jwt.getClaimAsString("name");if(nombre!=null&&!nombre.isBlank())return nombre;String usuario=jwt.getClaimAsString("preferred_username");if(usuario!=null&&!usuario.isBlank())return usuario;}
        return auth.getName();
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> sessions() {
        return servicio.cashSessions();
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public Map<String,Object> open(@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.openCash(body,nombreVendedor(auth));
    }

    @PatchMapping("/sessions/{id}/close")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public Map<String,Object> close(@PathVariable long id,@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.closeCash(id,body,nombreVendedor(auth));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> payments() {
        return servicio.payments();
    }

    @GetMapping("/payments/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public List<Map<String,Object>> documents(@PathVariable long id) {
        return servicio.paymentDocuments(id);
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO')")
    public Map<String,Object> collect(@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.collectPayment(body,nombreVendedor(auth));
    }
}
