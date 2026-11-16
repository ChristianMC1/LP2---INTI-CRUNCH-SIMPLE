package pe.inticrunch.compras.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.compras.entity.OrdenCompra;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from OrdenCompra p where p.id = :id")
    Optional<OrdenCompra> bloquear(Long id);
    @Query("select p from OrdenCompra p order by p.id desc")
    List<OrdenCompra> recientes(Pageable page);
    boolean existsByRequestId(Long requestId);
    @Query("select count(p) > 0 from OrdenCompra p where p.supplierId = :supplier and p.status in ('PENDING','PARTIAL')")
    boolean tienePendientes(Long supplier);
    @Query("select p from OrdenCompra p where p.status <> 'CANCELLED' order by p.id desc")
    List<OrdenCompra> noCanceladas();
    @Query("select p from OrdenCompra p where p.createdBy in :actores order by p.id desc")
    List<OrdenCompra> registradasPor(Collection<String> actores, Pageable page);
}
