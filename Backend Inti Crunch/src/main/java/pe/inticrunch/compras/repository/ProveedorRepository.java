package pe.inticrunch.compras.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.compras.entity.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findByActiveTrueOrderById();
    List<Proveedor> findByActiveTrueOrderByName();
    Optional<Proveedor> findByIdAndActiveTrue(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Proveedor s where s.id = :id")
    Optional<Proveedor> bloquear(Long id);
}
