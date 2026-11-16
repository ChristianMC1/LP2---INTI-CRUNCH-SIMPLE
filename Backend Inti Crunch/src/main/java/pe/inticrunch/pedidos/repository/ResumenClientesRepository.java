package pe.inticrunch.pedidos.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.Repository;
import pe.inticrunch.pedidos.entity.PedidoCliente;

/** Totales de pedidos por cliente para la lista de clientes. */
public interface ResumenClientesRepository extends Repository<PedidoCliente, Long> {
    @Query("select o.customerId, sum(case when o.status <> 'CANCELLED' then 1 else 0 end), sum(case when o.status = 'COMPLETED' then o.total else 0 end) from PedidoCliente o where o.customerId is not null group by o.customerId")
    List<Object[]> pedidosYConsumoPorCliente();
}
