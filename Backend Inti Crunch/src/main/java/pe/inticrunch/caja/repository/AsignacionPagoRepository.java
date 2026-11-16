package pe.inticrunch.caja.repository;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.caja.entity.AsignacionPago;

public interface AsignacionPagoRepository extends JpaRepository<AsignacionPago, AsignacionPago.Clave> {
    @Query("select coalesce(sum(a.amount), 0) from AsignacionPago a, Pago p where p.id = a.paymentId and a.orderId = :order and p.status = 'CONFIRMED'")
    BigDecimal pagadoDelPedido(Long order);
    List<AsignacionPago> findByPaymentId(Long paymentId);
    List<AsignacionPago> findByPaymentIdIn(Collection<Long> paymentIds);
    boolean existsByOrderId(Long orderId);
    /** Total cobrado (pagos confirmados) de cada pedido: [pedido, monto]. */
    @Query("select a.orderId, sum(a.amount) from AsignacionPago a, Pago p where p.id = a.paymentId and p.status = 'CONFIRMED' group by a.orderId")
    List<Object[]> pagadoPorPedido();
}
