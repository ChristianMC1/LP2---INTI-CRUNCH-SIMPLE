package pe.inticrunch.caja.repository;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.caja.entity.MovimientoCaja;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    /** Ingresos menos retiros de una caja. */
    @Query("select coalesce(sum(case when m.kind = 'IN' then m.amount else -m.amount end), 0) from MovimientoCaja m where m.cashSessionId = :session")
    BigDecimal netoDeSesion(Long session);
    @Query("select m from MovimientoCaja m order by m.id desc")
    List<MovimientoCaja> recientes(Pageable page);
    List<MovimientoCaja> findByCreatedByOrderByIdDesc(String createdBy);
}
