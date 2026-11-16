package pe.inticrunch.personal.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/** Colaborador del local. Tabla: employees */
@Entity
@Table(name = "employees")
public class Colaborador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "keycloak_subject", length = 100)
    private String keycloakSubject;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "hired_on", nullable = false)
    private LocalDate hiredOn = LocalDate.now(java.time.ZoneId.of("America/Lima"));

    @Column(name = "available", nullable = false)
    private Boolean available = Boolean.TRUE;

    @Column(name = "dni", length = 8)
    private String dni;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "username", length = 80)
    private String username;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "employee_roles", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "role_name", nullable = false)
    private Set<String> roles = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "employee_locations", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "location_id", nullable = false)
    private Set<Long> locationIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "courier_zones", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "zone_id", nullable = false)
    private Set<Long> zoneIds = new LinkedHashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeycloakSubject() { return keycloakSubject; }
    public void setKeycloakSubject(String keycloakSubject) { this.keycloakSubject = keycloakSubject; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDate getHiredOn() { return hiredOn; }
    public void setHiredOn(LocalDate hiredOn) { this.hiredOn = hiredOn; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public Set<Long> getLocationIds() { return locationIds; }
    public void setLocationIds(Set<Long> locationIds) { this.locationIds = locationIds; }
    public Set<Long> getZoneIds() { return zoneIds; }
    public void setZoneIds(Set<Long> zoneIds) { this.zoneIds = zoneIds; }
}
