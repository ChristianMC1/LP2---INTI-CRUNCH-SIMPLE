package pe.inticrunch.personal.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.caja.repository.SesionCajaRepository;
import pe.inticrunch.preparacion.repository.AsignacionDespachoRepository;
import pe.inticrunch.identidad.CuentaService;
import pe.inticrunch.identidad.AdministracionKeycloakService;
import pe.inticrunch.operaciones.Filas;
import pe.inticrunch.operaciones.Validaciones;
import pe.inticrunch.operaciones.repository.LocalRepository;
import pe.inticrunch.personal.entity.*;
import pe.inticrunch.personal.repository.*;

@Service
public class PersonalService extends Validaciones {
    private static final Set<String> ROLES = Set.of("ADMINISTRADOR", "SUPERVISOR", "CAJERO", "OPERACIONES", "ABASTECIMIENTO", "REPARTIDOR");
    private final AdministracionKeycloakService keycloak;
    private final ColaboradorRepository colaboradores;
    private final TurnoRepository turnos;
    private final AsignacionTurnoRepository asignaciones;
    private final AsignacionDespachoRepository entregas;
    private final SesionCajaRepository cajas;
    private final LocalRepository locales;

    public PersonalService(AdministracionKeycloakService keycloak, ColaboradorRepository colaboradores, TurnoRepository turnos, AsignacionTurnoRepository asignaciones,
                           AsignacionDespachoRepository entregas, SesionCajaRepository cajas, LocalRepository locales) {
        this.keycloak = keycloak; this.colaboradores = colaboradores; this.turnos = turnos; this.asignaciones = asignaciones; this.entregas = entregas; this.cajas = cajas; this.locales = locales;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> employees() {
        return colaboradores.todos().stream().map(e -> {
            Map<String, Object> row = Filas.of("ID", e.getId(), "Colaborador", e.getFullName(), "DNI", e.getDni(), "Celular", e.getPhone(), "Correo", e.getEmail(),
                "Roles", e.getRoles().isEmpty() ? null : String.join(", ", new TreeSet<>(e.getRoles())), "Usuario", e.getUsername(),
                "Puede ingresar", e.getKeycloakSubject() == null ? "No" : "Sí", "Activo", e.getActive(), "Ingreso", e.getHiredOn());
            row.put("_locationIds", e.getLocationIds().stream().sorted().toList());
            return row;
        }).toList();
    }

    private static List<String> roles(Map<String, Object> data) {
        // REGLA PERSONAL-01: Elige al menos un rol (por ejemplo, Cajero).
        if (!(data.get("roles") instanceof List<?> values) || values.isEmpty()) throw new IllegalArgumentException("Elige al menos un rol (por ejemplo, Cajero).");
        var roles = values.stream().map(value -> value.toString().toUpperCase(Locale.ROOT)).distinct().toList();
        // REGLA PERSONAL-02: Rol no válido.
        if (!ROLES.containsAll(roles)) throw new IllegalArgumentException("Rol no válido.");
        return roles;
    }
    private static String[] names(String fullName) {
        String clean = fullName.trim().replaceAll("\\s+", " "); int space = clean.indexOf(' ');
        return space < 0 ? new String[]{clean, "-"} : new String[]{clean.substring(0, space), clean.substring(space + 1)};
    }
    private void guardAdmin(Collection<String> roles, boolean actorIsAdmin, Colaborador employee) {
        if (actorIsAdmin) return;
        // REGLA PERSONAL-03: Solo un administrador puede dar el rol de administrador.
        if (roles.contains("ADMINISTRADOR")) throw new IllegalStateException("Solo un administrador puede dar el rol de administrador.");
        // REGLA PERSONAL-04: Solo un administrador puede modificar la cuenta de otro administrador.
        if (employee != null && employee.getRoles().contains("ADMINISTRADOR")) throw new IllegalStateException("Solo un administrador puede modificar la cuenta de otro administrador.");
    }

    /** Registra un colaborador y, si se indica contraseña, crea su acceso en Keycloak. */
    @Transactional public Map<String, Object> createEmployee(Map<String, Object> data, boolean actorIsAdmin) {
        String firstName = str(data, "firstName"), lastName = str(data, "lastName"), dni = str(data, "dni"), phone = CuentaService.cleanPhone(str(data, "phone")), email = str(data, "email").toLowerCase(Locale.ROOT);
        CuentaService.validatePerson(firstName, lastName, dni, phone, email);
        var roles = roles(data); guardAdmin(roles, actorIsAdmin, null);
        // REGLA PERSONAL-05: Ya existe un colaborador con el DNI 
        if (colaboradores.existsByDni(dni)) throw new IllegalStateException("Ya existe un colaborador con el DNI " + dni + ".");
        String password = optional(data, "password");
        String subject = null, username = null;
        if (!password.isBlank()) {
            CuentaService.validatePassword(password, optional(data, "confirmPassword"));
            username = optional(data, "username").isBlank() ? dni : optional(data, "username").toLowerCase(Locale.ROOT);
            // REGLA PERSONAL-06: El usuario solo puede tener letras minúsculas, números, punto o guion.
            if (!username.matches("[a-z0-9._-]{3,40}")) throw new IllegalArgumentException("El usuario solo puede tener letras minúsculas, números, punto o guion.");
            subject = keycloak.createUser(username, email, firstName, lastName, password, roles);
        }
        try {
            Colaborador e = new Colaborador();
            e.setFullName(firstName + " " + lastName); e.setPhone(phone); e.setDni(dni); e.setEmail(email); e.setUsername(username); e.setKeycloakSubject(subject);
            e.setHiredOn(optional(data, "hiredOn").isBlank() ? hoy() : LocalDate.parse(str(data, "hiredOn")));
            saveRoles(e, roles, data);
            long employee = colaboradores.saveAndFlush(e).getId();
            return Map.of("id", employee, "message", subject == null ? "Colaborador registrado (sin acceso al sistema)." : "Colaborador registrado. Ya puede iniciar sesión con su DNI " + dni + " y la contraseña indicada.");
        } catch (RuntimeException exception) { if (subject != null) keycloak.deleteUser(subject); throw exception; }
    }

    @Transactional public Map<String, Object> updateEmployee(long employee, Map<String, Object> data, boolean actorIsAdmin) {
        Colaborador record = colaboradores.bloquear(employee).orElseThrow(Validaciones::noExiste);
        var roles = roles(data); guardAdmin(roles, actorIsAdmin, record);
        boolean active = flag(data, "active");
        if (!active && (entregas.existsByEmployeeIdAndEndedAtIsNull(employee) || asignaciones.tieneTurnoVigente(employee, Instant.now())))
            // REGLA PERSONAL-07: Reasigna sus entregas y quítalo de los turnos vigentes antes de desactivarlo.
            throw new IllegalStateException("Reasigna sus entregas y quítalo de los turnos vigentes antes de desactivarlo.");
        String fullName = str(data, "fullName"), phone = CuentaService.cleanPhone(optional(data, "phone")), email = optional(data, "email").toLowerCase(Locale.ROOT), dni = optional(data, "dni");
        // REGLA PERSONAL-08: El DNI debe tener exactamente 8 números.
        if (!dni.isBlank() && !dni.matches("[0-9]{8}")) throw new IllegalArgumentException("El DNI debe tener exactamente 8 números.");
        // REGLA PERSONAL-09: Otro colaborador ya tiene el DNI 
        if (!dni.isBlank() && colaboradores.otroConDni(dni, employee)) throw new IllegalStateException("Otro colaborador ya tiene el DNI " + dni + ".");
        // REGLA PERSONAL-10: El celular debe tener 9 números y empezar con 9.
        if (!phone.isBlank() && !phone.matches("9[0-9]{8}")) throw new IllegalArgumentException("El celular debe tener 9 números y empezar con 9.");
        String subject = Objects.toString(record.getKeycloakSubject(), "");
        String[] parts = names(fullName);
        String username = Objects.toString(record.getUsername(), "");
        if (!subject.isBlank()) { keycloak.updateUser(subject, email, parts[0], parts[1], active); keycloak.setRoles(subject, roles); }
        else if (!optional(data, "password").isBlank()) {
            CuentaService.validatePassword(optional(data, "password"), optional(data, "confirmPassword"));
            // REGLA PERSONAL-11: Para crear su acceso escribe el DNI del colaborador.
            if (dni.isBlank()) throw new IllegalArgumentException("Para crear su acceso escribe el DNI del colaborador.");
            // REGLA PERSONAL-12: Para crear su acceso escribe el correo del colaborador.
            if (email.isBlank()) throw new IllegalArgumentException("Para crear su acceso escribe el correo del colaborador.");
            username = dni; subject = keycloak.createUser(username, email, parts[0], parts[1], optional(data, "password"), roles);
        }
        record.setFullName(fullName); record.setPhone(phone); record.setEmail(email); record.setDni(dni.isBlank() ? null : dni); record.setActive(active);
        record.setKeycloakSubject(subject.isBlank() ? null : subject); record.setUsername(username.isBlank() ? null : username);
        if (!optional(data, "hiredOn").isBlank()) record.setHiredOn(LocalDate.parse(str(data, "hiredOn")));
        saveRoles(record, roles, data);
        return Map.of("message", "Datos del colaborador guardados" + (subject.isBlank() ? "." : " y su acceso actualizado en Keycloak."));
    }

    @Transactional public Map<String, Object> changePassword(long employee, Map<String, Object> data, boolean actorIsAdmin) {
        Colaborador record = colaboradores.findById(employee).orElseThrow(Validaciones::noExiste);
        guardAdmin(List.of(), actorIsAdmin, record);
        // REGLA PERSONAL-13: Este colaborador todavía no tiene acceso. Usa 'Editar' y escribe una contraseña para crearlo.
        if (record.getKeycloakSubject() == null) throw new IllegalStateException("Este colaborador todavía no tiene acceso. Usa 'Editar' y escribe una contraseña para crearlo.");
        String password = String.valueOf(data.getOrDefault("password", ""));
        CuentaService.validatePassword(password, String.valueOf(data.getOrDefault("confirmPassword", "")));
        keycloak.resetPassword(record.getKeycloakSubject(), password);
        return Map.of("message", "Contraseña de " + record.getFullName() + " cambiada.");
    }

    private void saveRoles(Colaborador e, List<String> roles, Map<String, Object> data) {
        e.getRoles().clear(); e.getRoles().addAll(roles);
        var locations = ids(data.get("locationIds")); if (locations.isEmpty()) locations = List.of(1L);
        for (long location : locations) locales.findById(location).filter(l -> Boolean.TRUE.equals(l.getActive())).orElseThrow(Validaciones::noExiste);
        e.getLocationIds().clear(); e.getLocationIds().addAll(locations);
    }

    // ---------- Turnos ----------
    private Map<Long, String> nombres() { return colaboradores.findAll().stream().collect(Collectors.toMap(Colaborador::getId, Colaborador::getFullName)); }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> shifts() {
        var lista = turnos.recientes(PageRequest.of(0, 300));
        var nombre = nombres();
        Map<Long, List<AsignacionTurno>> equipo = lista.isEmpty() ? Map.of()
            : asignaciones.findByShiftIdIn(lista.stream().map(Turno::getId).toList()).stream().collect(Collectors.groupingBy(AsignacionTurno::getShiftId));
        return lista.stream().map(s -> {
            var miembros = equipo.getOrDefault(s.getId(), List.of());
            String personas = miembros.stream().map(a -> nombre.get(a.getEmployeeId())).filter(Objects::nonNull).sorted().collect(Collectors.joining(", "));
            return Filas.of("Turno", s.getId(), "Nombre", s.getName(), "Grupo", s.getGroupName() == null ? "—" : s.getGroupName(), "Inicio", s.getStartsAt(), "Fin", s.getEndsAt(),
                "Estado", s.getStatus(), "Personas", (long) miembros.size(), "Equipo", personas.isBlank() ? "Sin personas" : personas);
        }).toList();
    }
    @Transactional(readOnly = true)
    public Map<String, Object> shiftDetail(long shift) {
        Turno s = turnos.findById(shift).orElseThrow(Validaciones::noExiste);
        Map<String, Object> record = Filas.of("id", s.getId(), "name", s.getName(), "groupName", s.getGroupName(), "notes", s.getNotes(), "startsAt", s.getStartsAt(), "endsAt", s.getEndsAt(), "status", s.getStatus());
        record.put("employees", asignaciones.findByShiftIdOrderByEmployeeId(shift).stream().map(a -> Filas.of("employeeId", a.getEmployeeId(), "roleName", a.getRoleName())).toList());
        return record;
    }
    private static Map<String, Object> asistencia(AsignacionTurno a, String primera, Object valor, String persona) {
        return Filas.of(primera, valor, "Colaborador", persona, "Rol", a.getRoleName(), "Entrada", a.getCheckIn(), "Salida", a.getCheckOut(), "Estado", a.getAttendanceStatus(),
            "Tardanza (min)", a.getLateMinutes(), "Incidencia", a.getIncident());
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> shiftEmployees(long shift) {
        var nombre = nombres();
        return asignaciones.findByShiftIdOrderByEmployeeId(shift).stream().sorted(Comparator.comparing(a -> nombre.getOrDefault(a.getEmployeeId(), "")))
            .map(a -> asistencia(a, "ID", a.getEmployeeId(), nombre.get(a.getEmployeeId()))).toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> attendanceRecords() {
        var nombre = nombres();
        Map<Long, Turno> turno = turnos.findAll().stream().collect(Collectors.toMap(Turno::getId, t -> t));
        return asignaciones.recientes(PageRequest.of(0, 300)).stream()
            .sorted(Comparator.comparing((AsignacionTurno a) -> turno.get(a.getShiftId()).getStartsAt()).reversed().thenComparing(a -> nombre.getOrDefault(a.getEmployeeId(), "")))
            .map(a -> asistencia(a, "Turno", turno.get(a.getShiftId()).getName(), nombre.get(a.getEmployeeId()))).toList();
    }

    @Transactional public Map<String, Object> createShift(Map<String, Object> data) {
        Instant start = instant(data.get("startsAt")), end = instant(data.get("endsAt"));
        validatePeriod(start, end);
        Turno s = new Turno();
        s.setLocationId(1L); s.setName(str(data, "name")); s.setStartsAt(start); s.setEndsAt(end); s.setGroupName(nullable(data, "groupName")); s.setNotes(optional(data, "notes"));
        long shift = turnos.saveAndFlush(s).getId();
        saveMembers(shift, data, start, end);
        return Map.of("id", shift, "message", "Turno programado sin cruces de horario.");
    }
    @Transactional public Map<String, Object> updateShift(long shift, Map<String, Object> data) {
        Turno s = turnos.bloquear(shift).orElseThrow(Validaciones::noExiste);
        // REGLA PERSONAL-14: Solo se editan turnos programados o en curso.
        if (!Set.of("PLANNED", "ACTIVE").contains(s.getStatus())) throw new IllegalStateException("Solo se editan turnos programados o en curso.");
        Instant start = instant(data.get("startsAt")), end = instant(data.get("endsAt")); validatePeriod(start, end);
        s.setName(str(data, "name")); s.setGroupName(nullable(data, "groupName")); s.setNotes(optional(data, "notes")); s.setStartsAt(start); s.setEndsAt(end);
        var keep = new HashSet<Long>();
        for (var member : lines(data, "employees")) keep.add(id(member, "employeeId"));
        for (var current : asignaciones.findByShiftIdOrderByEmployeeId(shift)) {
            if (!keep.contains(current.getEmployeeId())) {
                // REGLA PERSONAL-15: No puedes quitar a alguien que ya marcó entrada en este turno.
                if (current.getCheckIn() != null) throw new IllegalStateException("No puedes quitar a alguien que ya marcó entrada en este turno.");
                asignaciones.delete(current);
            }
        }
        asignaciones.flush(); turnos.flush();
        saveMembers(shift, data, start, end);
        return Map.of("message", "Turno actualizado.");
    }
    @Transactional public Map<String, Object> deleteShift(long shift) {
        Turno s = turnos.bloquear(shift).orElseThrow(Validaciones::noExiste);
        // REGLA PERSONAL-16: Este turno ya tiene asistencias; ciérralo en lugar de eliminarlo.
        if (asignaciones.existsByShiftIdAndCheckInIsNotNull(shift)) throw new IllegalStateException("Este turno ya tiene asistencias; ciérralo en lugar de eliminarlo.");
        // REGLA PERSONAL-17: Una caja está vinculada a este turno; ciérralo en lugar de eliminarlo.
        if (cajas.existsByShiftId(shift)) throw new IllegalStateException("Una caja está vinculada a este turno; ciérralo en lugar de eliminarlo.");
        asignaciones.deleteAll(asignaciones.findByShiftIdOrderByEmployeeId(shift));
        turnos.delete(s);
        return Map.of("message", "Turno eliminado.");
    }
    private static void validatePeriod(Instant start, Instant end) {
        // REGLA PERSONAL-18: Un turno debe durar entre 1 minuto y 24 horas.
        if (!end.isAfter(start) || end.isAfter(start.plusSeconds(86400))) throw new IllegalArgumentException("Un turno debe durar entre 1 minuto y 24 horas.");
    }
    private void saveMembers(long shift, Map<String, Object> data, Instant start, Instant end) {
        for (var member : lines(data, "employees").stream().sorted(Comparator.comparing(item -> id(item, "employeeId"))).toList()) {
            long employee = id(member, "employeeId"); String role = str(member, "roleName").toUpperCase(Locale.ROOT);
            Colaborador person = colaboradores.bloquear(employee).filter(e -> Boolean.TRUE.equals(e.getActive())).orElseThrow(Validaciones::noExiste);
            if (!person.getRoles().contains(role)) throw new IllegalArgumentException(person.getFullName() + " no tiene el rol " + role.toLowerCase(Locale.ROOT) + ". Elige una función que sí tenga.");
            if (asignaciones.tieneCruce(employee, shift, start, end)) throw new IllegalStateException(person.getFullName() + " ya tiene otro turno en ese horario.");
            AsignacionTurno a = asignaciones.findById(new AsignacionTurno.Clave(shift, employee)).orElseGet(() -> {
                AsignacionTurno nueva = new AsignacionTurno(); nueva.setShiftId(shift); nueva.setEmployeeId(employee); return nueva;
            });
            a.setRoleName(role);
            asignaciones.save(a);
        }
    }

    @Transactional public Map<String, Object> attendance(long shift, long employee, Map<String, Object> data) {
        Turno s = turnos.bloquear(shift).orElseThrow(Validaciones::noExiste);
        // REGLA PERSONAL-19: El turno está cerrado o cancelado.
        if (Set.of("CLOSED", "CANCELLED").contains(s.getStatus())) throw new IllegalStateException("El turno está cerrado o cancelado.");
        String action = choice(data, "action", "IN", "OUT", "ABSENT", "NOTE");
        AsignacionTurno a = asignaciones.bloquear(shift, employee).orElseThrow(Validaciones::noExiste);
        boolean changed = false;
        Instant now = Instant.now();
        switch (action) {
            case "IN" -> {
                if (a.getCheckIn() == null && !"ABSENT".equals(a.getAttendanceStatus())) {
                    long minutes = Math.max(0, Math.floorDiv(Duration.between(s.getStartsAt(), now).getSeconds(), 60));
                    a.setCheckIn(now); a.setLateMinutes((int) minutes); a.setAttendanceStatus(now.isAfter(s.getStartsAt().plusSeconds(300)) ? "LATE" : "PRESENT");
                    changed = true;
                }
                s.setStatus("ACTIVE");
            }
            case "OUT" -> { if (a.getCheckIn() != null && a.getCheckOut() == null) { a.setCheckOut(now); changed = true; } }
            case "ABSENT" -> {
                // REGLA PERSONAL-20: El turno aún no comienza.
                if (s.getStartsAt().isAfter(now)) throw new IllegalStateException("El turno aún no comienza.");
                if (a.getCheckIn() == null) { a.setAttendanceStatus("ABSENT"); a.setIncident(str(data, "incident")); changed = true; }
            }
            default -> { a.setIncident(str(data, "incident")); changed = true; }
        }
        // REGLA PERSONAL-21: Esa marcación no corresponde: revisa si ya marcó entrada o salida.
        if (!changed) throw new IllegalStateException("Esa marcación no corresponde: revisa si ya marcó entrada o salida.");
        return Map.of("message", "Asistencia registrada.");
    }
    @Transactional public Map<String, Object> closeShift(long shift, Map<String, Object> data) {
        Turno s = turnos.bloquear(shift).orElseThrow(Validaciones::noExiste);
        String status = choice(data, "status", "CLOSED", "CANCELLED");
        // REGLA PERSONAL-22: El turno ya está cerrado.
        if (Set.of("CLOSED", "CANCELLED").contains(s.getStatus())) throw new IllegalStateException("El turno ya está cerrado.");
        // REGLA PERSONAL-23: Un turno con asistencias debe cerrarse, no cancelarse.
        if (status.equals("CANCELLED") && asignaciones.existsByShiftIdAndCheckInIsNotNull(shift)) throw new IllegalStateException("Un turno con asistencias debe cerrarse, no cancelarse.");
        // REGLA PERSONAL-24: Marca la salida o la ausencia de todos antes de cerrar el turno.
        if (status.equals("CLOSED") && asignaciones.faltaMarcar(shift)) throw new IllegalStateException("Marca la salida o la ausencia de todos antes de cerrar el turno.");
        s.setStatus(status);
        return Map.of("message", "Turno actualizado.");
    }
}
