package pe.inticrunch.compras.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.compras.entity.LineaSolicitud;

public interface LineaSolicitudRepository extends JpaRepository<LineaSolicitud, LineaSolicitud.Clave> {
    List<LineaSolicitud> findByRequestId(Long requestId);
    List<LineaSolicitud> findByRequestIdIn(Collection<Long> requestIds);
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("delete from LineaSolicitud l where l.requestId = :request")
    void borrarDeSolicitud(Long request);
}
