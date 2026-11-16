package pe.inticrunch.preparacion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.caja.service.SaldoService;
import pe.inticrunch.catalogo.Producto;
import pe.inticrunch.catalogo.ProductoRepository;
import pe.inticrunch.clientes.entity.MovimientoPuntos;
import pe.inticrunch.clientes.repository.MovimientoPuntosRepository;
import pe.inticrunch.preparacion.entity.*;
import pe.inticrunch.preparacion.repository.*;
import pe.inticrunch.identidad.CuentaService;
import pe.inticrunch.inventario.service.InventarioService;
import pe.inticrunch.operaciones.Filas;
import pe.inticrunch.operaciones.Validaciones;
import pe.inticrunch.pedidos.NumeroPedido;
import pe.inticrunch.pedidos.entity.*;
import pe.inticrunch.pedidos.repository.*;
import pe.inticrunch.personal.entity.Colaborador;
import pe.inticrunch.personal.repository.ColaboradorRepository;

/** Cocina, entregas, repartidores, zonas de reparto e incidencias. */
@Service
public class PreparacionService extends Validaciones {
    private static final List<String> ROLES_REPARTO = List.of("OPERACIONES", "REPARTIDOR");
    private final InventarioService inventory;
    private final SaldoService balances;
    private final PedidoClienteRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final OpcionDetallePedidoRepository opcionesDetalle;
    private final EventoPedidoRepository eventosPedido;
    private final ComandaRepository comandas;
    private final LineaComandaRepository lineasComanda;
    private final DespachoRepository despachos;
    private final AsignacionDespachoRepository asignaciones;
    private final EventoDespachoRepository eventosDespacho;
    private final ZonaRepartoRepository zonas;
    private final IncidenciaRepository incidencias;
    private final ColaboradorRepository colaboradores;
    private final MovimientoPuntosRepository puntos;
    private final ProductoRepository productos;
    private final NumeroPedido numero;

    public PreparacionService(InventarioService inventory, SaldoService balances, PedidoClienteRepository pedidos, DetallePedidoRepository detalles, OpcionDetallePedidoRepository opcionesDetalle,
                              EventoPedidoRepository eventosPedido, ComandaRepository comandas, LineaComandaRepository lineasComanda, DespachoRepository despachos, AsignacionDespachoRepository asignaciones,
                              EventoDespachoRepository eventosDespacho, ZonaRepartoRepository zonas, IncidenciaRepository incidencias, ColaboradorRepository colaboradores,
                              MovimientoPuntosRepository puntos, ProductoRepository productos, NumeroPedido numero) {
        this.inventory = inventory; this.balances = balances; this.pedidos = pedidos; this.detalles = detalles; this.opcionesDetalle = opcionesDetalle; this.eventosPedido = eventosPedido;
        this.comandas = comandas; this.lineasComanda = lineasComanda; this.despachos = despachos; this.asignaciones = asignaciones; this.eventosDespacho = eventosDespacho;
        this.zonas = zonas; this.incidencias = incidencias; this.colaboradores = colaboradores; this.puntos = puntos; this.productos = productos; this.numero = numero;
    }

    // ---------- Listas ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> dispatches() {
        var numeros = numero.todos();
        var lista = despachos.recientes(PageRequest.of(0, 300));
        Map<Long, Long> repartidorDe = asignaciones.findByDispatchIdInAndEndedAtIsNull(lista.stream().map(Despacho::getId).toList()).stream().collect(Collectors.toMap(AsignacionDespacho::getDispatchId, AsignacionDespacho::getEmployeeId, (a, b) -> b));
        Map<Long, String> nombres = colaboradores.findAllById(new HashSet<>(repartidorDe.values())).stream().collect(Collectors.toMap(Colaborador::getId, Colaborador::getFullName));
        return lista.stream().map(d -> {
            PedidoCliente o = pedidos.findById(d.getOrderId()).orElseThrow();
            return Filas.of("Despacho", d.getId(), "Pedido", numeros.get(o.getId()), "Cliente", o.getCustomerName(), "Tipo", d.getDeliveryType(), "Dirección", d.getAddress(),
                "Zona", d.getZone(), "Estado", d.getStatus(), "Repartidor", nombres.get(repartidorDe.get(d.getId())), "Estimado", d.getEstimatedAt());
        }).toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> kitchenTickets() {
        var numeros = numero.todos();
        return comandas.recientes(PageRequest.of(0, 300)).stream().map(k -> {
            PedidoCliente o = pedidos.findById(k.getOrderId()).orElseThrow();
            return Filas.of("Comanda", k.getId(), "Pedido", numeros.get(o.getId()), "Canal", o.getChannel(), "Cliente", o.getCustomerName(), "Estado", k.getStatus(),
                "Recibido", k.getReceivedAt(), "Detalle", detalleTexto(o.getId()), "Observaciones", o.getNotes());
        }).toList();
    }
    /** "2 x Papas (bien doradas) [Extra cremas] · 1 x Chicha" */
    private String detalleTexto(long order) {
        List<String> partes = new ArrayList<>();
        for (DetallePedido l : detalles.findByOrderIdOrderById(order)) {
            String nombre = l.getProductName() != null ? l.getProductName() : productos.findById(l.getProductId()).map(Producto::getName).orElse("Producto");
            var opciones = opcionesDetalle.findByOrderLineIdOrderByOptionId(l.getId()).stream().map(OpcionDetallePedido::getOptionName).toList();
            partes.add(l.getQuantity() + " x " + nombre + (l.getNotes() == null || l.getNotes().isBlank() ? "" : " (" + l.getNotes() + ")") + (opciones.isEmpty() ? "" : " [" + String.join(", ", opciones) + "]"));
        }
        return String.join(" · ", partes);
    }

    // ---------- Estados del pedido ----------
    @Transactional public void transition(long orderId, String next, String actor) {
        PedidoCliente order = pedidos.bloquear(orderId).orElseThrow(Validaciones::noExiste);
        String previous = order.getStatus();
        if (previous.equals(next)) return;
        boolean valid = switch (previous) {
            case "RECEIVED" -> Set.of("PREPARING", "CANCELLED").contains(next);
            case "PREPARING" -> Set.of("READY", "CANCELLED").contains(next);
            case "READY" -> Set.of("DISPATCHED", "COMPLETED", "CANCELLED").contains(next);
            case "DISPATCHED" -> next.equals("COMPLETED");
            default -> false;
        };
        // REGLA DELIVERY-01: No se puede cambiar el pedido de 
        if (!valid) throw new IllegalStateException("No se puede cambiar el pedido de " + previous + " a " + next + ".");
        // REGLA DELIVERY-02: Devuelve primero los pagos del pedido antes de cancelarlo.
        if (next.equals("CANCELLED") && balances.paid(orderId).signum() > 0) throw new IllegalStateException("Devuelve primero los pagos del pedido antes de cancelarlo.");
        // REGLA DELIVERY-03: Solo un delivery puede salir a ruta.
        if (next.equals("DISPATCHED") && !order.getChannel().equals("DELIVERY")) throw new IllegalStateException("Solo un delivery puede salir a ruta.");
        if (next.equals("DISPATCHED")) {
            // REGLA DELIVERY-04: Asigna un repartidor antes de salir a ruta.
            if (!asignaciones.pedidoTieneRepartidor(orderId)) throw new IllegalStateException("Asigna un repartidor antes de salir a ruta.");
            balances.requirePaid(orderId);
        }
        if (next.equals("COMPLETED")) {
            // REGLA DELIVERY-05: Registra primero la salida a ruta del delivery.
            if (order.getChannel().equals("DELIVERY") && !previous.equals("DISPATCHED")) throw new IllegalStateException("Registra primero la salida a ruta del delivery.");
            balances.requirePaid(orderId);
        }
        if (next.equals("PREPARING")) inventory.consumeOrder(orderId, actor);
        order.setStatus(next);
        recordOrderStatus(orderId, previous, next, actor);
        if (Set.of("PREPARING", "READY", "CANCELLED").contains(next)) {
            var tickets = comandas.findByOrderId(orderId);
            for (Comanda k : tickets) { k.setStatus(next); if (next.equals("READY")) k.setReadyAt(Instant.now()); }
            lineasComanda.findByTicketIdIn(tickets.stream().map(Comanda::getId).toList()).forEach(l -> l.setStatus(next));
        }
        String dispatchStatus = switch (next) { case "DISPATCHED" -> "ON_ROUTE"; case "COMPLETED" -> "DELIVERED"; default -> next; };
        for (Despacho d : despachos.findByOrderId(orderId)) {
            d.setStatus(dispatchStatus);
            if (dispatchStatus.equals("DELIVERED")) d.setDeliveredAt(Instant.now());
            evento(d.getId(), dispatchStatus, null, actor);
            if (Set.of("DELIVERED", "CANCELLED").contains(dispatchStatus)) asignaciones.findByDispatchIdAndEndedAtIsNull(d.getId()).forEach(a -> a.setEndedAt(Instant.now()));
        }
        // Al entregar, el cliente gana 1 punto por cada sol (una sola vez por pedido).
        if (next.equals("COMPLETED") && order.getCustomerId() != null && !puntos.existsByOrderIdAndReason(orderId, "COMPRA")) {
            MovimientoPuntos m = new MovimientoPuntos();
            m.setCustomerId(order.getCustomerId()); m.setOrderId(orderId); m.setPoints(order.getTotal().setScale(0, RoundingMode.DOWN).intValue()); m.setReason("COMPRA");
            puntos.save(m);
        }
    }
    @Transactional public Map<String, Object> updateKitchen(long ticket, Map<String, Object> data, String actor) {
        Comanda comanda = comandas.findById(ticket).orElseThrow(Validaciones::noExiste);
        transition(comanda.getOrderId(), choice(data, "status", "PREPARING", "READY"), actor);
        return Map.of("message", "Cocina, pedido e inventario sincronizados.");
    }
    @Transactional public Map<String, Object> updateDispatch(long dispatch, Map<String, Object> data, String actor) {
        Despacho despacho = despachos.findById(dispatch).orElseThrow(Validaciones::noExiste);
        String status = choice(data, "status", "ON_ROUTE", "DELIVERED", "CANCELLED");
        transition(despacho.getOrderId(), switch (status) { case "ON_ROUTE" -> "DISPATCHED"; case "DELIVERED" -> "COMPLETED"; default -> "CANCELLED"; }, actor);
        if (!optional(data, "note").isBlank()) evento(dispatch, status, optional(data, "note"), actor);
        return Map.of("message", "Despacho y pedido actualizados.");
    }
    @Transactional public Map<String, Object> assignCourier(long dispatch, Map<String, Object> data) {
        long employee = id(data, "employeeId");
        Despacho despacho = despachos.findById(dispatch).orElseThrow(Validaciones::noExiste);
        PedidoCliente order = pedidos.bloquear(despacho.getOrderId()).orElseThrow(Validaciones::noExiste);
        // REGLA DELIVERY-06: Solo se asignan deliveries pendientes de salida.
        if (!despacho.getDeliveryType().equals("DELIVERY") || Set.of("DISPATCHED", "COMPLETED", "CANCELLED").contains(order.getStatus())) throw new IllegalStateException("Solo se asignan deliveries pendientes de salida.");
        Colaborador repartidor = colaboradores.bloquear(employee)
            .filter(e -> Boolean.TRUE.equals(e.getActive()) && Boolean.TRUE.equals(e.getAvailable()) && e.getRoles().stream().anyMatch(ROLES_REPARTO::contains))
            .orElseThrow(Validaciones::noExiste);
        // REGLA DELIVERY-07: El repartidor está en ruta.
        if (asignaciones.estaEnRuta(employee)) throw new IllegalStateException("El repartidor está en ruta.");
        // REGLA DELIVERY-08: El repartidor no tiene cobertura en la zona del pedido.
        if (!repartidor.getZoneIds().containsAll(despacho.getZoneIds())) throw new IllegalStateException("El repartidor no tiene cobertura en la zona del pedido.");
        asignaciones.findByDispatchIdAndEndedAtIsNull(dispatch).forEach(a -> a.setEndedAt(Instant.now()));
        asignaciones.flush();
        AsignacionDespacho asignacion = new AsignacionDespacho(); asignacion.setDispatchId(dispatch); asignacion.setEmployeeId(employee);
        asignacion = asignaciones.save(asignacion);
        evento(dispatch, "ASSIGNED", "Repartidor: " + repartidor.getFullName(), "OPERACIONES");
        return Map.of("id", asignacion.getId(), "message", "Repartidor asignado; se conserva el estado de preparación.");
    }
    private void evento(long dispatch, String status, String note, String actor) {
        EventoDespacho e = new EventoDespacho(); e.setDispatchId(dispatch); e.setStatus(status); e.setNote(note); e.setCreatedBy(actor);
        eventosDespacho.save(e);
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(long dispatch) {
        return eventosDespacho.findByDispatchIdOrderById(dispatch).stream().map(e -> Filas.of("Estado", e.getStatus(), "Nota", e.getNote(), "Responsable", e.getCreatedBy(), "Fecha", e.getCreatedAt())).toList();
    }
    public void recordOrderStatus(long order, String previous, String next, String actor) {
        EventoPedido e = new EventoPedido(); e.setOrderId(order); e.setOldStatus(previous); e.setNewStatus(next); e.setChangedBy(actor);
        eventosPedido.save(e);
    }
    /** Crea la comanda de cocina y, si no es para comer en el local, la entrega con su zona. */
    @Transactional public void attachOrder(long order, String channel, String address, ZonaReparto zone) {
        Comanda comanda = new Comanda(); comanda.setOrderId(order);
        comanda = comandas.saveAndFlush(comanda);
        for (DetallePedido l : detalles.findByOrderIdOrderById(order)) {
            String categoria = productos.findById(l.getProductId()).map(Producto::getCategory).orElse("");
            LineaComanda linea = new LineaComanda();
            linea.setTicketId(comanda.getId()); linea.setOrderLineId(l.getId()); linea.setStation("Bebidas".equals(categoria) ? "BARRA" : "COCINA");
            lineasComanda.save(linea);
        }
        if (!channel.equals("DINE_IN")) {
            Despacho d = new Despacho();
            d.setOrderId(order); d.setLocationId(1L); d.setDeliveryType(channel); d.setAddress(address); d.setZone(zone == null ? null : zone.getName());
            d.setEstimatedAt(Instant.now().plusSeconds(60L * (zone == null ? 20 : zone.getEstimatedMinutes())));
            if (zone != null) d.getZoneIds().add(zone.getId());
            d = despachos.saveAndFlush(d);
            evento(d.getId(), "QUEUED", null, "SISTEMA");
        }
    }

    // ---------- Zonas ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> zones() {
        return zonas.findByActiveTrueOrderById().stream().map(z -> Filas.of("ID", z.getId(), "Zona", z.getName(), "Cobertura", z.getCoverage(), "Tarifa", z.getFee(), "Minutos", z.getEstimatedMinutes())).toList();
    }
    @Transactional public Map<String, Object> createZone(Map<String, Object> data) {
        String name = str(data, "name");
        // REGLA DELIVERY-09: Ya existe la zona 
        if (zonas.activaPorNombre(name).isPresent()) throw new IllegalArgumentException("Ya existe la zona " + name + ".");
        var anterior = zonas.buscarPorNombre(name).stream().findFirst();
        ZonaReparto zona = anterior.orElseGet(ZonaReparto::new);
        zona.setName(name); zona.setCoverage(str(data, "coverage")); zona.setFee(money(data.get("fee"), "Tarifa")); zona.setEstimatedMinutes(integer(data.get("estimatedMinutes"), "Minutos")); zona.setActive(true);
        zona = zonas.save(zona);
        return Map.of("id", zona.getId(), "message", anterior.isPresent() ? "Zona " + name + " reactivada." : "Zona creada.");
    }
    @Transactional public Map<String, Object> updateZone(long zone, Map<String, Object> data) {
        ZonaReparto zona = zonas.bloquear(zone).orElseThrow(Validaciones::noExiste);
        // REGLA DELIVERY-10: Ya existe otra zona (activa o retirada) con ese nombre. Usa otro nombre.
        if (zonas.otraConNombre(str(data, "name"), zone)) throw new IllegalArgumentException("Ya existe otra zona (activa o retirada) con ese nombre. Usa otro nombre.");
        zona.setName(str(data, "name")); zona.setCoverage(str(data, "coverage")); zona.setFee(money(data.get("fee"), "Tarifa")); zona.setEstimatedMinutes(integer(data.get("estimatedMinutes"), "Minutos"));
        return Map.of("message", "Zona actualizada.");
    }
    @Transactional public Map<String, Object> deleteZone(long zone) {
        ZonaReparto zona = zonas.bloquear(zone).orElseThrow(Validaciones::noExiste);
        colaboradores.findAll().forEach(e -> e.getZoneIds().remove(zone));
        zona.setActive(false);
        return Map.of("message", "Zona retirada. Ya no aparecerá para nuevos pedidos.");
    }

    // ---------- Repartidores ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> couriers() {
        Map<Long, String> nombreZona = zonas.findAll().stream().collect(Collectors.toMap(ZonaReparto::getId, ZonaReparto::getName));
        return colaboradores.activosConRol(ROLES_REPARTO).stream().map(e -> {
            String zonasTexto = e.getZoneIds().stream().sorted().map(nombreZona::get).filter(Objects::nonNull).collect(Collectors.joining(", "));
            return Filas.of("ID", e.getId(), "Repartidor", e.getFullName(), "Teléfono", e.getPhone(), "Disponible", e.getAvailable(), "Zonas", zonasTexto.isBlank() ? "Sin zonas" : zonasTexto, "Entregas asignadas", asignaciones.entregasPendientes(e.getId()));
        }).toList();
    }
    @Transactional(readOnly = true)
    public Map<String, Object> currentCoverage(long employee) {
        Colaborador e = colaboradores.findById(employee).orElseThrow(Validaciones::noExiste);
        return Filas.of("available", e.getAvailable(), "zoneIds", e.getZoneIds().stream().sorted().toList());
    }
    private static String celular(Map<String, Object> data) {
        String phone = CuentaService.cleanPhone(optional(data, "phone"));
        // REGLA DELIVERY-11: El celular debe tener 9 números y empezar con 9.
        if (!phone.isBlank() && !phone.matches("9[0-9]{8}")) throw new IllegalArgumentException("El celular debe tener 9 números y empezar con 9.");
        return phone;
    }
    @Transactional public Map<String, Object> createCourier(Map<String, Object> data) {
        String name = str(data, "fullName");
        Colaborador e = new Colaborador();
        e.setFullName(name); e.setPhone(celular(data)); e.setDni(nullable(data, "dni"));
        e.getRoles().add("REPARTIDOR"); e.getLocationIds().add(1L);
        e = colaboradores.saveAndFlush(e);
        courierCoverage(e.getId(), data);
        return Map.of("id", e.getId(), "message", "Repartidor " + name + " registrado.");
    }
    @Transactional public Map<String, Object> updateCourier(long employee, Map<String, Object> data) {
        Colaborador e = colaboradores.bloquear(employee).orElseThrow(Validaciones::noExiste);
        String phone = celular(data);
        e.setFullName(str(data, "fullName")); e.setPhone(phone);
        courierCoverage(employee, data);
        return Map.of("message", "Repartidor actualizado.");
    }
    @Transactional public Map<String, Object> deleteCourier(long employee) {
        Colaborador e = colaboradores.bloquear(employee).orElseThrow(Validaciones::noExiste);
        // REGLA DELIVERY-12: El repartidor tiene entregas asignadas. Asígnalas a otra persona primero.
        if (asignaciones.entregasPendientes(employee) > 0) throw new IllegalStateException("El repartidor tiene entregas asignadas. Asígnalas a otra persona primero.");
        e.getZoneIds().clear();
        e.getRoles().remove("REPARTIDOR");
        if (e.getRoles().isEmpty()) { e.setActive(false); e.setAvailable(false); }
        return Map.of("message", "Repartidor retirado de la lista.");
    }
    @Transactional public Map<String, Object> courierCoverage(long employee, Map<String, Object> data) {
        Colaborador e = colaboradores.bloquear(employee).filter(c -> Boolean.TRUE.equals(c.getActive())).orElseThrow(Validaciones::noExiste);
        e.getZoneIds().clear();
        for (long zone : ids(data.get("zoneIds"))) { zonas.findByIdAndActiveTrue(zone).orElseThrow(Validaciones::noExiste); e.getZoneIds().add(zone); }
        e.setAvailable(flag(data, "available"));
        return Map.of("message", "Cobertura y disponibilidad actualizadas.");
    }

    // ---------- Incidencias ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> incidents() {
        return incidencias.recientes(PageRequest.of(0, 300)).stream().map(i -> Filas.of("ID", i.getId(), "Despacho", i.getDispatchId(), "Tipo", i.getKind(), "Descripción", i.getDescription(),
            "Resolución", i.getResolution(), "Estado", i.getResolvedAt() == null ? "OPEN" : "RESOLVED", "Fecha", i.getCreatedAt())).toList();
    }
    @Transactional public Map<String, Object> createIncident(Map<String, Object> data, String actor) {
        Incidencia i = new Incidencia(); i.setDispatchId(id(data, "dispatchId")); i.setKind(str(data, "kind")); i.setDescription(str(data, "description")); i.setCreatedBy(actor);
        return Map.of("id", incidencias.save(i).getId(), "message", "Incidencia registrada.");
    }
    @Transactional public Map<String, Object> resolveIncident(long incident, Map<String, Object> data) {
        Incidencia i = incidencias.bloquearAbierta(incident).orElseThrow(Validaciones::noExiste);
        i.setResolution(str(data, "resolution")); i.setResolvedAt(Instant.now());
        return Map.of("message", "Incidencia resuelta.");
    }
}
