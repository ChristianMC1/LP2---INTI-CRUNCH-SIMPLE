package pe.inticrunch.reservas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** Reserva de una o varias mesas. Tabla: reservations */
@Entity
@Table(name = "reservations")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "guest_name", nullable = false, length = 150)
    private String guestName;

    @Column(name = "guest_phone", nullable = false, length = 40)
    private String guestPhone;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "BOOKED";

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "customer_subject", length = 120)
    private String customerSubject;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "reservation_tables", joinColumns = @JoinColumn(name = "reservation_id"))
    @Column(name = "table_id", nullable = false)
    private Set<Long> tableIds = new LinkedHashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }
    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public String getCustomerSubject() { return customerSubject; }
    public void setCustomerSubject(String customerSubject) { this.customerSubject = customerSubject; }
    public Set<Long> getTableIds() { return tableIds; }
    public void setTableIds(Set<Long> tableIds) { this.tableIds = tableIds; }
}
