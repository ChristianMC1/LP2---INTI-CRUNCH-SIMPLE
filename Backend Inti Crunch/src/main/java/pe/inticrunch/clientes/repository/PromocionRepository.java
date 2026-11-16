package pe.inticrunch.clientes.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.clientes.entity.Promocion;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promocion p where p.id = :id")
    Optional<Promocion> bloquear(Long id);
    @Query("select p from Promocion p where upper(p.code) = upper(:code)")
    List<Promocion> porCodigo(String code);
    /** Promociones activas y dentro de sus fechas. */
    @Query("select p from Promocion p where p.active = true and p.startsAt <= :ahora and p.endsAt >= :ahora order by p.id desc")
    List<Promocion> vigentes(Instant ahora);
    @Query("select p from Promocion p where upper(p.code) = upper(:code) and p.active = true and p.startsAt <= :ahora and p.endsAt >= :ahora")
    List<Promocion> vigentePorCodigo(String code, Instant ahora);
    @Query("select p from Promocion p order by p.startsAt desc")
    List<Promocion> todas();
}
