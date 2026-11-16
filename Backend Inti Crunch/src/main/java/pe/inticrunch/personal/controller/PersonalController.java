package pe.inticrunch.personal.controller;

import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.personal.service.PersonalService;

@RestController
@RequestMapping("/admin/staff")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
public class PersonalController {

    private final PersonalService servicio;

    public PersonalController(PersonalService servicio) {
        this.servicio=servicio;
    }

    private static boolean admin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(authority->authority.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    @GetMapping
    public List<Map<String,Object>> employees() {
        return servicio.employees();
    }

    @PostMapping
    public Map<String,Object> createEmployee(@RequestBody Map<String,Object> body,Authentication auth) {
        return servicio.createEmployee(body,admin(auth));
    }

    @PatchMapping("/{id}")
    public Map<String,Object> update(@PathVariable long id,@RequestBody Map<String,Object> data,Authentication auth) {
        return servicio.updateEmployee(id,data,admin(auth));
    }

    @PutMapping("/{id}/password")
    public Map<String,Object> password(@PathVariable long id,@RequestBody Map<String,Object> data,Authentication auth) {
        return servicio.changePassword(id,data,admin(auth));
    }

    @GetMapping("/attendance")
    public List<Map<String,Object>> attendanceRecords() {
        return servicio.attendanceRecords();
    }

    @GetMapping("/shifts")
    public List<Map<String,Object>> shifts() {
        return servicio.shifts();
    }

    @PostMapping("/shifts")
    public Map<String,Object> createShift(@RequestBody Map<String,Object> body) {
        return servicio.createShift(body);
    }

    @GetMapping("/shifts/{id}")
    public Map<String,Object> shift(@PathVariable long id) {
        return servicio.shiftDetail(id);
    }

    @PutMapping("/shifts/{id}")
    public Map<String,Object> updateShift(@PathVariable long id,@RequestBody Map<String,Object> body) {
        return servicio.updateShift(id,body);
    }

    @DeleteMapping("/shifts/{id}")
    public Map<String,Object> deleteShift(@PathVariable long id) {
        return servicio.deleteShift(id);
    }

    @PatchMapping("/shifts/{id}/status")
    public Map<String,Object> closeShift(@PathVariable long id,@RequestBody Map<String,Object> data) {
        return servicio.closeShift(id,data);
    }

    @GetMapping("/shifts/{id}/employees")
    public List<Map<String,Object>> shiftEmployees(@PathVariable long id) {
        return servicio.shiftEmployees(id);
    }

    @PatchMapping("/shifts/{shiftId}/employees/{employeeId}/attendance")
    public Map<String,Object> attendance(@PathVariable long shiftId,@PathVariable long employeeId,@RequestBody Map<String,Object> body) {
        return servicio.attendance(shiftId,employeeId,body);
    }
}
