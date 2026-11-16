package pe.inticrunch.personal.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Colaborador asignado a un turno con su asistencia. Tabla: employee_shifts */
@Entity
@Table(name = "employee_shifts")
@IdClass(AsignacionTurno.Clave.class)
public class AsignacionTurno {
    @Id
    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Id
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "role_name", nullable = false, length = 40)
    private String roleName;

    @Column(name = "check_in")
    private Instant checkIn;

    @Column(name = "check_out")
    private Instant checkOut;

    @Column(name = "incident", length = 300)
    private String incident;

    @Column(name = "attendance_status", nullable = false, length = 20)
    private String attendanceStatus = "PENDING";

    @Column(name = "late_minutes", nullable = false)
    private Integer lateMinutes = 0;

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public Instant getCheckIn() { return checkIn; }
    public void setCheckIn(Instant checkIn) { this.checkIn = checkIn; }
    public Instant getCheckOut() { return checkOut; }
    public void setCheckOut(Instant checkOut) { this.checkOut = checkOut; }
    public String getIncident() { return incident; }
    public void setIncident(String incident) { this.incident = incident; }
    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }

    /** Clave compuesta de la tabla employee_shifts. */
    public static class Clave implements java.io.Serializable {
        private Long shiftId;
        private Long employeeId;
        public Clave() {}
        public Clave(Long shiftId, Long employeeId) { this.shiftId = shiftId; this.employeeId = employeeId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Clave c)) return false; return java.util.Objects.equals(shiftId, c.shiftId) && java.util.Objects.equals(employeeId, c.employeeId); }
        @Override public int hashCode() { return java.util.Objects.hash(shiftId, employeeId); }
    }
}
