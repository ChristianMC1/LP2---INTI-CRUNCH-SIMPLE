package pe.inticrunch.compras.entity;

import jakarta.persistence.*;

/** Proveedor. Tabla: suppliers */
@Entity
@Table(name = "suppliers")
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "tax_id", length = 20)
    private String taxId;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
