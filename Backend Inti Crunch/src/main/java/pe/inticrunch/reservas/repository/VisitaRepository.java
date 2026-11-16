package pe.inticrunch.reservas.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.reservas.entity.Visita;

public interface VisitaRepository extends JpaRepository<Visita, Long> {
    @Query("select v from Visita v join v.tableIds t where t = :table and v.leftAt is null order by v.id desc")
    List<Visita> abiertasEnMesa(Long table);
    @Query("select distinct t from Visita v join v.tableIds t where v.leftAt is null")
    List<Long> mesasOcupadas();
    @Query("select count(v) > 0 from Visita v join v.tableIds t where t = :table")
    boolean mesaConHistorial(Long table);
    List<Visita> findByReservationIdAndLeftAtIsNull(Long reservationId);
    boolean existsByReservationId(Long reservationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Visita v where v.id = :id and v.leftAt is null")
    Optional<Visita> bloquearAbierta(Long id);
    @Query("select v from Visita v order by v.id desc")
    List<Visita> recientes(Pageable page);
}
