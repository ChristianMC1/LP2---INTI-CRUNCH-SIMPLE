package pe.inticrunch.caja.repository;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.caja.entity.MedioPago;

public interface MedioPagoRepository extends JpaRepository<MedioPago, Long> {
    List<MedioPago> findByPaymentIdOrderById(Long paymentId);
    /** Efectivo (más propinas) cobrado en una caja. */
    @Query("select coalesce(sum(m.amount + m.tip), 0) from MedioPago m, Pago p where p.id = m.paymentId and p.cashSessionId = :session and p.status = 'CONFIRMED' and m.method = 'CASH'")
    BigDecimal efectivoRecibido(Long session);
    /** Monto pagado con cada medio dentro de un pago: [medio, monto]. */
    @Query("select m.method, sum(m.amount) from MedioPago m where m.paymentId = :payment group by m.method order by m.method")
    List<Object[]> pagadoPorMedio(Long payment);
}
