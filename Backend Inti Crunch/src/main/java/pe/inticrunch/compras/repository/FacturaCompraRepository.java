package pe.inticrunch.compras.repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.compras.entity.FacturaCompra;

public interface FacturaCompraRepository extends JpaRepository<FacturaCompra, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FacturaCompra f where f.id = :id")
    Optional<FacturaCompra> bloquear(Long id);
    @Query("select coalesce(sum(f.total), 0) from FacturaCompra f where f.purchaseOrderId = :purchase")
    BigDecimal facturadoDeOrden(Long purchase);
    @Query("select f from FacturaCompra f order by f.dueDate, f.id")
    List<FacturaCompra> porVencimiento();
}
