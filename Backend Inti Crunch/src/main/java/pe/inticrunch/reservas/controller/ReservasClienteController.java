package pe.inticrunch.reservas.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.identidad.CuentaService;
import pe.inticrunch.reservas.service.ReservaService;

/** Reservas que hace el cliente desde la página principal (requiere sesión iniciada). */
@RestController @RequestMapping("/customer")
@PreAuthorize("isAuthenticated()")
public class ReservasClienteController {
    private final ReservaService reservations;
    private final CuentaService accounts;
    public ReservasClienteController(ReservaService reservations,CuentaService accounts){this.reservations=reservations;this.accounts=accounts;}

    @GetMapping("/tables/now") public List<Map<String,Object>> seating(Authentication auth){return reservations.seatingNow(auth.getName());}
    @GetMapping("/reservations/tables") public List<Map<String,Object>> tables(@RequestParam Instant start,@RequestParam(required=false) Instant end){return reservations.tableChoices(start,end==null?start.plusSeconds(7200):end,-1);}
    @GetMapping("/reservations") public List<Map<String,Object>> mine(Authentication auth){return reservations.customerReservations(auth.getName());}
    @PostMapping("/reservations") public Map<String,Object> create(@RequestBody Map<String,Object> body,Authentication auth){
        var profile=accounts.profile(auth);
        Instant start=Instant.parse(String.valueOf(body.get("startsAt")));
        if(start.isBefore(Instant.now().plusSeconds(15*60)))throw new IllegalArgumentException("Elige una hora con al menos 15 minutos de anticipación.");
        if(start.isAfter(Instant.now().plusSeconds(60L*24*3600)))throw new IllegalArgumentException("Puedes reservar con hasta 60 días de anticipación.");
        int hour=start.atZone(ZoneId.of("America/Lima")).getHour();
        if(hour<11||hour>21)throw new IllegalArgumentException("Atendemos reservas entre las 11:00 y las 21:00.");
        if(!(body.get("tableIds") instanceof List<?> tables)||tables.isEmpty())throw new IllegalArgumentException("Elige una o varias mesas libres.");
        Map<String,Object> data=new LinkedHashMap<>();
        data.put("guestName",profile.get("fullName"));
        String phone=CuentaService.cleanPhone(Objects.toString(body.get("phone"),""));
        data.put("guestPhone",phone.isBlank()?profile.get("phone"):phone);
        data.put("partySize",body.get("partySize"));data.put("startsAt",start.toString());data.put("endsAt",start.plusSeconds(7200).toString());
        data.put("tableIds",body.get("tableIds"));data.put("notes",Objects.toString(body.get("notes"),""));
        data.put("_customerId",accounts.customerFor(auth));data.put("_subject",auth.getName());
        if(Objects.toString(data.get("guestPhone"),"").isBlank())throw new IllegalArgumentException("Escribe un teléfono de contacto.");
        return reservations.createReservation(data);
    }
    @PatchMapping("/reservations/{id}/cancel") public Map<String,Object> cancel(@PathVariable long id,Authentication auth){return reservations.cancelCustomerReservation(id,auth.getName());}
}
