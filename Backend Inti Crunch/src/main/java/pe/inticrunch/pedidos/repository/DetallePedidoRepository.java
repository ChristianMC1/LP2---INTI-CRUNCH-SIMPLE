package pe.inticrunch.pedidos.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.pedidos.entity.DetallePedido;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
    List<DetallePedido> findByOrderIdOrderById(Long orderId);
    List<DetallePedido> findByOrderIdOrderByProductIdAscIdAsc(Long orderId);
    List<DetallePedido> findByOrderIdInOrderById(Collection<Long> orderIds);
}
