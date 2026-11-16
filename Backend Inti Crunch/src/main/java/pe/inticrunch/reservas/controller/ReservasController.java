package pe.inticrunch.reservas.controller;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.reservas.service.ReservaService;

@RestController
@RequestMapping("/admin/reservations")
public class ReservasController {

    private static final String STAFF="hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES','CAJERO')";

    private final ReservaService servicio;

    public ReservasController(ReservaService servicio) {
        this.servicio=servicio;
    }

    @GetMapping("/tables")
    @PreAuthorize(STAFF)
    public List<Map<String,Object>> tables() {
        return servicio.diningTables();
    }

    @GetMapping("/floor")
    @PreAuthorize(STAFF)
    public List<Map<String,Object>> floor() {
        return servicio.floor();
    }

    @PostMapping("/tables")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> table(@RequestBody Map<String,Object> data) {
        return servicio.createTable(data);
    }

    @PatchMapping("/tables/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> updateTable(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.updateTable(id,data);
    }

    @DeleteMapping("/tables/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public Map<String,Object> deleteTable(@PathVariable long id) {
        return servicio.deleteTable(id);
    }

    @PatchMapping("/tables/{id}/free")
    @PreAuthorize(STAFF)
    public Map<String,Object> freeTable(@PathVariable long id) {
        return servicio.freeTable(id);
    }

    @GetMapping("/availability")
    @PreAuthorize(STAFF)
    public List<Map<String,Object>> availability(@RequestParam java.time.Instant start,@RequestParam java.time.Instant end) {
        return servicio.availability(start,end);
    }

    @GetMapping("/table-choices")
    @PreAuthorize(STAFF)
    public List<Map<String,Object>> choices(@RequestParam java.time.Instant start,@RequestParam java.time.Instant end,@RequestParam(defaultValue="-1") long reservation) {
        return servicio.tableChoices(start,end,reservation);
    }

    @GetMapping("/{id}/events")
    @PreAuthorize(STAFF)
    public List<Map<String,Object>> history(@PathVariable long id) {
        return servicio.history(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize(STAFF)
    public Map<String,Object> detail(@PathVariable long id) {
        return servicio.reservationDetail(id);
    }

    @GetMapping("/visits")
    @PreAuthorize(STAFF)
    public List<Map<String,Object>> visits() {
        return servicio.visits();
    }

    @PostMapping("/visits")
    @PreAuthorize(STAFF)
    public Map<String,Object> walkIn(@RequestBody Map<String,Object> data) {
        return servicio.walkIn(data);
    }

    @PostMapping("/visits/{id}/orders")
    @PreAuthorize(STAFF)
    public Map<String,Object> attachOrder(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.attachOrder(id,data);
    }

    @GetMapping("/visits/{id}/orders")
    @PreAuthorize(STAFF)
    public List<Map<String,Object>> visitOrders(@PathVariable long id) {
        return servicio.visitOrders(id);
    }

    @PatchMapping("/visits/{id}/close")
    @PreAuthorize(STAFF)
    public Map<String,Object> finishVisit(@PathVariable long id) {
        return servicio.finishVisit(id);
    }

    @GetMapping
    @PreAuthorize(STAFF)
    public List<Map<String,Object>> reservations() {
        return servicio.reservations();
    }

    @PostMapping
    @PreAuthorize(STAFF)
    public Map<String,Object> create(@RequestBody Map<String,Object> body) {
        Map<String,Object> data=new LinkedHashMap<>(body);
        data.remove("_customerId");
        data.remove("_subject");
        return servicio.createReservation(data);
    }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF)
    public Map<String,Object> update(@PathVariable long id,@RequestBody Map<String,Object> body) {
        return servicio.updateReservation(id,body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','OPERACIONES')")
    public Map<String,Object> delete(@PathVariable long id) {
        return servicio.deleteReservation(id);
    }

    @PostMapping("/{id}/tables")
    @PreAuthorize(STAFF)
    public Map<String,Object> assignTables(@PathVariable long id,@RequestBody Map<String,Object> body) {
        return servicio.assignReservationTables(id,body);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(STAFF)
    public Map<String,Object> status(@PathVariable long id,@RequestBody Map<String,Object> body) {
        return servicio.reservationStatus(id,body);
    }
}
