package pe.inticrunch.preparacion.entity;

import jakarta.persistence.*;

/** Producto de la comanda y su estación. Tabla: kitchen_ticket_lines */
@Entity
@Table(name = "kitchen_ticket_lines")
@IdClass(LineaComanda.Clave.class)
public class LineaComanda {
    @Id
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Id
    @Column(name = "order_line_id", nullable = false)
    private Long orderLineId;

    @Id
    @Column(name = "station", nullable = false, length = 40)
    private String station = "PARRILLA";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getOrderLineId() { return orderLineId; }
    public void setOrderLineId(Long orderLineId) { this.orderLineId = orderLineId; }
    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Clave compuesta de la tabla kitchen_ticket_lines. */
    public static class Clave implements java.io.Serializable {
        private Long ticketId;
        private Long orderLineId;
        private String station;
        public Clave() {}
        public Clave(Long ticketId, Long orderLineId, String station) { this.ticketId = ticketId; this.orderLineId = orderLineId; this.station = station; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(ticketId, c.ticketId) && java.util.Objects.equals(orderLineId, c.orderLineId) && java.util.Objects.equals(station, c.station); }
        @Override public int hashCode() { return java.util.Objects.hash(ticketId, orderLineId, station); }
    }
}
