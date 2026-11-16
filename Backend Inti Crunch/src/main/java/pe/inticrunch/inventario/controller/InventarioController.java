package pe.inticrunch.inventario.controller;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.identidad.Actor;
import pe.inticrunch.inventario.service.InventarioService;

@RestController
@RequestMapping("/admin/inventory")
public class InventarioController {

    private static final String READ="hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO','OPERACIONES')";

    private static final String MANAGE="hasAnyRole('ADMINISTRADOR','SUPERVISOR','ABASTECIMIENTO')";

    private final InventarioService servicio;

    public InventarioController(InventarioService servicio) {
        this.servicio=servicio;
    }

    @GetMapping("/warehouses")
    @PreAuthorize(READ)
    public List<Map<String,Object>> warehouses() {
        return servicio.warehouses();
    }

    @PostMapping("/warehouses")
    @PreAuthorize(MANAGE)
    public Map<String,Object> warehouse(@RequestBody Map<String,Object> data) {
        return servicio.createWarehouse(data);
    }

    @PatchMapping("/warehouses/{id}")
    @PreAuthorize(MANAGE)
    public Map<String,Object> updateWarehouse(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.updateWarehouse(id,data);
    }

    @DeleteMapping("/warehouses/{id}")
    @PreAuthorize(MANAGE)
    public Map<String,Object> deleteWarehouse(@PathVariable long id) {
        return servicio.deleteWarehouse(id);
    }

    @GetMapping("/balances")
    @PreAuthorize(READ)
    public List<Map<String,Object>> balances() {
        return servicio.balances();
    }

    @GetMapping("/lots")
    @PreAuthorize(READ)
    public List<Map<String,Object>> lots() {
        return servicio.lots();
    }

    @PatchMapping("/lots/{id}")
    @PreAuthorize(MANAGE)
    public Map<String,Object> updateLot(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.updateLot(id,data);
    }

    @PostMapping("/lots/{id}/discard")
    @PreAuthorize(READ)
    public Map<String,Object> discardLot(@PathVariable long id,Authentication auth) {
        return servicio.discardLot(id,Actor.name(auth));
    }

    @GetMapping("/movements/{id}/lines")
    @PreAuthorize(READ)
    public List<Map<String,Object>> movementLines(@PathVariable long id) {
        return servicio.movementLines(id);
    }

    @GetMapping("/recipes/{id}/lines")
    @PreAuthorize(READ)
    public List<Map<String,Object>> recipeLines(@PathVariable long id) {
        return servicio.recipeLines(id);
    }

    @DeleteMapping("/recipes/{id}")
    @PreAuthorize(MANAGE)
    public Map<String,Object> deleteRecipe(@PathVariable long id) {
        return servicio.deleteRecipe(id);
    }

    @PatchMapping("/ingredients/{id}")
    @PreAuthorize(MANAGE)
    public Map<String,Object> updateIngredient(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.updateIngredient(id,data);
    }

    @DeleteMapping("/ingredients/{id}")
    @PreAuthorize(MANAGE)
    public Map<String,Object> deleteIngredient(@PathVariable long id) {
        return servicio.deleteIngredient(id);
    }

    @PostMapping("/ingredients/{id}/count")
    @PreAuthorize(READ)
    public Map<String,Object> count(@PathVariable long id,@RequestBody Map<String,Object> data,Authentication auth) {
        return servicio.countIngredient(id,data,Actor.name(auth));
    }

    @GetMapping("/ingredients")
    @PreAuthorize(READ)
    public List<Map<String,Object>> ingredients() {
        return servicio.ingredients();
    }

    @PostMapping("/ingredients")
    @PreAuthorize(MANAGE)
    public Map<String,Object> createIngredient(@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.createIngredient(body,Actor.name(auth));
    }

    @GetMapping("/movements")
    @PreAuthorize(READ)
    public List<Map<String,Object>> movements() {
        return servicio.inventoryMovements();
    }

    @PostMapping("/movements")
    @PreAuthorize(READ)
    public Map<String,Object> movement(@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.createMovement(body,Actor.name(auth));
    }

    @GetMapping("/recipes")
    @PreAuthorize(READ)
    public List<Map<String,Object>> recipes() {
        return servicio.recipes();
    }

    @PostMapping("/recipes")
    @PreAuthorize(READ)
    public Map<String,Object> createRecipe(@RequestBody Map<String,Object> body) {
        return servicio.createRecipe(body);
    }

    @GetMapping("/production")
    @PreAuthorize(READ)
    public List<Map<String,Object>> production() {
        return servicio.productionRuns();
    }

    @PostMapping("/production")
    @PreAuthorize(READ)
    public Map<String,Object> createProduction(@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.createProduction(body,Actor.name(auth));
    }
}
