package pe.inticrunch.reservas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** Grupo sentado en el salón. Tabla: visits */
@Entity
@Table(name = "visits")
public class Visita {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "guest_name", nullable = false, length = 150)
    private String guestName;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Column(name = "arrived_at", nullable = false)
    private Instant arrivedAt = Instant.now();

    @Column(name = "left_at")
    private Instant leftAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "visit_tables", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "table_id", nullable = false)
    private Set<Long> tableIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "visit_orders", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "order_id", nullable = false)
    private Set<Long> orderIds = new LinkedHashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public Integer getPartySize() { return partySize; }
    public void setPartySize(Integer partySize) { this.partySize = partySize; }
    public Instant getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(Instant arrivedAt) { this.arrivedAt = arrivedAt; }
    public Instant getLeftAt() { return leftAt; }
    public void setLeftAt(Instant leftAt) { this.leftAt = leftAt; }
    public Set<Long> getTableIds() { return tableIds; }
    public void setTableIds(Set<Long> tableIds) { this.tableIds = tableIds; }
    public Set<Long> getOrderIds() { return orderIds; }
    public void setOrderIds(Set<Long> orderIds) { this.orderIds = orderIds; }
}
