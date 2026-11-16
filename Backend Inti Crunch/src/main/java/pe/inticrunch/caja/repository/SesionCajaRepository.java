package pe.inticrunch.caja.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.caja.entity.SesionCaja;

public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SesionCaja s where s.id = :id and s.status = 'OPEN'")
    Optional<SesionCaja> bloquearAbierta(Long id);
    @Query("select s from SesionCaja s order by s.id desc")
    List<SesionCaja> recientes(Pageable page);
    @Query("select s from SesionCaja s where s.status = 'OPEN' order by s.id")
    List<SesionCaja> abiertas();
    @Query("select s from SesionCaja s where s.locationId = 1 and s.status = 'OPEN' order by s.id")
    List<SesionCaja> abiertasDelLocal();
    List<SesionCaja> findByOpenedByInOrderByIdDesc(Collection<String> openedBy);
    boolean existsByShiftId(Long shiftId);
}
