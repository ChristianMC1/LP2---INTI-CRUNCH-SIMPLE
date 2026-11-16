package pe.inticrunch.personal.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.personal.entity.AsignacionTurno;

public interface AsignacionTurnoRepository extends JpaRepository<AsignacionTurno, AsignacionTurno.Clave> {
    List<AsignacionTurno> findByShiftIdOrderByEmployeeId(Long shiftId);
    List<AsignacionTurno> findByShiftIdIn(Collection<Long> shiftIds);
    List<AsignacionTurno> findByEmployeeId(Long employeeId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AsignacionTurno a where a.shiftId = :shift and a.employeeId = :employee")
    Optional<AsignacionTurno> bloquear(Long shift, Long employee);
    boolean existsByShiftIdAndCheckInIsNotNull(Long shiftId);
    /** ¿Queda alguien sin marcar salida o ausencia? */
    @Query("select count(a) > 0 from AsignacionTurno a where a.shiftId = :shift and (a.attendanceStatus = 'PENDING' or (a.checkIn is not null and a.checkOut is null))")
    boolean faltaMarcar(Long shift);
    /** ¿El colaborador ya tiene otro turno que se cruza con ese horario? */
    @Query("select count(a) > 0 from AsignacionTurno a, Turno s where s.id = a.shiftId and a.employeeId = :employee and s.id <> :shift and s.status <> 'CANCELLED' and s.startsAt < :hasta and s.endsAt > :desde")
    boolean tieneCruce(Long employee, Long shift, Instant desde, Instant hasta);
    @Query("select count(a) > 0 from AsignacionTurno a, Turno s where s.id = a.shiftId and a.employeeId = :employee and s.status in ('PLANNED','ACTIVE') and s.endsAt > :ahora")
    boolean tieneTurnoVigente(Long employee, Instant ahora);
    @Query("select count(a) > 0 from AsignacionTurno a, Turno s where s.id = a.shiftId and s.id = :shift and a.employeeId = :employee and s.status in ('PLANNED','ACTIVE') and a.roleName in ('CAJERO','ADMINISTRADOR','SUPERVISOR')")
    boolean puedeCobrarEnTurno(Long shift, Long employee);
    @Query("select a from AsignacionTurno a, Turno s where s.id = a.shiftId order by s.startsAt desc")
    List<AsignacionTurno> recientes(Pageable page);
}
