package pe.inticrunch.compras.controller;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.compras.service.ComprasService;

@RestController
@RequestMapping("/admin/purchases")
public class ComprasController {

    private final ComprasService servicio;

    public ComprasController(ComprasService servicio) {
        this.servicio=servicio;
    }

    @GetMapping("/catalog")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public List<Map<String,Object>> catalog() {
        return servicio.catalog();
    }

    @PostMapping("/catalog")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> quote(@RequestBody Map<String,Object> data) {
        return servicio.quote(data);
    }

    @PatchMapping("/suppliers/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> updateSupplier(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.updateSupplier(id,data);
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public List<Map<String,Object>> requests() {
        return servicio.requests();
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> request(@RequestBody Map<String,Object> data,Authentication auth) {
        return servicio.createRequest(data,pe.inticrunch.identidad.Actor.name(auth));
    }

    @GetMapping("/requests/{id}/lines")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public List<Map<String,Object>> requestLines(@PathVariable long id) {
        return servicio.requestLines(id);
    }

    @PatchMapping("/orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> cancel(@PathVariable long id) {
        return servicio.cancelPurchase(id);
    }

    @GetMapping("/receipts")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public List<Map<String,Object>> receipts() {
        return servicio.receipts();
    }

    @GetMapping("/receipts/{id}/lines")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public List<Map<String,Object>> receiptLines(@PathVariable long id) {
        return servicio.receiptLines(id);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public List<Map<String,Object>> invoices() {
        return servicio.invoices();
    }

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> invoice(@RequestBody Map<String,Object> data) {
        return servicio.invoice(data);
    }

    @PostMapping("/invoices/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> payInvoice(@PathVariable long id,@RequestBody Map<String,Object> data,Authentication auth) {
        return servicio.payInvoice(id,data,pe.inticrunch.identidad.Actor.name(auth));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public List<Map<String,Object>> suppliers() {
        return servicio.suppliers();
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> supplier(@RequestBody Map<String,Object> body) {
        return servicio.createSupplier(body);
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public List<Map<String,Object>> orders() {
        return servicio.purchases();
    }

    @GetMapping("/orders/{id}/lines")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public List<Map<String,Object>> lines(@PathVariable long id) {
        return servicio.purchaseLines(id);
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> createOrder(@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.createPurchase(body,pe.inticrunch.identidad.Actor.name(auth));
    }

    @PatchMapping("/orders/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> receive(@PathVariable long id,@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.receivePurchase(id,body,pe.inticrunch.identidad.Actor.name(auth));
    }

    @DeleteMapping("/suppliers/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> deleteSupplier(@PathVariable long id) {
        return servicio.deleteSupplier(id);
    }

    @DeleteMapping("/catalog")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> deleteQuote(@RequestParam long supplierId,@RequestParam long ingredientId) {
        return servicio.deleteQuote(supplierId,ingredientId);
    }

    @PutMapping("/requests/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> updateRequest(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.updateRequest(id,data);
    }

    @DeleteMapping("/requests/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')")
    public Map<String,Object> deleteRequest(@PathVariable long id) {
        return servicio.deleteRequest(id);
    }
}
