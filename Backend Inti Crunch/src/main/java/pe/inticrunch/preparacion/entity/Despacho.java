package pe.inticrunch.preparacion.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** Entrega de un pedido (recojo, para llevar o delivery). Tabla: dispatches */
@Entity
@Table(name = "dispatches")
public class Despacho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "delivery_type", nullable = false, length = 20)
    private String deliveryType;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "zone", length = 100)
    private String zone;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "QUEUED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "customer_note", length = 300)
    private String customerNote;

    @Column(name = "estimated_at")
    private Instant estimatedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "dispatch_zones", joinColumns = @JoinColumn(name = "dispatch_id"))
    @Column(name = "zone_id", nullable = false)
    private Set<Long> zoneIds = new LinkedHashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }
    public Instant getEstimatedAt() { return estimatedAt; }
    public void setEstimatedAt(Instant estimatedAt) { this.estimatedAt = estimatedAt; }
    public Set<Long> getZoneIds() { return zoneIds; }
    public void setZoneIds(Set<Long> zoneIds) { this.zoneIds = zoneIds; }
}
