package pe.inticrunch.pedidos.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.pedidos.entity.OpcionDetallePedido;

public interface OpcionDetallePedidoRepository extends JpaRepository<OpcionDetallePedido, OpcionDetallePedido.Clave> {
    List<OpcionDetallePedido> findByOrderLineIdOrderByOptionId(Long orderLineId);
    List<OpcionDetallePedido> findByOrderLineIdInOrderByOptionId(Collection<Long> orderLineIds);
}
