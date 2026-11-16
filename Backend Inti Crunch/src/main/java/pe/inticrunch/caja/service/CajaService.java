package pe.inticrunch.caja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.caja.entity.*;
import pe.inticrunch.caja.repository.*;
import pe.inticrunch.configuracion.ConfiguracionEmpresa;
import pe.inticrunch.clientes.entity.MovimientoPuntos;
import pe.inticrunch.clientes.repository.MovimientoPuntosRepository;
import pe.inticrunch.catalogo.ProductoRepository;
import pe.inticrunch.operaciones.Filas;
import pe.inticrunch.operaciones.Validaciones;
import pe.inticrunch.operaciones.repository.LocalRepository;
import pe.inticrunch.operaciones.repository.SecuenciaRepository;
import pe.inticrunch.pedidos.NumeroPedido;
import pe.inticrunch.pedidos.entity.DetallePedido;
import pe.inticrunch.pedidos.entity.PedidoCliente;
import pe.inticrunch.pedidos.repository.DetallePedidoRepository;
import pe.inticrunch.pedidos.repository.OpcionDetallePedidoRepository;
import pe.inticrunch.pedidos.repository.PedidoClienteRepository;
import pe.inticrunch.personal.entity.Colaborador;
import pe.inticrunch.personal.entity.Turno;
import pe.inticrunch.personal.repository.AsignacionTurnoRepository;
import pe.inticrunch.personal.repository.ColaboradorRepository;
import pe.inticrunch.personal.repository.TurnoRepository;

@Service
public class CajaService extends Validaciones {
    private final SaldoService balances;
    private final ComprobantePdfService pdf;
    private final ConfiguracionEmpresa company;
    private final SesionCajaRepository sesiones;
    private final MovimientoCajaRepository movimientos;
    private final PagoRepository pagos;
    private final MedioPagoRepository medios;
    private final AsignacionPagoRepository asignaciones;
    private final DevolucionRepository devoluciones;
    private final ComprobanteRepository comprobantes;
    private final LineaComprobanteRepository lineasComprobante;
    private final ArchivoComprobanteRepository archivos;
    private final PedidoClienteRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final OpcionDetallePedidoRepository opcionesDetalle;
    private final ProductoRepository productos;
    private final MovimientoPuntosRepository puntos;
    private final ColaboradorRepository colaboradores;
    private final TurnoRepository turnos;
    private final AsignacionTurnoRepository asignacionesTurno;
    private final LocalRepository locales;
    private final SecuenciaRepository secuencias;
    private final NumeroPedido numero;

    public CajaService(SaldoService balances, ComprobantePdfService pdf, ConfiguracionEmpresa company, SesionCajaRepository sesiones, MovimientoCajaRepository movimientos,
                       PagoRepository pagos, MedioPagoRepository medios, AsignacionPagoRepository asignaciones, DevolucionRepository devoluciones,
                       ComprobanteRepository comprobantes, LineaComprobanteRepository lineasComprobante, ArchivoComprobanteRepository archivos,
                       PedidoClienteRepository pedidos, DetallePedidoRepository detalles, OpcionDetallePedidoRepository opcionesDetalle, ProductoRepository productos,
                       MovimientoPuntosRepository puntos, ColaboradorRepository colaboradores, TurnoRepository turnos, AsignacionTurnoRepository asignacionesTurno,
                       LocalRepository locales, SecuenciaRepository secuencias, NumeroPedido numero) {
        this.balances = balances; this.pdf = pdf; this.company = company; this.sesiones = sesiones; this.movimientos = movimientos; this.pagos = pagos; this.medios = medios;
        this.asignaciones = asignaciones; this.devoluciones = devoluciones; this.comprobantes = comprobantes; this.lineasComprobante = lineasComprobante; this.archivos = archivos;
        this.pedidos = pedidos; this.detalles = detalles; this.opcionesDetalle = opcionesDetalle; this.productos = productos; this.puntos = puntos;
        this.colaboradores = colaboradores; this.turnos = turnos; this.asignacionesTurno = asignacionesTurno; this.locales = locales; this.secuencias = secuencias; this.numero = numero;
    }

    // ---------- Apertura, movimientos y cierre ----------
    @Transactional public Map<String, Object> openCash(Map<String, Object> data, String actor) {
        Long employee = optionalId(data, "employeeId"), shift = optionalId(data, "shiftId");
        if (employee != null) colaboradores.findById(employee).filter(e -> Boolean.TRUE.equals(e.getActive())).orElseThrow(Validaciones::noExiste);
        if (shift != null) {
            // REGLA CAJA-01: Selecciona al colaborador responsable del turno.
            if (employee == null) throw new IllegalArgumentException("Selecciona al colaborador responsable del turno.");
            if (!asignacionesTurno.puedeCobrarEnTurno(shift, employee)) throw noExiste();
        }
        SesionCaja s = new SesionCaja();
        s.setLocationId(1L); s.setOpenedBy(actor); s.setOpeningAmount(money(data.get("openingAmount"), "Fondo inicial")); s.setEmployeeId(employee); s.setShiftId(shift);
        return Map.of("id", sesiones.save(s).getId(), "message", "Caja abierta.");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> cashSessions() {
        Map<Long, String> personas = colaboradores.findAll().stream().collect(Collectors.toMap(Colaborador::getId, Colaborador::getFullName));
        Map<Long, String> nombresTurno = turnos.findAll().stream().collect(Collectors.toMap(Turno::getId, Turno::getName));
        return sesiones.recientes(PageRequest.of(0, 100)).stream().map(s -> Filas.of("Caja", s.getId(), "Responsable", s.getOpenedBy(), "Apertura", s.getOpenedAt(), "Estado", s.getStatus(),
            "Colaborador", s.getEmployeeId() == null ? null : personas.get(s.getEmployeeId()), "Turno", s.getShiftId() == null ? null : nombresTurno.get(s.getShiftId()),
            "Fondo inicial", s.getOpeningAmount(), "Contado", s.getClosingAmount(), "Diferencia", s.getDifference(), "Esperado", expected(s))).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> payments() {
        var numeros = numero.todos();
        var lista = pagos.recientes(PageRequest.of(0, 300));
        Map<Long, BigDecimal> devuelto = new HashMap<>();
        for (Object[] row : devoluciones.devueltoPorPago()) devuelto.put((Long) row[0], (BigDecimal) row[1]);
        Map<Long, List<AsignacionPago>> porPago = lista.isEmpty() ? Map.of()
            : asignaciones.findByPaymentIdIn(lista.stream().map(Pago::getId).toList()).stream().collect(Collectors.groupingBy(AsignacionPago::getPaymentId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Pago p : lista) for (AsignacionPago a : porPago.getOrDefault(p.getId(), List.of()))
            rows.add(Filas.of("Pago", p.getId(), "Pedido", numeros.get(a.getOrderId()), "Medio", p.getMethod(), "Monto", p.getAmount(), "Propina", p.getTip(),
                "Devuelto", devuelto.getOrDefault(p.getId(), BigDecimal.ZERO), "Responsable", p.getCreatedBy(), "Fecha", p.getCreatedAt()));
        return rows;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> movements() {
        return movimientos.recientes(PageRequest.of(0, 300)).stream().map(m -> Filas.of("ID", m.getId(), "Caja", m.getCashSessionId(), "Tipo", m.getKind(), "Monto", m.getAmount(),
            "Motivo", m.getReason(), "Responsable", m.getCreatedBy(), "Fecha", m.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> refunds() {
        var numeros = numero.todos();
        return devoluciones.recientes(PageRequest.of(0, 300)).stream().map(r -> Filas.of("ID", r.getId(), "Pago", r.getPaymentId(), "Pedido", numeros.get(r.getOrderId()),
            "Medio", r.getMethod(), "Monto", r.getAmount(), "Motivo", r.getReason(), "Fecha", r.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> documents() {
        var numeros = numero.todos();
        return comprobantes.recientes(PageRequest.of(0, 300)).stream().map(d -> Filas.of("ID", d.getId(), "Tipo", d.getDocumentType(), "Número", numeroComprobante(d),
            "Pedido", numeros.get(d.getOrderId()), "Cliente", d.getCustomerName(), "Monto pagado", d.getTotal(), "IGV", d.getTaxAmount(), "Fecha", d.getIssuedAt())).toList();
    }
    private static String numeroComprobante(Comprobante d) { return d.getSeries() + "-" + String.format("%08d", d.getNumber()); }

    @Transactional public Map<String, Object> movement(Map<String, Object> data, String actor) {
        long session = id(data, "cashSessionId");
        SesionCaja caja = sesiones.bloquearAbierta(session).orElseThrow(Validaciones::noExiste);
        String kind = choice(data, "kind", "IN", "OUT");
        BigDecimal amount = money(data.get("amount"), "Monto");
        // REGLA CAJA-02: El monto debe ser positivo.
        if (amount.signum() <= 0) throw new IllegalArgumentException("El monto debe ser positivo.");
        // REGLA CAJA-03: El retiro supera el efectivo esperado.
        if (kind.equals("OUT") && expected(caja).compareTo(amount) < 0) throw new IllegalStateException("El retiro supera el efectivo esperado.");
        MovimientoCaja m = new MovimientoCaja();
        m.setCashSessionId(session); m.setKind(kind); m.setAmount(amount); m.setReason(str(data, "reason")); m.setCreatedBy(actor);
        return Map.of("id", movimientos.save(m).getId(), "message", "Movimiento de caja registrado.");
    }

    /** Efectivo que debería haber en la caja: fondo + cobros en efectivo − devoluciones en efectivo ± movimientos. */
    public BigDecimal expected(long session) { return expected(sesiones.findById(session).orElseThrow(Validaciones::noExiste)); }
    private BigDecimal expected(SesionCaja s) {
        return s.getOpeningAmount().add(medios.efectivoRecibido(s.getId())).subtract(devoluciones.efectivoDevuelto(s.getId())).add(movimientos.netoDeSesion(s.getId()));
    }

    @Transactional public Map<String, Object> closeCash(long session, Map<String, Object> data, String actor) {
        SesionCaja caja = sesiones.bloquearAbierta(session).orElseThrow(Validaciones::noExiste);
        BigDecimal counted = money(data.get("closingAmount"), "Efectivo contado"), expected = expected(caja), difference = counted.subtract(expected);
        caja.setStatus("CLOSED"); caja.setClosedBy(actor); caja.setClosedAt(java.time.Instant.now()); caja.setClosingAmount(counted); caja.setExpectedAmount(expected); caja.setDifference(difference);
        return Map.of("expected", expected, "counted", counted, "difference", difference, "message", "Caja cerrada. Diferencia: S/ " + difference);
    }

    // ---------- Cobros ----------
    @Transactional public Map<String, Object> collectPublic(long order, Map<String, Object> data) {
        Map<String, Object> request = new LinkedHashMap<>(data);
        var abiertas = sesiones.abiertasDelLocal();
        // REGLA CAJA-04: La caja está cerrada. El personal debe abrir una caja antes de simular el cobro.
        if (abiertas.isEmpty()) throw new IllegalStateException("La caja está cerrada. El personal debe abrir una caja antes de simular el cobro.");
        request.put("cashSessionId", abiertas.getFirst().getId()); request.put("orderId", order);
        request.putIfAbsent("amount", balances.due(order));
        return collectPayment(request, "CHECKOUT SIMULADO");
    }

    @Transactional public Map<String, Object> collectPayment(Map<String, Object> data, String actor) {
        // REGLA CAJA-05: Pago simulado rechazado. No se realizó ningún cobro.
        if ("DECLINED".equals(data.get("simulationResult"))) throw new IllegalStateException("Pago simulado rechazado. No se realizó ningún cobro.");
        long order;
        if (data.containsKey("orderId")) order = id(data, "orderId");
        else {
            var allocations = lines(data, "allocations");
            // REGLA CAJA-06: Registra un pedido por operación de cobro.
            if (allocations.size() != 1) throw new IllegalArgumentException("Registra un pedido por operación de cobro.");
            order = id(allocations.getFirst(), "orderId");
        }
        UUID key = optional(data, "requestKey").isBlank() ? UUID.randomUUID() : UUID.fromString(str(data, "requestKey"));
        locales.bloquear(1L); // evita cobros duplicados por doble clic
        var existing = pagos.findByRequestKey(key);
        if (existing.isPresent()) {
            long payment = existing.get().getId();
            var alloc = asignaciones.findByPaymentId(payment);
            // REGLA CAJA-07: La clave de pago pertenece a otro pedido.
            if (!alloc.isEmpty() && alloc.getFirst().getOrderId() != order) throw new IllegalArgumentException("La clave de pago pertenece a otro pedido.");
            return Map.of("id", payment, "message", "El pago ya estaba registrado.", "documents", paymentDocuments(payment));
        }
        long session = id(data, "cashSessionId");
        sesiones.bloquearAbierta(session).orElseThrow(Validaciones::noExiste);
        PedidoCliente purchase = pedidos.bloquear(order).orElseThrow(Validaciones::noExiste);
        // REGLA CAJA-08: No se puede cobrar un pedido cancelado.
        if (purchase.getStatus().equals("CANCELLED")) throw new IllegalStateException("No se puede cobrar un pedido cancelado.");

        List<Map<String, Object>> methods;
        if (data.get("methods") instanceof List<?>) methods = lines(data, "methods");
        else {
            Object amount = data.get("amount");
            if (amount == null && data.containsKey("allocations")) amount = lines(data, "allocations").getFirst().get("amount");
            Map<String, Object> single = new LinkedHashMap<>();
            single.put("method", str(data, "method")); single.put("amount", amount); single.put("tip", data.getOrDefault("tip", 0)); single.put("reference", optional(data, "reference"));
            methods = List.of(single);
        }
        BigDecimal amount = BigDecimal.ZERO, tip = BigDecimal.ZERO;
        for (var method : methods) {
            choice(method, "method", "CASH", "CARD", "YAPE", "PLIN", "TRANSFER");
            BigDecimal part = money(method.get("amount"), "Monto");
            // REGLA CAJA-09: Cada medio debe aportar un monto positivo.
            if (part.signum() <= 0) throw new IllegalArgumentException("Cada medio debe aportar un monto positivo.");
            amount = amount.add(part); tip = tip.add(money(method.getOrDefault("tip", 0), "Propina"));
        }
        BigDecimal due = balances.due(order);
        // REGLA CAJA-10: El pago supera el saldo pendiente de S/ 
        if (amount.compareTo(due) > 0) throw new IllegalStateException("El pago supera el saldo pendiente de S/ " + due + ".");
        String type = String.valueOf(data.getOrDefault("documentType", "BOLETA")).toUpperCase(Locale.ROOT);
        // REGLA CAJA-11: Selecciona boleta o factura.
        if (!Set.of("BOLETA", "FACTURA").contains(type)) throw new IllegalArgumentException("Selecciona boleta o factura.");
        String document = optional(data, "customerDocumentNumber"), customerAddress = optional(data, "customerAddress"), customerName = optional(data, "customerBusinessName");
        // REGLA CAJA-12: La factura requiere RUC de 11 dígitos, razón social y dirección fiscal.
        if (type.equals("FACTURA") && (!document.matches("[0-9]{11}") || customerAddress.isBlank() || customerName.isBlank())) throw new IllegalArgumentException("La factura requiere RUC de 11 dígitos, razón social y dirección fiscal.");
        // REGLA CAJA-13: El DNI debe tener 8 dígitos.
        if (type.equals("BOLETA") && !document.isBlank() && !document.matches("[0-9]{8}")) throw new IllegalArgumentException("El DNI debe tener 8 dígitos.");

        Pago pago = new Pago();
        pago.setCashSessionId(session); pago.setMethod(methods.size() == 1 ? str(methods.getFirst(), "method").toUpperCase(Locale.ROOT) : "MIXED");
        pago.setAmount(amount); pago.setTip(tip); pago.setReference(optional(data, "reference")); pago.setCreatedBy(actor); pago.setRequestKey(key);
        long payment = pagos.save(pago).getId();
        StringJoiner summary = new StringJoiner(" + ");
        for (var method : methods) {
            String name = str(method, "method").toUpperCase(Locale.ROOT);
            BigDecimal part = money(method.get("amount"), "Monto"), gratuity = money(method.getOrDefault("tip", 0), "Propina");
            MedioPago medio = new MedioPago();
            medio.setPaymentId(payment); medio.setMethod(name); medio.setAmount(part); medio.setTip(gratuity); medio.setReference(optional(method, "reference"));
            medios.save(medio);
            summary.add(paymentLabel(name) + " S/ " + part + (gratuity.signum() > 0 ? " (propina S/ " + gratuity + ")" : ""));
        }
        AsignacionPago asignacion = new AsignacionPago();
        asignacion.setPaymentId(payment); asignacion.setOrderId(order); asignacion.setAmount(amount);
        asignaciones.save(asignacion);

        // Comprobante (boleta o factura) con IGV incluido del 18 %
        BigDecimal base = amount.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        Comprobante c = new Comprobante();
        c.setPaymentId(payment); c.setOrderId(order); c.setDocumentType(type); c.setSeries(type.equals("FACTURA") ? "F001" : "B001");
        c.setNumber(secuencias.siguiente("payment_document_number_seq"));
        c.setCustomerDocumentType(type.equals("FACTURA") ? "RUC" : "DNI"); c.setCustomerDocumentNumber(document.isBlank() ? null : document);
        c.setCustomerAddress(customerAddress.isBlank() ? purchase.getDeliveryAddress() : customerAddress);
        c.setCustomerName(customerName.isBlank() ? purchase.getCustomerName() : customerName); c.setCustomerPhone(purchase.getCustomerPhone());
        c.setSellerName(company.name); c.setSellerTaxId(company.ruc()); c.setSellerAddress(company.address); c.setSellerPhone(company.phone); c.setCashier(actor);
        c.setSubtotal(base); c.setTaxRate(new BigDecimal("18")); c.setTaxAmount(amount.subtract(base)); c.setTotal(amount); c.setOrderTotal(purchase.getTotal());
        c.setPendingAmount(due.subtract(amount)); c.setDiscount(purchase.getDiscount()); c.setDeliveryFee(purchase.getDeliveryFee()); c.setPaymentSummary(summary.toString());
        long receipt = comprobantes.save(c).getId();
        for (DetallePedido ol : detalles.findByOrderIdOrderById(order)) {
            String nombre = ol.getProductName() != null ? ol.getProductName() : productos.findById(ol.getProductId()).map(p -> p.getName()).orElse("Producto");
            var extras = opcionesDetalle.findByOrderLineIdOrderByOptionId(ol.getId());
            if (!extras.isEmpty()) nombre += " [" + extras.stream().map(o -> o.getOptionName()).collect(Collectors.joining(", ")) + "]";
            if (ol.getNotes() != null && !ol.getNotes().isBlank()) nombre += " - " + ol.getNotes();
            LineaComprobante linea = new LineaComprobante();
            linea.setDocumentId(receipt); linea.setProductName(nombre); linea.setQuantity(ol.getQuantity()); linea.setUnitPrice(ol.getUnitPrice());
            linea.setLineTotal(ol.getUnitPrice().multiply(BigDecimal.valueOf(ol.getQuantity())));
            lineasComprobante.save(linea);
        }
        comprobantes.flush();
        ArchivoComprobante archivo = new ArchivoComprobante();
        archivo.setDocumentId(receipt); archivo.setContent(renderizar(c));
        archivos.save(archivo);
        return Map.of("id", payment, "message", "Pago simulado confirmado y PDF generado.", "documents", paymentDocuments(payment));
    }

    /** Arma los datos que usa el PDF (mismas claves que las columnas de la tabla). */
    private byte[] renderizar(Comprobante d) {
        Map<String, Object> receipt = new HashMap<>();
        receipt.put("id", d.getId()); receipt.put("order_id", d.getOrderId()); receipt.put("order_number", numero.de(d.getOrderId()));
        receipt.put("document_type", d.getDocumentType()); receipt.put("series", d.getSeries()); receipt.put("number", d.getNumber());
        receipt.put("customer_document_type", d.getCustomerDocumentType()); receipt.put("customer_document_number", d.getCustomerDocumentNumber());
        receipt.put("customer_address", d.getCustomerAddress()); receipt.put("customer_name", d.getCustomerName()); receipt.put("customer_phone", d.getCustomerPhone());
        receipt.put("seller_name", d.getSellerName()); receipt.put("seller_tax_id", d.getSellerTaxId()); receipt.put("seller_address", d.getSellerAddress()); receipt.put("seller_phone", d.getSellerPhone());
        receipt.put("cashier", d.getCashier()); receipt.put("subtotal", d.getSubtotal()); receipt.put("tax_amount", d.getTaxAmount()); receipt.put("total", d.getTotal());
        receipt.put("order_total", d.getOrderTotal()); receipt.put("pending_amount", d.getPendingAmount()); receipt.put("discount", d.getDiscount()); receipt.put("delivery_fee", d.getDeliveryFee());
        receipt.put("payment_summary", d.getPaymentSummary()); receipt.put("issued_at", d.getIssuedAt() == null ? null : Timestamp.from(d.getIssuedAt()));
        List<Map<String, Object>> lines = new ArrayList<>();
        for (var l : lineasComprobante.findByDocumentIdOrderById(d.getId())) {
            Map<String, Object> line = new HashMap<>();
            line.put("product_name", l.getProductName()); line.put("quantity", l.getQuantity()); line.put("unit_price", l.getUnitPrice()); line.put("line_total", l.getLineTotal());
            lines.add(line);
        }
        return pdf.render(receipt, lines);
    }

    private static String paymentLabel(String method) {
        return switch (method) { case "CASH" -> "Efectivo"; case "CARD" -> "Tarjeta"; case "YAPE" -> "Yape"; case "PLIN" -> "Plin"; case "TRANSFER" -> "Transferencia"; default -> method; };
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> paymentDocuments(long payment) {
        return comprobantes.findByPaymentIdOrderById(payment).stream().map(d -> Filas.of("id", d.getId(), "tipo", d.getDocumentType(), "serie", d.getSeries(), "numero", d.getNumber(), "total", d.getTotal(), "pedido", d.getOrderId())).toList();
    }
    @Transactional(readOnly = true)
    public byte[] document(long receipt) {
        var file = archivos.findById(receipt);
        if (file.isPresent()) return file.get().getContent();
        return renderizar(comprobantes.findById(receipt).orElseThrow(Validaciones::noExiste));
    }
    @Transactional(readOnly = true)
    public byte[] publicDocument(long receipt, long order) {
        if (!comprobantes.existsByIdAndOrderId(receipt, order)) throw noExiste();
        return document(receipt);
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> orderDocuments(long order) {
        return comprobantes.findByOrderIdOrderById(order).stream().map(d -> Filas.of("id", d.getId(), "tipo", d.getDocumentType(), "serie", d.getSeries(), "numero", d.getNumber(), "total", d.getTotal())).toList();
    }

    // ---------- Devoluciones ----------
    /** Lo que todavía se puede devolver de un pago, separado por medio de pago. */
    @Transactional(readOnly = true)
    public Map<String, Object> refundable(long payment) {
        List<Map<String, Object>> available = new ArrayList<>(); BigDecimal total = BigDecimal.ZERO;
        for (Object[] row : medios.pagadoPorMedio(payment)) {
            String method = (String) row[0];
            BigDecimal left = ((BigDecimal) row[1]).subtract(devoluciones.devueltoPorMedio(payment, method));
            if (left.signum() > 0) { available.add(Map.of("method", method, "label", paymentLabel(method), "available", left)); total = total.add(left); }
        }
        var abiertas = sesiones.abiertas();
        return Map.of("methods", available, "total", total, "cashSessionId", abiertas.isEmpty() ? "" : abiertas.getFirst().getId());
    }

    @Transactional public Map<String, Object> refund(long payment, Map<String, Object> data, String actor) {
        var alloc = asignaciones.findByPaymentId(payment);
        if (alloc.isEmpty()) throw noExiste();
        long order = alloc.getFirst().getOrderId();
        PedidoCliente purchase = pedidos.bloquear(order).orElseThrow(Validaciones::noExiste);
        Pago record = pagos.bloquearConfirmado(payment).orElseThrow(Validaciones::noExiste);
        BigDecimal amount = money(data.get("amount"), "Monto a devolver");
        // REGLA CAJA-14: Escribe un monto mayor que S/ 0.00.
        if (amount.signum() <= 0) throw new IllegalArgumentException("Escribe un monto mayor que S/ 0.00.");
        var options = refundable(payment);
        @SuppressWarnings("unchecked") var methods = (List<Map<String, Object>>) options.get("methods");
        // REGLA CAJA-15: Este pago ya fue devuelto por completo.
        if (methods.isEmpty()) throw new IllegalStateException("Este pago ya fue devuelto por completo.");
        // Se usa el medio original del pago; si fue mixto, el elegido o el único que tenga saldo.
        String requested = optional(data, "method").toUpperCase(Locale.ROOT);
        var chosen = methods.stream().filter(method -> method.get("method").equals(requested)).findFirst().orElse(methods.size() == 1 ? methods.getFirst() : null);
        // REGLA CAJA-16: Elige con qué medio se devuelve: 
        if (chosen == null) throw new IllegalArgumentException("Elige con qué medio se devuelve: " + String.join(", ", methods.stream().map(method -> method.get("label") + " (hasta S/ " + method.get("available") + ")").toList()) + ".");
        String method = chosen.get("method").toString(); BigDecimal available = (BigDecimal) chosen.get("available");
        // REGLA CAJA-17: Puedes devolver como máximo S/ 
        if (amount.compareTo(available) > 0) throw new IllegalArgumentException("Puedes devolver como máximo S/ " + available + " por " + chosen.get("label") + ".");
        Long session = optionalId(data, "cashSessionId");
        if (session == null && !options.get("cashSessionId").toString().isBlank()) session = ((Number) options.get("cashSessionId")).longValue();
        if (method.equals("CASH")) {
            // REGLA CAJA-18: Para devolver efectivo abre primero una caja.
            if (session == null) throw new IllegalStateException("Para devolver efectivo abre primero una caja.");
            SesionCaja caja = sesiones.bloquearAbierta(session).orElseThrow(Validaciones::noExiste);
            BigDecimal enCaja = expected(caja);
            // REGLA CAJA-19: En la caja solo hay S/ 
            if (enCaja.compareTo(amount) < 0) throw new IllegalStateException("En la caja solo hay S/ " + enCaja + " en efectivo. Devuelve un monto menor o usa otra caja.");
        } else if (session == null) session = record.getCashSessionId();
        Devolucion d = new Devolucion();
        d.setPaymentId(payment); d.setOrderId(order); d.setCashSessionId(session); d.setMethod(method); d.setAmount(amount);
        d.setReason(optional(data, "reason").isBlank() ? "Devolución al cliente" : str(data, "reason")); d.setCreatedBy(actor);
        long refund = devoluciones.saveAndFlush(d).getId();
        // Si se devolvió dinero, se quitan los puntos que ya no corresponden.
        int awarded = puntos.puntosDelPedido(order);
        if (awarded > 0 && purchase.getCustomerId() != null) {
            int remaining = balances.paid(order).setScale(0, RoundingMode.DOWN).intValue();
            int reverse = Math.max(0, awarded - remaining);
            if (reverse > 0) {
                MovimientoPuntos m = new MovimientoPuntos();
                m.setCustomerId(purchase.getCustomerId()); m.setOrderId(order); m.setPoints(-reverse); m.setReason("DEVOLUCIÓN");
                puntos.save(m);
            }
        }
        return Map.of("id", refund, "message", "Devolución de S/ " + amount + " por " + chosen.get("label") + " registrada.");
    }
}
