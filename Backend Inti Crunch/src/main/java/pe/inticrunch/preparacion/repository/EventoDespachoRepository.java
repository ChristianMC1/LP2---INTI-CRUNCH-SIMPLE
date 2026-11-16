package pe.inticrunch.preparacion.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.preparacion.entity.EventoDespacho;

public interface EventoDespachoRepository extends JpaRepository<EventoDespacho, Long> {
    List<EventoDespacho> findByDispatchIdOrderById(Long dispatchId);
}
