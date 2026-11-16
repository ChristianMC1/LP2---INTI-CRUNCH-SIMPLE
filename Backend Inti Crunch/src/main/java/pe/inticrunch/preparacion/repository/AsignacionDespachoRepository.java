package pe.inticrunch.preparacion.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.preparacion.entity.AsignacionDespacho;

public interface AsignacionDespachoRepository extends JpaRepository<AsignacionDespacho, Long> {
    List<AsignacionDespacho> findByDispatchIdAndEndedAtIsNull(Long dispatchId);
    boolean existsByEmployeeIdAndEndedAtIsNull(Long employeeId);
    List<AsignacionDespacho> findByDispatchIdInAndEndedAtIsNull(Collection<Long> dispatchIds);
    @Query("select count(a) > 0 from AsignacionDespacho a, Despacho d where d.id = a.dispatchId and d.orderId = :order and a.endedAt is null")
    boolean pedidoTieneRepartidor(Long order);
    @Query("select count(a) > 0 from AsignacionDespacho a, Despacho d where d.id = a.dispatchId and a.employeeId = :employee and a.endedAt is null and d.status = 'ON_ROUTE'")
    boolean estaEnRuta(Long employee);
    @Query("select count(a) from AsignacionDespacho a, Despacho d where d.id = a.dispatchId and a.employeeId = :employee and a.endedAt is null and d.status not in ('DELIVERED','CANCELLED')")
    long entregasPendientes(Long employee);
    @Query("select a from AsignacionDespacho a where a.employeeId = :employee order by a.id desc")
    List<AsignacionDespacho> delRepartidor(Long employee);
}
