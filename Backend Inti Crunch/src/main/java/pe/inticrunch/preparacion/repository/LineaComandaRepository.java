package pe.inticrunch.preparacion.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.preparacion.entity.LineaComanda;

public interface LineaComandaRepository extends JpaRepository<LineaComanda, LineaComanda.Clave> {
    List<LineaComanda> findByTicketIdIn(Collection<Long> ticketIds);
}
