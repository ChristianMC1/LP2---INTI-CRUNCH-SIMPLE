package pe.inticrunch.personal.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.personal.entity.Turno;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Turno s where s.id = :id")
    Optional<Turno> bloquear(Long id);
    @Query("select s from Turno s order by s.id desc")
    List<Turno> recientes(Pageable page);
    @Query("select s from Turno s where s.status in ('PLANNED','ACTIVE') order by s.startsAt desc")
    List<Turno> vigentes();
}
