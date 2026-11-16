package pe.inticrunch.preparacion.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.preparacion.entity.Incidencia;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    @Query("select i from Incidencia i order by i.id desc")
    List<Incidencia> recientes(Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incidencia i where i.id = :id and i.resolvedAt is null")
    Optional<Incidencia> bloquearAbierta(Long id);
}
