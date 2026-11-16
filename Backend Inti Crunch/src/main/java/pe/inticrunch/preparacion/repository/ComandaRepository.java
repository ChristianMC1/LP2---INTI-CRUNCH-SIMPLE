package pe.inticrunch.preparacion.repository;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.preparacion.entity.Comanda;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {
    List<Comanda> findByOrderId(Long orderId);
    @Query("select k from Comanda k order by k.receivedAt desc")
    List<Comanda> recientes(Pageable page);
    @Query("select k from Comanda k where k.readyAt is not null and k.receivedAt >= :desde and k.receivedAt < :hasta")
    List<Comanda> listasEntre(Instant desde, Instant hasta);
}
