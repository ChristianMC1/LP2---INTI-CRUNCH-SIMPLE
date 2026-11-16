package pe.inticrunch.pedidos;

import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.pedidos.repository.PedidoClienteRepository;

/**
 * Número de pedido consecutivo (1, 2, 3…) que ve la gente, aunque el identificador interno tenga saltos.
 * Es la posición del pedido al ordenarlos por su identificador.
 */
@Component
public class NumeroPedido {
    private final PedidoClienteRepository pedidos;
    public NumeroPedido(PedidoClienteRepository pedidos) { this.pedidos = pedidos; }

    @Transactional(readOnly = true)
    public long de(long orderId) { return pedidos.countByIdLessThanEqual(orderId); }

    /** Números de muchos pedidos con una sola consulta. */
    @Transactional(readOnly = true)
    public Map<Long, Long> todos() {
        Map<Long, Long> result = new HashMap<>();
        long n = 0;
        for (Long id : pedidos.todosLosIds()) result.put(id, ++n);
        return result;
    }
}
