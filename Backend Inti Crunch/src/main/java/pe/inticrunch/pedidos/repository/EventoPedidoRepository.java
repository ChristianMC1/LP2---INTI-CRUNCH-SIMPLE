package pe.inticrunch.pedidos.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.pedidos.entity.EventoPedido;

public interface EventoPedidoRepository extends JpaRepository<EventoPedido, Long> {
    List<EventoPedido> findByOrderIdOrderById(Long orderId);
    @Query("select e from EventoPedido e where e.changedBy in :actores order by e.createdAt desc")
    List<EventoPedido> hechosPor(Collection<String> actores);
}
