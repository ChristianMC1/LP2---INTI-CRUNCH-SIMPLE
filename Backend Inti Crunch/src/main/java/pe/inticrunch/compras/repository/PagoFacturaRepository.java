package pe.inticrunch.compras.repository;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.compras.entity.PagoFactura;

public interface PagoFacturaRepository extends JpaRepository<PagoFactura, Long> {
    @Query("select coalesce(sum(p.amount), 0) from PagoFactura p where p.invoiceId = :invoice")
    BigDecimal pagadoDeFactura(Long invoice);
    /** Total pagado de cada factura: [factura, monto]. */
    @Query("select p.invoiceId, sum(p.amount) from PagoFactura p group by p.invoiceId")
    List<Object[]> pagadoPorFactura();
}
