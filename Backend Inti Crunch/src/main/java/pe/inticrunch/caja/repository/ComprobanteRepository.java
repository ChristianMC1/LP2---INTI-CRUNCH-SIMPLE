package pe.inticrunch.caja.repository;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.caja.entity.Comprobante;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
    @Query("select d from Comprobante d order by d.id desc")
    List<Comprobante> recientes(Pageable page);
    List<Comprobante> findByPaymentIdOrderById(Long paymentId);
    List<Comprobante> findByOrderIdOrderById(Long orderId);
    List<Comprobante> findByOrderIdInOrderById(Collection<Long> orderIds);
    boolean existsByIdAndOrderId(Long id, Long orderId);
}
