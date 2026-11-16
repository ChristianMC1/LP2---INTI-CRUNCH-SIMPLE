package pe.inticrunch.caja.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.caja.repository.AsignacionPagoRepository;
import pe.inticrunch.caja.repository.DevolucionRepository;
import pe.inticrunch.operaciones.Validaciones;
import pe.inticrunch.pedidos.repository.PedidoClienteRepository;

/** Cuánto se pagó de un pedido y cuánto falta. */
@Service
public class SaldoService {
    private final AsignacionPagoRepository asignaciones;
    private final DevolucionRepository devoluciones;
    private final PedidoClienteRepository pedidos;
    public SaldoService(AsignacionPagoRepository asignaciones, DevolucionRepository devoluciones, PedidoClienteRepository pedidos) { this.asignaciones = asignaciones; this.devoluciones = devoluciones; this.pedidos = pedidos; }

    @Transactional(readOnly = true)
    public BigDecimal paid(long orderId) { return asignaciones.pagadoDelPedido(orderId).subtract(devoluciones.devueltoDelPedido(orderId)); }
    @Transactional(readOnly = true)
    public BigDecimal due(long orderId) { return pedidos.findById(orderId).orElseThrow(Validaciones::noExiste).getTotal().subtract(paid(orderId)); }
    // REGLA CAJA-20: El pedido debe estar pagado antes de la entrega.
    public void requirePaid(long orderId) { if (due(orderId).signum() > 0) throw new IllegalStateException("El pedido debe estar pagado antes de la entrega."); }
}
