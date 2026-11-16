package pe.inticrunch.reservas.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.caja.service.SaldoService;
import pe.inticrunch.operaciones.Filas;
import pe.inticrunch.operaciones.Validaciones;
import pe.inticrunch.pedidos.NumeroPedido;
import pe.inticrunch.pedidos.entity.DetallePedido;
import pe.inticrunch.pedidos.entity.OpcionDetallePedido;
import pe.inticrunch.pedidos.entity.PedidoCliente;
import pe.inticrunch.pedidos.repository.*;
import pe.inticrunch.reservas.entity.*;
import pe.inticrunch.reservas.repository.*;

@Service
public class ReservaService extends Validaciones {
    private static final long STAY_SECONDS = 7200;
    private final MesaRepository mesas;
    private final ReservaRepository reservas;
    private final VisitaRepository visitas;
    private final EventoReservaRepository eventos;
    private final PedidoClienteRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final OpcionDetallePedidoRepository opcionesDetalle;
    private final SaldoService saldos;
    private final NumeroPedido numero;
    public ReservaService(MesaRepository mesas, ReservaRepository reservas, VisitaRepository visitas, EventoReservaRepository eventos, PedidoClienteRepository pedidos,
                          DetallePedidoRepository detalles, OpcionDetallePedidoRepository opcionesDetalle, SaldoService saldos, NumeroPedido numero) {
        this.mesas = mesas; this.reservas = reservas; this.visitas = visitas; this.eventos = eventos; this.pedidos = pedidos; this.detalles = detalles;
        this.opcionesDetalle = opcionesDetalle; this.saldos = saldos; this.numero = numero;
    }

    /** Mesas libres para un horario: sin reservas que se crucen y sin clientes sentados que sigan ahí. */
    private Set<Long> mesasNoDisponibles(Instant start, Instant end, long ownReservation) {
        Set<Long> ocupadas = new HashSet<>(reservas.mesasReservadas(start, end, ownReservation));
        if (start.isBefore(Instant.now().plusSeconds(3 * 3600))) ocupadas.addAll(visitas.mesasOcupadas());
        return ocupadas;
    }
    private String codigo(long table) { return mesas.findById(table).map(Mesa::getTableCode).orElse("#" + table); }
    private String codigos(Collection<Long> tables) {
        Map<Long, String> code = mesas.findAll().stream().collect(Collectors.toMap(Mesa::getId, Mesa::getTableCode));
        return tables.stream().sorted().map(code::get).filter(Objects::nonNull).collect(Collectors.joining(", "));
    }

    // ---------- Mesas ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> diningTables() {
        Set<Long> ocupadas = new HashSet<>(visitas.mesasOcupadas());
        Instant ahora = Instant.now();
        Set<Long> reservadas = new HashSet<>(reservas.mesasConReservaPendiente(ahora, ahora.plusSeconds(STAY_SECONDS), -1L));
        return mesas.findByActiveTrueOrderById().stream().map(t -> Filas.of("ID", t.getId(), "Mesa", t.getTableCode(), "Capacidad", t.getCapacity(), "Zona", t.getArea() == null ? "Salón" : t.getArea(),
            "Estado", ocupadas.contains(t.getId()) ? "Ocupada" : reservadas.contains(t.getId()) ? "Reservada" : "Disponible")).toList();
    }
    @Transactional public Map<String, Object> createTable(Map<String, Object> data) {
        String code = tableCode(str(data, "tableCode"));
        // REGLA RESERVAS-01: Ya existe la mesa 
        if (mesas.existeActiva(code)) throw new IllegalArgumentException("Ya existe la mesa " + code + ".");
        var anterior = mesas.buscarPorCodigo(code).stream().findFirst();
        Mesa mesa = anterior.orElseGet(Mesa::new);
        if (mesa.getId() == null) { mesa.setLocationId(1L); mesa.setTableCode(code); }
        mesa.setCapacity(integer(data.get("capacity"), "Capacidad")); mesa.setArea(area(data)); mesa.setActive(true);
        mesa = mesas.save(mesa);
        return Map.of("id", mesa.getId(), "message", anterior.isPresent() ? "Mesa " + code + " reactivada." : "Mesa " + code + " creada.");
    }
    @Transactional public Map<String, Object> updateTable(long table, Map<String, Object> data) {
        Mesa mesa = mesas.bloquear(table).orElseThrow(Validaciones::noExiste);
        String code = tableCode(str(data, "tableCode"));
        // REGLA RESERVAS-02: Ya existe otra mesa con el código 
        if (mesas.otraConCodigo(code, table)) throw new IllegalArgumentException("Ya existe otra mesa con el código " + code + ".");
        mesa.setTableCode(code); mesa.setCapacity(integer(data.get("capacity"), "Capacidad")); mesa.setArea(area(data));
        return Map.of("message", "Mesa " + code + " actualizada.");
    }
    @Transactional public Map<String, Object> deleteTable(long table) {
        Mesa mesa = mesas.bloquear(table).orElseThrow(Validaciones::noExiste);
        if (!visitas.abiertasEnMesa(table).isEmpty() || reservas.mesaConReservaPendiente(table))
            // REGLA RESERVAS-03: La mesa tiene clientes o reservas pendientes. Libérala o mueve sus reservas antes de quitarla.
            throw new IllegalStateException("La mesa tiene clientes o reservas pendientes. Libérala o mueve sus reservas antes de quitarla.");
        if (visitas.mesaConHistorial(table) || reservas.mesaConHistorial(table)) {
            mesa.setActive(false);
            return Map.of("message", "Mesa retirada. Su historial se conserva.");
        }
        mesas.delete(mesa);
        return Map.of("message", "Mesa eliminada.");
    }
    /** "13" o "m13" se guardan como "M13" para que todas las mesas se lean igual. */
    public static String tableCode(String raw) {
        String code = raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (code.matches("[0-9]+")) code = "M" + Integer.parseInt(code);
        if (code.matches("MESA[0-9]+")) code = "M" + Integer.parseInt(code.substring(4));
        // REGLA RESERVAS-04: El código de la mesa es muy largo (máximo 20 caracteres).
        if (code.length() > 20) throw new IllegalArgumentException("El código de la mesa es muy largo (máximo 20 caracteres).");
        return code;
    }
    private static String area(Map<String, Object> data) { String value = optional(data, "area"); return value.isBlank() ? "Salón" : value; }

    /** Plano del salón: estado de cada mesa con sus clientes, próxima reserva y pedidos. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> floor() {
        var numeros = numero.todos();
        Instant ahora = Instant.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Mesa mesa : mesas.findByActiveTrueOrderById()) {
            long id = mesa.getId();
            Map<String, Object> item = Filas.of("id", id, "code", mesa.getTableCode(), "capacity", mesa.getCapacity(), "area", mesa.getArea() == null ? "Salón" : mesa.getArea());
            var abiertas = visitas.abiertasEnMesa(id);
            var proxima = reservas.proximas(id, ahora, PageRequest.of(0, 1));
            String state = "LIBRE";
            Set<Long> pedidosDeVisita = new HashSet<>(List.of(-1L));
            if (!abiertas.isEmpty()) {
                state = "OCUPADA";
                Visita v = abiertas.getFirst();
                pedidosDeVisita.addAll(v.getOrderIds());
                item.put("visit", Filas.of("id", v.getId(), "guest", v.getGuestName() == null ? "Clientes" : v.getGuestName(), "partySize", v.getPartySize(), "arrivedAt", v.getArrivedAt()));
            }
            if (!proxima.isEmpty()) {
                Reserva r = proxima.getFirst();
                item.put("reservation", Filas.of("id", r.getId(), "guest", r.getGuestName(), "partySize", r.getPartySize(), "startsAt", r.getStartsAt(), "endsAt", r.getEndsAt(), "notes", r.getNotes()));
                if (state.equals("LIBRE") && r.getStartsAt().isBefore(ahora.plusSeconds(STAY_SECONDS))) state = "RESERVADA";
            }
            item.put("state", state);
            List<Map<String, Object>> orders = new ArrayList<>();
            for (PedidoCliente o : pedidos.activosDeMesa(id, pedidosDeVisita)) {
                List<String> lines = new ArrayList<>();
                for (DetallePedido l : detalles.findByOrderIdOrderById(o.getId())) {
                    var opciones = opcionesDetalle.findByOrderLineIdOrderByOptionId(l.getId()).stream().map(OpcionDetallePedido::getOptionName).toList();
                    lines.add(l.getQuantity() + " × " + l.getProductName() + (opciones.isEmpty() ? "" : " (" + String.join(", ", opciones) + ")") + (l.getNotes() == null || l.getNotes().isBlank() ? "" : " · " + l.getNotes()));
                }
                orders.add(Filas.of("id", o.getId(), "number", numeros.get(o.getId()), "customer", o.getCustomerName(), "status", o.getStatus(), "total", o.getTotal(),
                    "createdAt", o.getCreatedAt(), "lines", lines, "balance", o.getTotal().subtract(saldos.paid(o.getId()))));
            }
            item.put("orders", orders);
            result.add(item);
        }
        return result;
    }

    // ---------- Disponibilidad ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> availability(Instant start, Instant end) {
        // REGLA RESERVAS-05: El fin debe ser posterior al inicio.
        if (!end.isAfter(start)) throw new IllegalArgumentException("El fin debe ser posterior al inicio.");
        Set<Long> ocupadas = mesasNoDisponibles(start, end, -1L);
        return mesas.findByActiveTrueOrderById().stream().filter(t -> !ocupadas.contains(t.getId()))
            .map(t -> Filas.of("ID", t.getId(), "Mesa", t.getTableCode(), "Capacidad", t.getCapacity())).toList();
    }
    /** Todas las mesas con su disponibilidad para el horario pedido (para dibujar la cuadrícula de selección). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> tableChoices(Instant start, Instant end, long ownReservation) {
        Set<Long> ocupadas = mesasNoDisponibles(start, end, ownReservation);
        return mesas.findByActiveTrueOrderById().stream().map(t -> Filas.of("id", t.getId(), "code", t.getTableCode(), "capacity", t.getCapacity(),
            "area", t.getArea() == null ? "Salón" : t.getArea(), "available", !ocupadas.contains(t.getId()))).toList();
    }
    /** Mesas para elegir al pedir "para comer en el local" (ahora mismo). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> seatingNow(String subject) {
        Instant ahora = Instant.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Mesa mesa : mesas.findByActiveTrueOrderById()) {
            long id = mesa.getId();
            var abiertas = visitas.abiertasEnMesa(id);
            boolean occupied = !abiertas.isEmpty();
            var reservadas = reservas.reservadasPronto(id, ahora, ahora.plusSeconds(STAY_SECONDS));
            boolean mine = subject != null && reservadas.stream().anyMatch(r -> subject.equals(r.getCustomerSubject()));
            boolean seatedHere = occupied && subject != null && pedidos.findAllById(abiertas.getFirst().getOrderIds()).stream().anyMatch(o -> subject.equals(o.getCustomerSubject()));
            String state = seatedHere ? "TU_MESA" : mine && !occupied ? "TU_RESERVA" : occupied ? "OCUPADA" : !reservadas.isEmpty() ? "RESERVADA" : "LIBRE";
            result.add(Filas.of("id", id, "code", mesa.getTableCode(), "capacity", mesa.getCapacity(), "area", mesa.getArea() == null ? "Salón" : mesa.getArea(),
                "state", state, "available", Set.of("LIBRE", "TU_RESERVA", "TU_MESA").contains(state)));
        }
        return result;
    }

    // ---------- Reservas ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> reservations() {
        return reservas.recientes(PageRequest.of(0, 300)).stream().map(r -> {
            String asignadas = codigos(r.getTableIds());
            return Filas.of("Reserva", r.getId(), "Cliente", r.getGuestName(), "Teléfono", r.getGuestPhone(), "Personas", r.getPartySize(), "Inicio", r.getStartsAt(), "Fin", r.getEndsAt(),
                "Estado", r.getStatus(), "Mesas", asignadas.isBlank() ? "Por asignar" : asignadas, "Notas", r.getNotes());
        }).toList();
    }
    @Transactional(readOnly = true)
    public Map<String, Object> reservationDetail(long reservation) {
        Reserva r = reservas.findById(reservation).orElseThrow(Validaciones::noExiste);
        return Filas.of("id", r.getId(), "guestName", r.getGuestName(), "guestPhone", r.getGuestPhone(), "partySize", r.getPartySize(), "startsAt", r.getStartsAt(), "endsAt", r.getEndsAt(),
            "notes", r.getNotes(), "status", r.getStatus(), "tableIds", r.getTableIds().stream().sorted().toList());
    }
    @Transactional public Map<String, Object> createReservation(Map<String, Object> data) {
        Instant start = instant(data.get("startsAt"));
        Instant end = optional(data, "endsAt").isBlank() ? start.plusSeconds(STAY_SECONDS) : instant(data.get("endsAt"));
        // REGLA RESERVAS-06: La hora de salida debe ser posterior a la hora de llegada.
        if (!end.isAfter(start)) throw new IllegalArgumentException("La hora de salida debe ser posterior a la hora de llegada.");
        int guests = integer(data.get("partySize"), "Personas");
        Object subject = data.get("_subject");
        Reserva r = new Reserva();
        r.setGuestName(str(data, "guestName")); r.setGuestPhone(str(data, "guestPhone")); r.setPartySize(guests); r.setStartsAt(start); r.setEndsAt(end); r.setNotes(optional(data, "notes"));
        r.setCustomerId(data.get("_customerId") instanceof Number number ? number.longValue() : null);
        r.setCustomerSubject(subject == null ? null : subject.toString());
        r = reservas.saveAndFlush(r);
        var tables = ids(data.get("tableIds"));
        if (!tables.isEmpty()) assign(r, tables, guests, start, end);
        event(r.getId(), "BOOKED", subject == null ? "Reserva creada por el local" : "Reserva hecha por el cliente desde la web");
        return Map.of("id", r.getId(), "message", tables.isEmpty() ? "Reserva creada; falta elegir las mesas." : "Reserva confirmada con sus mesas.");
    }
    @Transactional public Map<String, Object> updateReservation(long reservation, Map<String, Object> data) {
        Reserva r = reservas.bloquear(reservation).filter(x -> x.getStatus().equals("BOOKED")).orElseThrow(Validaciones::noExiste);
        Instant start = instant(data.get("startsAt"));
        Instant end = optional(data, "endsAt").isBlank() ? start.plusSeconds(STAY_SECONDS) : instant(data.get("endsAt"));
        // REGLA RESERVAS-07: La hora de salida debe ser posterior a la hora de llegada.
        if (!end.isAfter(start)) throw new IllegalArgumentException("La hora de salida debe ser posterior a la hora de llegada.");
        int guests = integer(data.get("partySize"), "Personas");
        r.setGuestName(str(data, "guestName")); r.setGuestPhone(str(data, "guestPhone")); r.setPartySize(guests); r.setStartsAt(start); r.setEndsAt(end); r.setNotes(optional(data, "notes"));
        var tables = ids(data.get("tableIds"));
        if (tables.isEmpty()) r.getTableIds().clear(); else assign(r, tables, guests, start, end);
        event(reservation, "BOOKED", "Reserva editada");
        return Map.of("message", "Reserva actualizada.");
    }
    @Transactional public Map<String, Object> deleteReservation(long reservation) {
        Reserva r = reservas.bloquear(reservation).orElseThrow(Validaciones::noExiste);
        // REGLA RESERVAS-08: Esta reserva ya tuvo clientes en el local; se conserva en el historial. Puedes finalizarla o cancelarla.
        if (r.getStatus().equals("ARRIVED") || visitas.existsByReservationId(reservation)) throw new IllegalStateException("Esta reserva ya tuvo clientes en el local; se conserva en el historial. Puedes finalizarla o cancelarla.");
        eventos.borrarDeReserva(reservation);
        reservas.delete(r);
        return Map.of("message", "Reserva eliminada.");
    }
    @Transactional public Map<String, Object> assignReservationTables(long reservation, Map<String, Object> data) {
        Reserva r = reservas.bloquear(reservation).filter(x -> x.getStatus().equals("BOOKED")).orElseThrow(Validaciones::noExiste);
        var tables = ids(data.get("tableIds"));
        // REGLA RESERVAS-09: Selecciona al menos una mesa.
        if (tables.isEmpty()) throw new IllegalArgumentException("Selecciona al menos una mesa.");
        assign(r, tables, r.getPartySize(), r.getStartsAt(), r.getEndsAt());
        event(reservation, "BOOKED", "Mesas actualizadas");
        return Map.of("message", "Mesas asignadas.");
    }
    /** Bloquea las mesas y comprueba que entren todas las personas. */
    private void bloquearMesas(List<Long> tables, int guests) {
        int capacity = 0;
        for (long table : tables) capacity += mesas.bloquear(table).filter(m -> Boolean.TRUE.equals(m.getActive())).orElseThrow(Validaciones::noExiste).getCapacity();
        // REGLA RESERVAS-10: Las mesas elegidas tienen 
        if (capacity < guests) throw new IllegalArgumentException("Las mesas elegidas tienen " + capacity + " lugares y el grupo es de " + guests + " personas. Elige otra mesa más.");
    }
    private void assign(Reserva reserva, List<Long> tables, int guests, Instant start, Instant end) {
        bloquearMesas(tables, guests);
        Set<Long> ocupadas = mesasNoDisponibles(start, end, reserva.getId());
        for (long table : tables)
            // REGLA RESERVAS-11: La mesa 
            if (ocupadas.contains(table)) throw new IllegalStateException("La mesa " + codigo(table) + " ya está reservada u ocupada en ese horario. Elige otra.");
        reserva.getTableIds().clear();
        reserva.getTableIds().addAll(tables);
    }
    @Transactional public Map<String, Object> reservationStatus(long reservation, Map<String, Object> data) {
        Reserva r = reservas.bloquear(reservation).orElseThrow(Validaciones::noExiste);
        String next = choice(data, "status", "ARRIVED", "CANCELLED", "NO_SHOW", "COMPLETED"), previous = r.getStatus();
        if (!(previous.equals("BOOKED") && Set.of("ARRIVED", "CANCELLED", "NO_SHOW").contains(next) || previous.equals("ARRIVED") && next.equals("COMPLETED")))
            // REGLA RESERVAS-12: Ese cambio no corresponde al estado actual de la reserva.
            throw new IllegalStateException("Ese cambio no corresponde al estado actual de la reserva.");
        // REGLA RESERVAS-13: La reserva aún no empieza; todavía no se puede marcar como 'no vino'.
        if (next.equals("NO_SHOW") && r.getStartsAt().isAfter(Instant.now())) throw new IllegalStateException("La reserva aún no empieza; todavía no se puede marcar como 'no vino'.");
        if (next.equals("ARRIVED")) {
            List<Long> tables = r.getTableIds().stream().sorted().toList();
            // REGLA RESERVAS-14: Elige las mesas antes de registrar la llegada.
            if (tables.isEmpty()) throw new IllegalStateException("Elige las mesas antes de registrar la llegada.");
            bloquearMesas(tables, r.getPartySize());
            // REGLA RESERVAS-15: Una de las mesas todavía tiene clientes. Libérala primero.
            for (long table : tables) if (!visitas.abiertasEnMesa(table).isEmpty()) throw new IllegalStateException("Una de las mesas todavía tiene clientes. Libérala primero.");
            Visita v = new Visita(); v.setReservationId(reservation); v.setGuestName(r.getGuestName()); v.setPartySize(r.getPartySize()); v.getTableIds().addAll(tables);
            visitas.saveAndFlush(v);
        }
        r.setStatus(next);
        if (next.equals("COMPLETED")) visitas.findByReservationIdAndLeftAtIsNull(reservation).stream().findFirst().ifPresent(v -> finishVisit(v.getId()));
        event(reservation, next, optional(data, "note"));
        return Map.of("message", switch (next) { case "ARRIVED" -> "Llegada registrada; mesas ocupadas."; case "COMPLETED" -> "Los clientes se fueron; mesas libres."; case "CANCELLED" -> "Reserva cancelada; mesas libres."; default -> "Reserva marcada como 'no vino'; mesas libres."; });
    }

    // ---------- Reservas del cliente (web) ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> customerReservations(String subject) {
        return reservas.deLaCuenta(subject).stream().map(r -> Filas.of("id", r.getId(), "partySize", r.getPartySize(), "startsAt", r.getStartsAt(), "endsAt", r.getEndsAt(),
            "status", r.getStatus(), "notes", r.getNotes(), "tables", codigos(r.getTableIds()))).toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> customerReservations(String subject, Long customer) {
        return reservas.delCliente(subject, customer == null ? -1L : customer).stream().map(r -> Filas.of("id", r.getId(), "partySize", r.getPartySize(), "startsAt", r.getStartsAt(), "endsAt", r.getEndsAt(),
            "status", r.getStatus(), "notes", r.getNotes(), "tables", codigos(r.getTableIds()))).toList();
    }
    @Transactional public Map<String, Object> cancelCustomerReservation(long reservation, String subject) {
        Reserva r = reservas.bloquear(reservation).filter(x -> subject.equals(x.getCustomerSubject()) && x.getStatus().equals("BOOKED")).orElseThrow(Validaciones::noExiste);
        r.setStatus("CANCELLED");
        event(reservation, "CANCELLED", "Cancelada por el cliente");
        return Map.of("message", "Tu reserva fue cancelada.");
    }

    // ---------- Visitas y pedidos en salón ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> visits() {
        return visitas.recientes(PageRequest.of(0, 300)).stream().map(v -> Filas.of("ID", v.getId(), "Cliente", v.getGuestName(), "Personas", v.getPartySize(), "Reserva", v.getReservationId(),
            "Mesas", codigos(v.getTableIds()), "Llegada", v.getArrivedAt(), "Salida", v.getLeftAt(), "Estado", v.getLeftAt() == null ? "OPEN" : "CLOSED")).toList();
    }
    @Transactional public Map<String, Object> walkIn(Map<String, Object> data) {
        int guests = integer(data.get("partySize"), "Personas");
        var tables = ids(data.get("tableIds"));
        // REGLA RESERVAS-16: Selecciona las mesas para los clientes.
        if (tables.isEmpty()) throw new IllegalArgumentException("Selecciona las mesas para los clientes.");
        bloquearMesas(tables, guests);
        Instant now = Instant.now(), end = now.plusSeconds(STAY_SECONDS);
        Set<Long> ocupadas = new HashSet<>(visitas.mesasOcupadas());
        ocupadas.addAll(reservas.mesasConReservaPendiente(now, end, -1L));
        // REGLA RESERVAS-17: La mesa 
        for (long table : tables) if (ocupadas.contains(table)) throw new IllegalStateException("La mesa " + codigo(table) + " está ocupada o reservada para pronto. Elige otra.");
        Visita v = new Visita(); v.setGuestName(str(data, "guestName")); v.setPartySize(guests); v.getTableIds().addAll(tables);
        v = visitas.save(v);
        return Map.of("id", v.getId(), "message", "Clientes sentados; mesas ocupadas.");
    }
    /** Ubica un pedido "para comer en el local" en su mesa y lo vincula con la visita. */
    @Transactional public void seatOrder(long order, long table, String guest, long customer, String subject, int party) {
        Mesa mesa = mesas.bloquear(table).filter(m -> Boolean.TRUE.equals(m.getActive())).orElseThrow(Validaciones::noExiste);
        var abiertas = visitas.abiertasEnMesa(table);
        Visita visita;
        if (!abiertas.isEmpty()) visita = abiertas.getFirst();
        else {
            Instant ahora = Instant.now();
            var reservadas = reservas.reservadasPronto(table, ahora, ahora.plusSeconds(STAY_SECONDS));
            if (!reservadas.isEmpty()) {
                Reserva r = reservadas.getFirst();
                boolean own = subject != null && subject.equals(r.getCustomerSubject()) || r.getCustomerId() != null && r.getCustomerId() == customer;
                // REGLA RESERVAS-18: La mesa 
                if (!own) throw new IllegalStateException("La mesa " + mesa.getTableCode() + " está reservada para otra persona. Elige otra mesa libre.");
                reservationStatus(r.getId(), Map.of("status", "ARRIVED", "note", "Llegó y pidió desde la web"));
                visita = visitas.findByReservationIdAndLeftAtIsNull(r.getId()).getFirst();
            } else {
                visita = new Visita(); visita.setGuestName(guest); visita.setPartySize(Math.max(1, party > 0 ? party : 1)); visita.getTableIds().add(table);
                visita = visitas.saveAndFlush(visita);
            }
        }
        visita.getOrderIds().add(order);
        pedidos.findById(order).orElseThrow(Validaciones::noExiste).setTableId(table);
    }
    @Transactional public Map<String, Object> attachOrder(long visit, Map<String, Object> data) {
        Visita visita = visitas.bloquearAbierta(visit).orElseThrow(Validaciones::noExiste);
        long order = id(data, "orderId");
        PedidoCliente pedido = pedidos.bloquear(order).filter(o -> o.getChannel().equals("DINE_IN") && !o.getStatus().equals("CANCELLED")).orElseThrow(Validaciones::noExiste);
        if (visita.getOrderIds().contains(order)) return Map.of("message", "El pedido ya estaba en esta mesa.");
        visita.getOrderIds().add(order);
        if (pedido.getTableId() == null) visita.getTableIds().stream().sorted().findFirst().ifPresent(pedido::setTableId);
        return Map.of("message", "Pedido ubicado en la mesa.");
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> visitOrders(long visit) {
        var numeros = numero.todos();
        Visita v = visitas.findById(visit).orElseThrow(Validaciones::noExiste);
        return pedidos.findAllById(v.getOrderIds()).stream().sorted(Comparator.comparing(PedidoCliente::getId))
            .map(o -> Filas.of("Pedido", numeros.get(o.getId()), "Cliente", o.getCustomerName(), "Estado", o.getStatus(), "Total", o.getTotal())).toList();
    }
    @Transactional public Map<String, Object> finishVisit(long visit) {
        Visita v = visitas.bloquearAbierta(visit).orElseThrow(Validaciones::noExiste);
        if (pedidos.findAllById(v.getOrderIds()).stream().anyMatch(o -> !Set.of("COMPLETED", "CANCELLED").contains(o.getStatus())))
            // REGLA RESERVAS-19: Esta mesa aún tiene pedidos sin entregar o sin cobrar. Entrégalos o cancélalos antes de liberarla.
            throw new IllegalStateException("Esta mesa aún tiene pedidos sin entregar o sin cobrar. Entrégalos o cancélalos antes de liberarla.");
        v.setLeftAt(Instant.now());
        if (v.getReservationId() != null) {
            reservas.findById(v.getReservationId()).filter(r -> r.getStatus().equals("ARRIVED")).ifPresent(r -> r.setStatus("COMPLETED"));
            event(v.getReservationId(), "COMPLETED", "Clientes se fueron; mesas libres");
        }
        return Map.of("message", "Mesa liberada; ya está disponible.");
    }
    @Transactional public Map<String, Object> freeTable(long table) {
        var abiertas = visitas.abiertasEnMesa(table);
        if (abiertas.isEmpty()) return Map.of("message", "La mesa ya estaba libre.");
        return finishVisit(abiertas.getFirst().getId());
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(long reservation) {
        return eventos.findByReservationIdOrderById(reservation).stream().map(e -> Filas.of("Estado", e.getStatus(), "Nota", e.getNote(), "Fecha", e.getCreatedAt())).toList();
    }
    private void event(long reservation, String status, String note) {
        EventoReserva e = new EventoReserva(); e.setReservationId(reservation); e.setStatus(status); e.setNote(note);
        eventos.save(e);
    }
}
