package pe.inticrunch.identidad;

import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.clientes.entity.Cliente;
import pe.inticrunch.clientes.repository.ClienteRepository;
import pe.inticrunch.operaciones.Validaciones;
import pe.inticrunch.personal.repository.ColaboradorRepository;

/** Registro de clientes, inicio de sesión con DNI y perfil de la persona conectada. */
@Service
public class CuentaService extends Validaciones {
    private static final Map<String, String> ROLE_LABELS = new LinkedHashMap<>();
    static {
        ROLE_LABELS.put("ADMINISTRADOR", "Administrador"); ROLE_LABELS.put("SUPERVISOR", "Supervisor"); ROLE_LABELS.put("CAJERO", "Cajero"); ROLE_LABELS.put("OPERACIONES", "Operaciones");
        ROLE_LABELS.put("ABASTECIMIENTO", "Abastecimiento"); ROLE_LABELS.put("REPARTIDOR", "Repartidor"); ROLE_LABELS.put("CLIENTE", "Cliente");
    }
    private final AdministracionKeycloakService keycloak;
    private final ClienteRepository clientes;
    private final ColaboradorRepository colaboradores;
    public CuentaService(AdministracionKeycloakService keycloak, ClienteRepository clientes, ColaboradorRepository colaboradores) { this.keycloak = keycloak; this.clientes = clientes; this.colaboradores = colaboradores; }

    public static String roleLabel(Collection<String> roles) { return ROLE_LABELS.entrySet().stream().filter(entry -> roles.contains(entry.getKey())).map(Map.Entry::getValue).findFirst().orElse("Equipo Inti Crunch"); }

    public static String cleanPhone(String value) { return value == null ? "" : value.replaceAll("[\\s-]", ""); }
    public static void validatePerson(String firstName, String lastName, String dni, String phone, String email) {
        // REGLA CUENTA-01: Escribe el nombre con letras (entre 2 y 60).
        if (!firstName.matches("[\\p{L} .'-]{2,60}")) throw new IllegalArgumentException("Escribe el nombre con letras (entre 2 y 60).");
        // REGLA CUENTA-02: Escribe los apellidos con letras (entre 2 y 80).
        if (!lastName.matches("[\\p{L} .'-]{2,80}")) throw new IllegalArgumentException("Escribe los apellidos con letras (entre 2 y 80).");
        // REGLA CUENTA-03: El DNI debe tener exactamente 8 números.
        if (!dni.matches("[0-9]{8}")) throw new IllegalArgumentException("El DNI debe tener exactamente 8 números.");
        // REGLA CUENTA-04: El celular debe tener 9 números y empezar con 9.
        if (!phone.matches("9[0-9]{8}")) throw new IllegalArgumentException("El celular debe tener 9 números y empezar con 9.");
        // REGLA CUENTA-05: Escribe un correo válido, por ejemplo nombre@correo.com.
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")) throw new IllegalArgumentException("Escribe un correo válido, por ejemplo nombre@correo.com.");
    }
    public static void validatePassword(String password, String confirmation) {
        // REGLA CUENTA-06: La contraseña debe tener al menos 8 caracteres, con letras y números.
        if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres, con letras y números.");
        // REGLA CUENTA-07: Las contraseñas no coinciden. Escríbelas otra vez.
        if (confirmation != null && !password.equals(confirmation)) throw new IllegalArgumentException("Las contraseñas no coinciden. Escríbelas otra vez.");
    }

    @Transactional public Map<String, Object> register(Map<String, Object> data) {
        String firstName = str(data, "firstName"), lastName = str(data, "lastName"), dni = str(data, "dni"), phone = cleanPhone(str(data, "phone")), email = str(data, "email").toLowerCase(Locale.ROOT),
            password = String.valueOf(data.get("password")), confirmation = String.valueOf(data.get("confirmPassword"));
        validatePerson(firstName, lastName, dni, phone, email); validatePassword(password, confirmation);
        // REGLA CUENTA-08: Ya existe una cuenta con ese DNI. Inicia sesión con tu contraseña.
        if (clientes.existsByDniAndSubjectIsNotNull(dni)) throw new IllegalStateException("Ya existe una cuenta con ese DNI. Inicia sesión con tu contraseña.");
        // REGLA CUENTA-09: Ese celular ya pertenece a otra cuenta.
        if (clientes.existsByPhoneAndSubjectIsNotNull(phone)) throw new IllegalStateException("Ese celular ya pertenece a otra cuenta.");
        // REGLA CUENTA-10: Ese DNI pertenece a un colaborador. Inicia sesión con tu cuenta de personal.
        if (colaboradores.existsByDni(dni)) throw new IllegalStateException("Ese DNI pertenece a un colaborador. Inicia sesión con tu cuenta de personal.");
        String subject = keycloak.createUser(dni, email, firstName, lastName, password, List.of("CLIENTE"));
        try {
            // Si ya compró antes (por celular o DNI), su ficha de cliente se vincula a la nueva cuenta.
            var existing = clientes.porCelularODni(phone, dni);
            Cliente c = existing.isEmpty() ? new Cliente() : existing.getFirst();
            c.setFullName(firstName + " " + lastName); c.setPhone(phone); c.setEmail(email); c.setDni(dni); c.setSubject(subject);
            clientes.saveAndFlush(c);
        } catch (RuntimeException exception) { keycloak.deleteUser(subject); throw exception; }
        var tokens = keycloak.login(dni, password);
        return Map.of("message", "¡Bienvenido a Inti Crunch, " + firstName + "! Tu cuenta está lista.", "tokens", tokens);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> login(Map<String, Object> data) {
        String user = str(data, "dni").trim(), password = String.valueOf(data.getOrDefault("password", ""));
        // REGLA CUENTA-11: Escribe tu contraseña.
        if (password.isBlank()) throw new IllegalArgumentException("Escribe tu contraseña.");
        // Los colaboradores pueden tener un usuario distinto a su DNI en Keycloak.
        String username = colaboradores.usuarioPorDni(user).stream().findFirst().orElse(user);
        return Map.of("tokens", keycloak.login(username.toLowerCase(Locale.ROOT), password));
    }

    /** Datos para autocompletar pedidos y reservas, y el rol que se muestra en el perfil. */
    @Transactional(readOnly = true)
    public Map<String, Object> profile(Authentication auth) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> roles = auth.getAuthorities().stream().map(authority -> authority.getAuthority().replaceFirst("^ROLE_", "")).filter(ROLE_LABELS::containsKey).toList();
        String name = Actor.name(auth);
        result.put("name", name); result.put("roles", roles); result.put("roleLabel", roleLabel(roles));
        String username = auth.getPrincipal() instanceof Jwt jwt ? Objects.toString(jwt.getClaimAsString("preferred_username"), "") : "";
        String email = auth.getPrincipal() instanceof Jwt jwt ? Objects.toString(jwt.getClaimAsString("email"), "") : "";
        Optional<Cliente> customer = clientes.findBySubject(auth.getName());
        if (customer.isEmpty() && username.matches("[0-9]{8}")) customer = clientes.findByDni(username).stream().findFirst();
        if (customer.isPresent()) {
            Cliente c = customer.get();
            result.put("customerId", c.getId()); result.put("fullName", c.getFullName()); result.put("phone", c.getPhone()); result.put("email", Objects.toString(c.getEmail(), email)); result.put("dni", c.getDni());
        } else { result.put("fullName", name); result.put("email", email); result.put("dni", username.matches("[0-9]{8}") ? username : ""); }
        colaboradores.findByKeycloakSubject(auth.getName()).ifPresent(e -> {
            result.put("employeeId", e.getId()); result.put("fullName", e.getFullName());
            if (result.get("phone") == null) result.put("phone", e.getPhone());
        });
        return result;
    }

    /** Busca o crea la ficha de cliente de la cuenta conectada. */
    @Transactional public long customerFor(Authentication auth) {
        var profile = profile(auth);
        if (profile.get("customerId") != null) {
            long id = ((Number) profile.get("customerId")).longValue();
            clientes.findById(id).filter(c -> c.getSubject() == null).ifPresent(c -> c.setSubject(auth.getName()));
            return id;
        }
        String phone = Objects.toString(profile.get("phone"), "");
        // REGLA CUENTA-12: Completa tu celular en tu cuenta antes de continuar.
        if (phone.isBlank()) throw new IllegalStateException("Completa tu celular en tu cuenta antes de continuar.");
        var byPhone = clientes.sinCuentaConCelular(phone);
        if (!byPhone.isEmpty()) { Cliente c = byPhone.getFirst(); c.setSubject(auth.getName()); return c.getId(); }
        Cliente c = new Cliente();
        c.setFullName(Objects.toString(profile.get("fullName"), "")); c.setPhone(phone); c.setEmail(Objects.toString(profile.get("email"), null)); c.setSubject(auth.getName());
        return clientes.save(c).getId();
    }
}
