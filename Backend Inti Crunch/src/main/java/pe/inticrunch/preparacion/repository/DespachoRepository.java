package pe.inticrunch.preparacion.repository;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.preparacion.entity.Despacho;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {
    List<Despacho> findByOrderId(Long orderId);
    @Query("select d from Despacho d order by d.id desc")
    List<Despacho> recientes(Pageable page);
    @Query("select d from Despacho d where d.status not in ('DELIVERED','CANCELLED') order by d.id desc")
    List<Despacho> pendientes();
}
