package pe.inticrunch.operaciones.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.operaciones.entity.Local;

public interface LocalRepository extends JpaRepository<Local, Long> {
    /** Bloquea el local mientras se registra un pedido o un pago, para evitar duplicados por doble clic. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Local l where l.id = :id")
    Optional<Local> bloquear(Long id);

    List<Local> findByActiveTrueOrderByName();
}
