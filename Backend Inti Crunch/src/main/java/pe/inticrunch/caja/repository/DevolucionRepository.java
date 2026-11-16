package pe.inticrunch.caja.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.caja.entity.Devolucion;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {
    @Query("select coalesce(sum(r.amount), 0) from Devolucion r where r.orderId = :order")
    BigDecimal devueltoDelPedido(Long order);
    @Query("select coalesce(sum(r.amount), 0) from Devolucion r where r.paymentId = :payment")
    BigDecimal devueltoDelPago(Long payment);
    @Query("select coalesce(sum(r.amount), 0) from Devolucion r where r.paymentId = :payment and r.method = :method")
    BigDecimal devueltoPorMedio(Long payment, String method);
    @Query("select coalesce(sum(r.amount), 0) from Devolucion r where r.cashSessionId = :session and r.method = 'CASH'")
    BigDecimal efectivoDevuelto(Long session);
    @Query("select coalesce(sum(r.amount), 0) from Devolucion r where r.createdAt >= :desde and r.createdAt < :hasta")
    BigDecimal devueltoEntre(Instant desde, Instant hasta);
    @Query("select coalesce(sum(r.amount), 0) from Devolucion r")
    BigDecimal totalDevuelto();
    /** Total devuelto por cada pago: [pago, monto]. */
    @Query("select r.paymentId, sum(r.amount) from Devolucion r group by r.paymentId")
    List<Object[]> devueltoPorPago();
    /** Total devuelto de cada pedido: [pedido, monto]. */
    @Query("select r.orderId, sum(r.amount) from Devolucion r group by r.orderId")
    List<Object[]> devueltoPorPedido();
    List<Devolucion> findByCreatedAtGreaterThanEqual(Instant desde);
    @Query("select r from Devolucion r order by r.id desc")
    List<Devolucion> recientes(Pageable page);
}
