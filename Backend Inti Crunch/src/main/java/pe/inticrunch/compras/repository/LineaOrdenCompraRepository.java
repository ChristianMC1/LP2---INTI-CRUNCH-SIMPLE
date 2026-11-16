package pe.inticrunch.compras.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.compras.entity.LineaOrdenCompra;

public interface LineaOrdenCompraRepository extends JpaRepository<LineaOrdenCompra, Long> {
    List<LineaOrdenCompra> findByPurchaseOrderIdOrderById(Long purchaseOrderId);
    List<LineaOrdenCompra> findByPurchaseOrderIdIn(Collection<Long> purchaseOrderIds);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LineaOrdenCompra l where l.id = :line and l.purchaseOrderId = :purchase")
    Optional<LineaOrdenCompra> bloquear(Long line, Long purchase);
    @Query("select count(l) from LineaOrdenCompra l where l.purchaseOrderId = :purchase and l.receivedQuantity < l.quantity")
    long pendientes(Long purchase);
}
