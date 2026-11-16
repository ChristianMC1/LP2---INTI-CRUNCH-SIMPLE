package pe.inticrunch.identidad;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.caja.entity.AsignacionPago;
import pe.inticrunch.caja.entity.Pago;
import pe.inticrunch.caja.repository.*;
import pe.inticrunch.caja.service.CajaService;
import pe.inticrunch.clientes.repository.MovimientoPuntosRepository;
import pe.inticrunch.preparacion.entity.Despacho;
import pe.inticrunch.preparacion.repository.AsignacionDespachoRepository;
import pe.inticrunch.preparacion.repository.DespachoRepository;
import pe.inticrunch.inventario.entity.Almacen;
import pe.inticrunch.inventario.repository.AlmacenRepository;
import pe.inticrunch.inventario.repository.MovimientoInventarioRepository;
import pe.inticrunch.operaciones.Filas;
import pe.inticrunch.pedidos.NumeroPedido;
import pe.inticrunch.pedidos.PedidoService;
import pe.inticrunch.pedidos.entity.EventoPedido;
import pe.inticrunch.pedidos.entity.PedidoCliente;
import pe.inticrunch.pedidos.repository.EventoPedidoRepository;
import pe.inticrunch.pedidos.repository.PedidoClienteRepository;
import pe.inticrunch.compras.entity.OrdenCompra;
import pe.inticrunch.compras.entity.Proveedor;
import pe.inticrunch.compras.repository.*;
import pe.inticrunch.compras.service.ComprasService;
import pe.inticrunch.reservas.service.ReservaService;
import pe.inticrunch.personal.entity.Turno;
import pe.inticrunch.personal.repository.AsignacionTurnoRepository;
import pe.inticrunch.personal.repository.TurnoRepository;

/** Historial de la persona conectada: sus compras como cliente y, si trabaja en el local, lo que registró. */
@Service
public class HistorialService {
    private static final PageRequest CIEN = PageRequest.of(0, 100);
    private final CuentaService accounts;
    private final PedidoService orders;
    private final CajaService cash;
    private final ReservaService reservations;
    private final ComprasService compras;
    private final PedidoClienteRepository pedidos;
    private final EventoPedidoRepository eventos;
    private final MovimientoPuntosRepository puntos;
    private final PagoRepository pagos;
    private final AsignacionPagoRepository asignaciones;
    private final DevolucionRepository devoluciones;
    private final ComprobanteRepository comprobantes;
    private final SesionCajaRepository cajas;
    private final OrdenCompraRepository ordenes;
    private final RecepcionRepository recepciones;
    private final ProveedorRepository proveedores;
    private final AlmacenRepository almacenes;
    private final MovimientoInventarioRepository movimientos;
    private final AsignacionTurnoRepository turnosAsignados;
    private final TurnoRepository turnos;
    private final AsignacionDespachoRepository entregas;
    private final DespachoRepository despachos;
    private final NumeroPedido numero;

    public HistorialService(CuentaService accounts, PedidoService orders, CajaService cash, ReservaService reservations, ComprasService compras, PedidoClienteRepository pedidos,
                            EventoPedidoRepository eventos, MovimientoPuntosRepository puntos, PagoRepository pagos, AsignacionPagoRepository asignaciones,
                            DevolucionRepository devoluciones, ComprobanteRepository comprobantes, SesionCajaRepository cajas, OrdenCompraRepository ordenes,
                            RecepcionRepository recepciones, ProveedorRepository proveedores, AlmacenRepository almacenes, MovimientoInventarioRepository movimientos,
                            AsignacionTurnoRepository turnosAsignados, TurnoRepository turnos, AsignacionDespachoRepository entregas, DespachoRepository despachos, NumeroPedido numero) {
        this.accounts = accounts; this.orders = orders; this.cash = cash; this.reservations = reservations; this.compras = compras; this.pedidos = pedidos; this.eventos = eventos;
        this.puntos = puntos; this.pagos = pagos; this.asignaciones = asignaciones; this.devoluciones = devoluciones; this.comprobantes = comprobantes; this.cajas = cajas;
        this.ordenes = ordenes; this.recepciones = recepciones; this.proveedores = proveedores; this.almacenes = almacenes; this.movimientos = movimientos;
        this.turnosAsignados = turnosAsignados; this.turnos = turnos; this.entregas = entregas; this.despachos = despachos; this.numero = numero;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> history(Authentication auth) {
        var profile = accounts.profile(auth);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile", profile);
        String subject = auth.getName();
        Long customerId = profile.get("customerId") == null ? null : ((Number) profile.get("customerId")).longValue();
        var numeros = numero.todos();

        // ---------- Como cliente ----------
        List<Map<String, Object>> myOrders = new ArrayList<>();
        BigDecimal spent = BigDecimal.ZERO;
        for (PedidoCliente o : pedidos.delCliente(subject, customerId == null ? -1L : customerId, PageRequest.of(0, 60))) {
            var order = orders.get(o.getId(), true);
            order.put("history", orders.history(o.getId()));
            order.put("documents", cash.orderDocuments(o.getId()));
            if ("COMPLETED".equals(order.get("status"))) spent = spent.add((BigDecimal) order.get("total"));
            myOrders.add(order);
        }
        result.put("orders", myOrders);
        result.put("reservations", reservations.customerReservations(subject, customerId));
        if (customerId != null) {
            result.put("points", puntos.puntosDelCliente(customerId));
            result.put("pointMovements", puntos.delCliente(customerId, CIEN).stream().map(m -> Filas.of("Pedido", m.getOrderId() == null ? null : numeros.get(m.getOrderId()),
                "Puntos", m.getPoints(), "Motivo", m.getReason(), "Fecha", m.getCreatedAt())).toList());
        } else { result.put("points", 0); result.put("pointMovements", List.of()); }
        result.put("totalSpent", spent);

        // ---------- Como colaborador ----------
        @SuppressWarnings("unchecked") var roles = (List<String>) profile.get("roles");
        boolean staff = roles.stream().anyMatch(role -> !role.equals("CLIENTE"));
        if (!staff) return result;
        String name = Actor.name(auth), username = auth.getPrincipal() instanceof Jwt jwt ? Objects.toString(jwt.getClaimAsString("preferred_username"), subject) : subject;
        List<String> me = List.of(name, subject, username);
        List<Map<String, Object>> sections = new ArrayList<>();
        Long employee = profile.get("employeeId") == null ? null : ((Number) profile.get("employeeId")).longValue();

        if (employee != null) {
            Map<Long, Turno> turno = turnos.findAll().stream().collect(Collectors.toMap(Turno::getId, t -> t));
            section(sections, "shifts", "Mis turnos", "Tus horarios, con tu entrada, salida y tardanza.", turnosAsignados.findByEmployeeId(employee).stream()
                .sorted(Comparator.comparing((pe.inticrunch.personal.entity.AsignacionTurno a) -> turno.get(a.getShiftId()).getStartsAt()).reversed()).limit(100)
                .map(a -> { Turno s = turno.get(a.getShiftId());
                    return Filas.of("Turno", s.getName(), "Grupo", s.getGroupName() == null ? "—" : s.getGroupName(), "Inicio", s.getStartsAt(), "Fin", s.getEndsAt(), "Función", a.getRoleName(),
                        "Estado", a.getAttendanceStatus(), "Entrada", a.getCheckIn(), "Salida", a.getCheckOut(), "Tardanza (min)", a.getLateMinutes()); }).toList());
        }

        // Pedidos en los que cambió el estado: uno por pedido, con su última acción.
        Map<Long, Instant> ultima = new LinkedHashMap<>();
        for (EventoPedido e : eventos.hechosPor(me)) ultima.merge(e.getOrderId(), e.getCreatedAt(), (a, b) -> a.isAfter(b) ? a : b);
        Map<Long, PedidoCliente> atendidos = pedidos.findAllById(ultima.keySet()).stream().collect(Collectors.toMap(PedidoCliente::getId, o -> o));
        section(sections, "handled", "Pedidos que atendí", "Pedidos en los que cambiaste el estado (preparar, listo, entregar…).", ultima.entrySet().stream()
            .sorted(Map.Entry.<Long, Instant>comparingByValue().reversed()).limit(100).filter(e -> atendidos.containsKey(e.getKey()))
            .map(e -> { PedidoCliente o = atendidos.get(e.getKey());
                return Filas.of("Pedido", numeros.get(o.getId()), "Cliente", o.getCustomerName(), "Atención", o.getChannel(), "Estado", o.getStatus(), "Total", o.getTotal(), "Última acción", e.getValue()); }).toList());

        var misPagos = pagos.findByCreatedByInOrderByIdDesc(me).stream().limit(100).toList();
        Map<Long, BigDecimal> devuelto = new HashMap<>();
        for (Object[] row : devoluciones.devueltoPorPago()) devuelto.put((Long) row[0], (BigDecimal) row[1]);
        Map<Long, List<AsignacionPago>> porPago = misPagos.isEmpty() ? Map.of()
            : asignaciones.findByPaymentIdIn(misPagos.stream().map(Pago::getId).toList()).stream().collect(Collectors.groupingBy(AsignacionPago::getPaymentId));
        List<Map<String, Object>> cobros = new ArrayList<>();
        for (Pago p : misPagos) for (AsignacionPago a : porPago.getOrDefault(p.getId(), List.of()))
            cobros.add(Filas.of("_paymentId", p.getId(), "_documentId", comprobantes.findByPaymentIdOrderById(p.getId()).stream().findFirst().map(d -> d.getId()).orElse(null),
                "Pedido", numeros.get(a.getOrderId()), "Medio", p.getMethod(), "Monto", p.getAmount(), "Propina", p.getTip(), "Devuelto", devuelto.getOrDefault(p.getId(), BigDecimal.ZERO), "Fecha", p.getCreatedAt()));
        section(sections, "payments", "Cobros que registré", "Pagos que cobraste, con su boleta o factura.", cobros);

        section(sections, "cash", "Cajas que abrí", "Aperturas y cierres de caja a tu nombre.", cajas.findByOpenedByInOrderByIdDesc(me).stream().limit(100)
            .map(s -> Filas.of("Apertura", s.getOpenedAt(), "Estado", s.getStatus(), "Fondo inicial", s.getOpeningAmount(), "Cierre", s.getClosedAt(), "Contado", s.getClosingAmount(), "Diferencia", s.getDifference())).toList());

        Map<Long, String> proveedor = proveedores.findAll().stream().collect(Collectors.toMap(Proveedor::getId, Proveedor::getName));
        var misOrdenes = ordenes.registradasPor(me, CIEN);
        var detalle = compras.detalleDeOrdenes(misOrdenes.stream().map(OrdenCompra::getId).toList());
        section(sections, "purchases", "Compras que hice", "Órdenes de compra a proveedores que registraste.", misOrdenes.stream()
            .map(p -> Filas.of("Proveedor", proveedor.get(p.getSupplierId()), "Qué se compró", detalle.get(p.getId()), "Estado", p.getStatus(), "Total", p.getTotal(), "Fecha", p.getCreatedAt())).toList());

        Map<Long, String> almacen = almacenes.findAll().stream().collect(Collectors.toMap(Almacen::getId, Almacen::getName));
        Map<Long, Long> proveedorDeOrden = ordenes.findAll().stream().collect(Collectors.toMap(OrdenCompra::getId, OrdenCompra::getSupplierId));
        section(sections, "receipts", "Mercadería que recibí", "Recepciones de compras que registraste.", recepciones.recibidasPor(me, CIEN).stream()
            .map(r -> Filas.of("_order", r.getPurchaseOrderId(), "Proveedor", proveedor.get(proveedorDeOrden.get(r.getPurchaseOrderId())), "Documento", r.getDocumentNumber(),
                "Almacén", almacen.get(r.getWarehouseId()), "Fecha", r.getReceivedAt())).toList());

        section(sections, "inventory", "Movimientos de inventario", "Entradas, salidas, mermas y conteos que registraste.", movimientos.registradosPor(me, CIEN).stream()
            .map(m -> Filas.of("Tipo", m.getKind(), "Almacén", m.getWarehouseId() == null ? null : almacen.get(m.getWarehouseId()), "Motivo", m.getNote(), "Fecha", m.getCreatedAt())).toList());

        if (employee != null) {
            var asignadas = entregas.delRepartidor(employee).stream().limit(100).toList();
            Map<Long, Despacho> despacho = despachos.findAllById(asignadas.stream().map(a -> a.getDispatchId()).toList()).stream().collect(Collectors.toMap(Despacho::getId, d -> d));
            Map<Long, PedidoCliente> pedidoDe = pedidos.findAllById(despacho.values().stream().map(Despacho::getOrderId).toList()).stream().collect(Collectors.toMap(PedidoCliente::getId, o -> o));
            section(sections, "deliveries", "Mis entregas", "Pedidos de delivery que te asignaron.", asignadas.stream().filter(a -> despacho.containsKey(a.getDispatchId())).map(a -> {
                Despacho d = despacho.get(a.getDispatchId()); PedidoCliente o = pedidoDe.get(d.getOrderId());
                return Filas.of("Pedido", numeros.get(d.getOrderId()), "Cliente", o == null ? null : o.getCustomerName(), "Dirección", d.getAddress(), "Zona", d.getZone(),
                    "Estado", d.getStatus(), "Asignado", a.getAssignedAt(), "Entregado", d.getDeliveredAt());
            }).toList());
        }
        result.put("work", sections);
        return result;
    }

    private static void section(List<Map<String, Object>> sections, String key, String title, String help, List<Map<String, Object>> rows) {
        if (!rows.isEmpty()) sections.add(Map.of("key", key, "title", title, "help", help, "rows", rows));
    }
}
