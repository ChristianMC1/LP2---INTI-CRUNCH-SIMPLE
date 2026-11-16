package pe.inticrunch.compras.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.inventario.entity.Almacen;
import pe.inticrunch.inventario.entity.Insumo;
import pe.inticrunch.inventario.entity.LineaMovimiento;
import pe.inticrunch.inventario.entity.MovimientoInventario;
import pe.inticrunch.inventario.repository.AlmacenRepository;
import pe.inticrunch.inventario.repository.InsumoRepository;
import pe.inticrunch.inventario.repository.LineaMovimientoRepository;
import pe.inticrunch.inventario.repository.MovimientoInventarioRepository;
import pe.inticrunch.inventario.service.StockService;
import pe.inticrunch.operaciones.Filas;
import pe.inticrunch.operaciones.Validaciones;
import pe.inticrunch.compras.entity.*;
import pe.inticrunch.compras.repository.*;

@Service
public class ComprasService extends Validaciones {
    private final StockService stock;
    private final ProveedorRepository proveedores;
    private final PrecioProveedorRepository precios;
    private final SolicitudCompraRepository solicitudes;
    private final LineaSolicitudRepository lineasSolicitud;
    private final OrdenCompraRepository ordenes;
    private final LineaOrdenCompraRepository lineasOrden;
    private final RecepcionRepository recepciones;
    private final LineaRecepcionRepository lineasRecepcion;
    private final FacturaCompraRepository facturas;
    private final PagoFacturaRepository pagosFactura;
    private final InsumoRepository insumos;
    private final AlmacenRepository almacenes;
    private final MovimientoInventarioRepository movimientos;
    private final LineaMovimientoRepository lineasMovimiento;

    public ComprasService(StockService stock, ProveedorRepository proveedores, PrecioProveedorRepository precios, SolicitudCompraRepository solicitudes,
                          LineaSolicitudRepository lineasSolicitud, OrdenCompraRepository ordenes, LineaOrdenCompraRepository lineasOrden, RecepcionRepository recepciones,
                          LineaRecepcionRepository lineasRecepcion, FacturaCompraRepository facturas, PagoFacturaRepository pagosFactura, InsumoRepository insumos,
                          AlmacenRepository almacenes, MovimientoInventarioRepository movimientos, LineaMovimientoRepository lineasMovimiento) {
        this.stock = stock; this.proveedores = proveedores; this.precios = precios; this.solicitudes = solicitudes; this.lineasSolicitud = lineasSolicitud;
        this.ordenes = ordenes; this.lineasOrden = lineasOrden; this.recepciones = recepciones; this.lineasRecepcion = lineasRecepcion; this.facturas = facturas;
        this.pagosFactura = pagosFactura; this.insumos = insumos; this.almacenes = almacenes; this.movimientos = movimientos; this.lineasMovimiento = lineasMovimiento;
    }

    private Map<Long, Insumo> insumosPorId() { return insumos.findAll().stream().collect(Collectors.toMap(Insumo::getId, i -> i)); }
    private Map<Long, String> proveedoresPorId() { return proveedores.findAll().stream().collect(Collectors.toMap(Proveedor::getId, Proveedor::getName)); }
    /** "2 kg de Papa, 1 und de Aceite": lo que pide una orden o solicitud. */
    public static String describir(Map<Long, BigDecimal> cantidades, Map<Long, Insumo> insumo) {
        String texto = cantidades.entrySet().stream().filter(e -> insumo.containsKey(e.getKey()))
            .sorted(Comparator.comparing(e -> insumo.get(e.getKey()).getName()))
            .map(e -> e.getValue().stripTrailingZeros().toPlainString() + " " + insumo.get(e.getKey()).getUnit() + " de " + insumo.get(e.getKey()).getName())
            .collect(Collectors.joining(", "));
        return texto.isBlank() ? null : texto;
    }
    /** Descripción de lo que se compra en cada orden. */
    public Map<Long, String> detalleDeOrdenes(Collection<Long> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        var insumo = insumosPorId();
        Map<Long, Map<Long, BigDecimal>> porOrden = new HashMap<>();
        for (var l : lineasOrden.findByPurchaseOrderIdIn(orderIds)) porOrden.computeIfAbsent(l.getPurchaseOrderId(), k -> new LinkedHashMap<>()).merge(l.getIngredientId(), l.getQuantity(), BigDecimal::add);
        Map<Long, String> result = new HashMap<>();
        porOrden.forEach((orden, cantidades) -> result.put(orden, describir(cantidades, insumo)));
        return result;
    }

    // ---------- Proveedores ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> suppliers() {
        return proveedores.findByActiveTrueOrderById().stream().map(s -> Filas.of("ID", s.getId(), "Proveedor", s.getName(), "RUC", s.getTaxId(), "Teléfono", s.getPhone(), "Correo", s.getEmail())).toList();
    }
    @Transactional public Map<String, Object> deleteSupplier(long supplier) {
        Proveedor s = proveedores.bloquear(supplier).orElseThrow(Validaciones::noExiste);
        // REGLA COMPRAS-01: Este proveedor tiene órdenes de compra sin recibir. Recíbelas o cancélalas primero.
        if (ordenes.tienePendientes(supplier)) throw new IllegalStateException("Este proveedor tiene órdenes de compra sin recibir. Recíbelas o cancélalas primero.");
        s.setActive(false);
        return Map.of("message", "Proveedor retirado. Sus compras anteriores se conservan.");
    }
    @Transactional public Map<String, Object> createSupplier(Map<String, Object> data) {
        validateTax(data);
        Proveedor s = new Proveedor();
        s.setName(str(data, "name")); s.setTaxId(optional(data, "taxId")); s.setPhone(optional(data, "phone")); s.setEmail(optional(data, "email"));
        return Map.of("id", proveedores.save(s).getId(), "message", "Proveedor registrado.");
    }
    @Transactional public Map<String, Object> updateSupplier(long supplier, Map<String, Object> data) {
        validateTax(data);
        Proveedor s = proveedores.bloquear(supplier).orElseThrow(Validaciones::noExiste);
        s.setName(str(data, "name")); s.setTaxId(optional(data, "taxId")); s.setPhone(optional(data, "phone")); s.setEmail(optional(data, "email"));
        return Map.of("message", "Proveedor actualizado.");
    }
    private void validateTax(Map<String, Object> data) {
        // REGLA COMPRAS-02: El RUC debe tener 11 dígitos.
        if (!optional(data, "taxId").isBlank() && !optional(data, "taxId").matches("[0-9]{11}")) throw new IllegalArgumentException("El RUC debe tener 11 dígitos.");
    }

    // ---------- Comparación de precios ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> catalog() {
        Map<Long, Proveedor> activos = proveedores.findByActiveTrueOrderById().stream().collect(Collectors.toMap(Proveedor::getId, s -> s));
        var insumo = insumosPorId();
        return precios.findAll().stream().filter(p -> activos.containsKey(p.getSupplierId()) && insumo.containsKey(p.getIngredientId()))
            .sorted(Comparator.comparing((PrecioProveedor p) -> insumo.get(p.getIngredientId()).getName()).thenComparing(PrecioProveedor::getLastPrice))
            .map(p -> Filas.of("_supplierId", p.getSupplierId(), "_ingredientId", p.getIngredientId(), "Proveedor", activos.get(p.getSupplierId()).getName(),
                "Insumo", insumo.get(p.getIngredientId()).getName(), "Precio", p.getLastPrice(), "Condiciones", p.getSupplierSku(), "Actualizado", p.getUpdatedAt())).toList();
    }
    /** Guarda (o actualiza) el último precio de un proveedor para un insumo. */
    private void guardarPrecio(long supplier, long ingredient, BigDecimal price, String conditions) {
        PrecioProveedor p = precios.findById(new PrecioProveedor.Clave(supplier, ingredient)).orElseGet(() -> {
            PrecioProveedor nuevo = new PrecioProveedor(); nuevo.setSupplierId(supplier); nuevo.setIngredientId(ingredient); return nuevo;
        });
        p.setLastPrice(price); p.setUpdatedAt(Instant.now());
        if (conditions != null) p.setSupplierSku(conditions);
        precios.save(p);
    }
    @Transactional public Map<String, Object> quote(Map<String, Object> data) {
        guardarPrecio(id(data, "supplierId"), id(data, "ingredientId"), money(data.get("unitPrice"), "Precio"), optional(data, "conditions"));
        return Map.of("message", "Cotización actualizada.");
    }
    @Transactional public Map<String, Object> deleteQuote(long supplier, long ingredient) {
        precios.findById(new PrecioProveedor.Clave(supplier, ingredient)).ifPresent(precios::delete);
        return Map.of("message", "Precio eliminado de la comparación.");
    }

    // ---------- Órdenes de compra ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> purchases() {
        var lista = ordenes.recientes(PageRequest.of(0, 300));
        var proveedor = proveedoresPorId();
        var detalle = detalleDeOrdenes(lista.stream().map(OrdenCompra::getId).toList());
        return lista.stream().map(p -> Filas.of("Orden", p.getId(), "Proveedor", proveedor.get(p.getSupplierId()), "Qué se compra", detalle.get(p.getId()),
            "Estado", p.getStatus(), "Total", p.getTotal(), "Responsable", p.getCreatedBy(), "Fecha", p.getCreatedAt())).toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> purchaseLines(long purchase) {
        var insumo = insumosPorId();
        return lineasOrden.findByPurchaseOrderIdOrderById(purchase).stream().sorted(Comparator.comparing(l -> insumo.get(l.getIngredientId()).getName()))
            .map(l -> Filas.of("ID", l.getId(), "Insumo ID", l.getIngredientId(), "Insumo", insumo.get(l.getIngredientId()).getName(), "Solicitado", l.getQuantity(),
                "Recibido", l.getReceivedQuantity(), "Pendiente", l.getQuantity().subtract(l.getReceivedQuantity()), "Precio unitario", l.getUnitPrice())).toList();
    }

    // ---------- Solicitudes ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> requests() {
        var lista = solicitudes.recientes(PageRequest.of(0, 300));
        var insumo = insumosPorId();
        Map<Long, Map<Long, BigDecimal>> porSolicitud = new HashMap<>();
        if (!lista.isEmpty()) for (var l : lineasSolicitud.findByRequestIdIn(lista.stream().map(SolicitudCompra::getId).toList()))
            porSolicitud.computeIfAbsent(l.getRequestId(), k -> new LinkedHashMap<>()).merge(l.getIngredientId(), l.getQuantity(), BigDecimal::add);
        return lista.stream().map(r -> Filas.of("ID", r.getId(), "Motivo", r.getNote(), "Qué se necesita", describir(porSolicitud.getOrDefault(r.getId(), Map.of()), insumo),
            "Estado", r.getStatus(), "Responsable", r.getCreatedBy(), "Fecha", r.getCreatedAt())).toList();
    }
    private void guardarLineas(long request, List<Map<String, Object>> details) {
        for (var detail : details) {
            LineaSolicitud l = new LineaSolicitud();
            l.setRequestId(request); l.setIngredientId(id(detail, "ingredientId")); l.setQuantity(positive(detail.get("quantity"), "Cantidad"));
            lineasSolicitud.save(l);
        }
    }
    @Transactional public Map<String, Object> updateRequest(long request, Map<String, Object> data) {
        SolicitudCompra r = solicitudes.bloquearPendiente(request).orElseThrow(Validaciones::noExiste);
        var details = lines(data, "lines");
        r.setNote(str(data, "note"));
        lineasSolicitud.borrarDeSolicitud(request);
        guardarLineas(request, details);
        return Map.of("message", "Solicitud actualizada.");
    }
    @Transactional public Map<String, Object> deleteRequest(long request) {
        SolicitudCompra r = solicitudes.bloquearPendiente(request).orElseThrow(Validaciones::noExiste);
        if (ordenes.existsByRequestId(request)) { r.setStatus("CANCELLED"); return Map.of("message", "Solicitud anulada."); }
        lineasSolicitud.borrarDeSolicitud(request);
        solicitudes.delete(r);
        return Map.of("message", "Solicitud eliminada.");
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> requestLines(long request) {
        var insumo = insumosPorId();
        return lineasSolicitud.findByRequestId(request).stream().map(l -> {
            Insumo i = insumo.get(l.getIngredientId());
            return Filas.of("ID", i.getId(), "Insumo", i.getName(), "Cantidad", l.getQuantity(), "Unidad", i.getUnit());
        }).toList();
    }
    @Transactional public Map<String, Object> createRequest(Map<String, Object> data, String actor) {
        SolicitudCompra r = new SolicitudCompra();
        r.setNote(str(data, "note")); r.setCreatedBy(actor);
        long request = solicitudes.saveAndFlush(r).getId();
        guardarLineas(request, lines(data, "lines"));
        return Map.of("id", request, "message", "Solicitud de abastecimiento registrada.");
    }

    @Transactional public Map<String, Object> createPurchase(Map<String, Object> data, String actor) {
        long supplier = id(data, "supplierId");
        proveedores.findByIdAndActiveTrue(supplier).orElseThrow(Validaciones::noExiste);
        Long request = optionalId(data, "requestId");
        SolicitudCompra solicitud = request == null ? null : solicitudes.bloquearPendiente(request).orElseThrow(Validaciones::noExiste);
        OrdenCompra orden = new OrdenCompra();
        orden.setLocationId(1L); orden.setSupplierId(supplier); orden.setRequestId(request); orden.setCreatedBy(actor);
        long purchase = ordenes.saveAndFlush(orden).getId();
        BigDecimal total = BigDecimal.ZERO;
        Map<Long, BigDecimal> pedido = new HashMap<>();
        for (var detail : lines(data, "lines")) {
            long ingredient = id(detail, "ingredientId");
            insumos.findByIdAndActiveTrue(ingredient).orElseThrow(Validaciones::noExiste);
            BigDecimal quantity = positive(detail.get("quantity"), "Cantidad"), price = money(detail.get("unitPrice"), "Precio");
            LineaOrdenCompra l = new LineaOrdenCompra();
            l.setPurchaseOrderId(purchase); l.setIngredientId(ingredient); l.setQuantity(quantity); l.setUnitPrice(price);
            lineasOrden.save(l);
            guardarPrecio(supplier, ingredient, price, null);
            pedido.merge(ingredient, quantity, BigDecimal::max);
            total = total.add(quantity.multiply(price));
        }
        if (solicitud != null) {
            // La orden debe cubrir cada insumo de la solicitud en la cantidad pedida (al menos en una línea).
            boolean missing = lineasSolicitud.findByRequestId(request).stream().anyMatch(r -> pedido.getOrDefault(r.getIngredientId(), BigDecimal.ZERO).compareTo(r.getQuantity()) < 0);
            // REGLA COMPRAS-03: La orden debe cubrir los insumos y cantidades de la solicitud asociada.
            if (missing) throw new IllegalArgumentException("La orden debe cubrir los insumos y cantidades de la solicitud asociada.");
            solicitud.setStatus("ORDERED");
        }
        orden.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        return Map.of("id", purchase, "message", "Orden de compra creada.");
    }

    @Transactional public Map<String, Object> cancelPurchase(long purchase) {
        OrdenCompra orden = ordenes.bloquear(purchase).filter(o -> o.getStatus().equals("PENDING")).orElseThrow(Validaciones::noExiste);
        if (orden.getRequestId() != null) solicitudes.findById(orden.getRequestId()).ifPresent(r -> r.setStatus("PENDING"));
        orden.setStatus("CANCELLED");
        return Map.of("message", "Orden de compra cancelada.");
    }

    @Transactional public Map<String, Object> receivePurchase(long purchase, Map<String, Object> data, String actor) {
        OrdenCompra orden = ordenes.bloquear(purchase).filter(o -> Set.of("PENDING", "PARTIAL").contains(o.getStatus())).orElseThrow(Validaciones::noExiste);
        long warehouse = optional(data, "warehouseId").isBlank() ? 1 : id(data, "warehouseId");
        almacenes.findByIdAndActiveTrue(warehouse).orElseThrow(Validaciones::noExiste);
        Recepcion recepcion = new Recepcion();
        recepcion.setPurchaseOrderId(purchase); recepcion.setDocumentNumber(optional(data, "documentNumber")); recepcion.setWarehouseId(warehouse);
        recepcion.setReceivedBy(actor); recepcion.setNote(optional(data, "note"));
        long receipt = recepciones.saveAndFlush(recepcion).getId();
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setLocationId(1L); movimiento.setWarehouseId(warehouse); movimiento.setKind("IN"); movimiento.setReference("OC-" + purchase + "/REC-" + receipt);
        movimiento.setNote("Recepción de compra"); movimiento.setCreatedBy(actor);
        long movement = movimientos.saveAndFlush(movimiento).getId();
        var details = lines(data, "lines").stream().sorted(Comparator.comparing(detail -> id(detail, "lineId"))).toList();
        for (var detail : details) {
            long line = id(detail, "lineId");
            LineaOrdenCompra requested = lineasOrden.bloquear(line, purchase).orElseThrow(Validaciones::noExiste);
            BigDecimal quantity = decimal(detail.get("quantity"), "Cantidad recibida"), observed = decimal(detail.getOrDefault("observedQuantity", 0), "Cantidad observada");
            // REGLA COMPRAS-04: Indica al menos una cantidad aceptada u observada.
            if (quantity.add(observed).signum() <= 0) throw new IllegalArgumentException("Indica al menos una cantidad aceptada u observada.");
            BigDecimal remaining = requested.getQuantity().subtract(requested.getReceivedQuantity());
            // REGLA COMPRAS-05: La recepción más lo observado supera la cantidad pendiente.
            if (quantity.add(observed).compareTo(remaining) > 0) throw new IllegalArgumentException("La recepción más lo observado supera la cantidad pendiente.");
            long ingredient = requested.getIngredientId();
            LocalDate expiry = optional(detail, "expiresOn").isBlank() ? null : LocalDate.parse(str(detail, "expiresOn"));
            // REGLA COMPRAS-06: No se admite un lote vencido.
            if (expiry != null && expiry.isBefore(hoy())) throw new IllegalArgumentException("No se admite un lote vencido.");
            if (quantity.signum() > 0) stock.add(warehouse, ingredient, quantity, optional(detail, "batchCode").isBlank() ? "REC-" + receipt : optional(detail, "batchCode"), expiry);
            requested.setReceivedQuantity(requested.getReceivedQuantity().add(quantity));
            LineaRecepcion lr = new LineaRecepcion();
            lr.setReceiptId(receipt); lr.setPurchaseLineId(line); lr.setQuantity(quantity); lr.setObservedQuantity(observed); lr.setNote(optional(detail, "note"));
            lineasRecepcion.save(lr);
            if (quantity.signum() > 0) {
                LineaMovimiento lm = new LineaMovimiento();
                lm.setMovementId(movement); lm.setIngredientId(ingredient); lm.setQuantity(quantity); lm.setUnitCost(requested.getUnitPrice());
                lineasMovimiento.save(lm);
            }
        }
        lineasOrden.flush();
        long outstanding = lineasOrden.pendientes(purchase);
        orden.setStatus(outstanding == 0 ? "RECEIVED" : "PARTIAL");
        if (outstanding == 0) orden.setReceivedAt(Instant.now());
        return Map.of("id", receipt, "message", outstanding == 0 ? "Compra recibida por completo." : "Recepción parcial registrada. Lo observado queda pendiente de reposición.");
    }

    // ---------- Recepciones ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> receipts() {
        Map<Long, String> almacen = almacenes.findAll().stream().collect(Collectors.toMap(Almacen::getId, Almacen::getName));
        return recepciones.recientes(PageRequest.of(0, 300)).stream().map(r -> Filas.of("ID", r.getId(), "Orden", r.getPurchaseOrderId(), "Documento", r.getDocumentNumber(),
            "Almacén", almacen.get(r.getWarehouseId()), "Responsable", r.getReceivedBy(), "Fecha", r.getReceivedAt())).toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> receiptLines(long receipt) {
        var insumo = insumosPorId();
        return lineasRecepcion.findByReceiptId(receipt).stream().map(r -> {
            var l = lineasOrden.findById(r.getPurchaseLineId()).orElseThrow(Validaciones::noExiste);
            return Filas.of("Insumo", insumo.get(l.getIngredientId()).getName(), "Aceptado", r.getQuantity(), "Observado", r.getObservedQuantity(), "Nota", r.getNote());
        }).toList();
    }

    // ---------- Facturas y cuentas por pagar ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> invoices() {
        var proveedor = proveedoresPorId();
        Map<Long, BigDecimal> pagado = new HashMap<>();
        for (Object[] row : pagosFactura.pagadoPorFactura()) pagado.put((Long) row[0], (BigDecimal) row[1]);
        return facturas.porVencimiento().stream().map(f -> Filas.of("ID", f.getId(), "Orden", f.getPurchaseOrderId(), "Proveedor", proveedor.get(f.getSupplierId()),
            "Documento", f.getNumber(), "Total", f.getTotal(), "Pendiente", f.getTotal().subtract(pagado.getOrDefault(f.getId(), BigDecimal.ZERO)), "Vencimiento", f.getDueDate())).toList();
    }
    @Transactional public Map<String, Object> invoice(Map<String, Object> data) {
        long purchase = id(data, "purchaseOrderId");
        OrdenCompra order = ordenes.bloquear(purchase).filter(o -> !o.getStatus().equals("CANCELLED")).orElseThrow(Validaciones::noExiste);
        BigDecimal total = money(data.get("total"), "Total");
        BigDecimal invoiced = facturas.facturadoDeOrden(purchase);
        // REGLA COMPRAS-07: El monto facturado supera la orden de compra.
        if (total.signum() <= 0 || invoiced.add(total).compareTo(order.getTotal()) > 0) throw new IllegalArgumentException("El monto facturado supera la orden de compra.");
        FacturaCompra f = new FacturaCompra();
        f.setPurchaseOrderId(purchase); f.setSupplierId(order.getSupplierId()); f.setNumber(str(data, "number")); f.setTotal(total); f.setDueDate(LocalDate.parse(str(data, "dueDate")));
        return Map.of("id", facturas.save(f).getId(), "message", "Documento y cuenta por pagar registrados.");
    }
    @Transactional public Map<String, Object> payInvoice(long invoice, Map<String, Object> data, String actor) {
        FacturaCompra record = facturas.bloquear(invoice).orElseThrow(Validaciones::noExiste);
        BigDecimal paid = pagosFactura.pagadoDeFactura(invoice);
        BigDecimal amount = money(data.get("amount"), "Monto");
        // REGLA COMPRAS-08: El pago supera el saldo pendiente.
        if (amount.signum() <= 0 || amount.compareTo(record.getTotal().subtract(paid)) > 0) throw new IllegalArgumentException("El pago supera el saldo pendiente.");
        PagoFactura p = new PagoFactura();
        p.setInvoiceId(invoice); p.setAmount(amount); p.setReference(optional(data, "reference")); p.setCreatedBy(actor);
        return Map.of("id", pagosFactura.save(p).getId(), "message", "Pago al proveedor registrado en modo simulado.");
    }
}
