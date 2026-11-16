package pe.inticrunch.inventario.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.inventario.entity.LineaMovimiento;

public interface LineaMovimientoRepository extends JpaRepository<LineaMovimiento, Long> {
    List<LineaMovimiento> findByMovementIdOrderById(Long movementId);
    List<LineaMovimiento> findByMovementIdInOrderById(Collection<Long> movementIds);
}
