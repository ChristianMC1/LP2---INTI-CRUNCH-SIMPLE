package pe.inticrunch.pedidos;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.caja.service.SaldoService;
import pe.inticrunch.catalogo.Producto;
import pe.inticrunch.catalogo.ProductoRepository;
import pe.inticrunch.catalogo.entity.OpcionProducto;
import pe.inticrunch.catalogo.repository.OpcionProductoRepository;
import pe.inticrunch.clientes.entity.Cliente;
import pe.inticrunch.clientes.repository.ClienteRepository;
import pe.inticrunch.clientes.service.ClienteService;
import pe.inticrunch.preparacion.entity.ZonaReparto;
import pe.inticrunch.preparacion.repository.DespachoRepository;
import pe.inticrunch.preparacion.repository.ZonaRepartoRepository;
import pe.inticrunch.preparacion.service.PreparacionService;
import pe.inticrunch.identidad.CuentaService;
import pe.inticrunch.operaciones.Filas;
import pe.inticrunch.operaciones.Validaciones;
import pe.inticrunch.operaciones.repository.LocalRepository;
import pe.inticrunch.pedidos.entity.*;
import pe.inticrunch.pedidos.repository.*;
import pe.inticrunch.reservas.repository.MesaRepository;
import pe.inticrunch.reservas.service.ReservaService;

@Service
public class PedidoService extends Validaciones {
    private final PreparacionService fulfillment;
    private final SaldoService balances;
    private final ClienteService customers;
    private final CuentaService accounts;
    private final ReservaService tables;
    private final PedidoClienteRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final OpcionDetallePedidoRepository opcionesDetalle;
    private final EventoPedidoRepository eventos;
    private final DescuentoPedidoRepository descuentos;
    private final ClienteRepository clientes;
    private final ProductoRepository productos;
    private final OpcionProductoRepository opciones;
    private final ZonaRepartoRepository zonas;
    private final DespachoRepository despachos;
    private final MesaRepository mesas;
    private final LocalRepository locales;
    private final NumeroPedido numero;

    public PedidoService(PreparacionService fulfillment, SaldoService balances, ClienteService customers, CuentaService accounts, ReservaService tables,
                         PedidoClienteRepository pedidos, DetallePedidoRepository detalles, OpcionDetallePedidoRepository opcionesDetalle, EventoPedidoRepository eventos,
                         DescuentoPedidoRepository descuentos, ClienteRepository clientes, ProductoRepository productos, OpcionProductoRepository opciones,
                         ZonaRepartoRepository zonas, DespachoRepository despachos, MesaRepository mesas, LocalRepository locales, NumeroPedido numero) {
        this.fulfillment = fulfillment; this.balances = balances; this.customers = customers; this.accounts = accounts; this.tables = tables;
        this.pedidos = pedidos; this.detalles = detalles; this.opcionesDetalle = opcionesDetalle; this.eventos = eventos; this.descuentos = descuentos;
        this.clientes = clientes; this.productos = productos; this.opciones = opciones; this.zonas = zonas; this.despachos = despachos; this.mesas = mesas;
        this.locales = locales; this.numero = numero;
    }

    /** Pedido hecho desde la web por una persona con sesión iniciada: se completan sus datos de cliente. */
    @Transactional public Map<String, Object> createForAccount(Map<String, Object> data, Authentication auth) {
        Map<String, Object> request = new LinkedHashMap<>(data);
        var profile = accounts.profile(auth);
        if (optional(request, "customerName").isBlank()) request.put("customerName", profile.get("fullName"));
        if (optional(request, "customerPhone").isBlank()) request.put("customerPhone", profile.get("phone"));
        // REGLA PEDIDOS-01: Elige la mesa donde comerás.
        if ("DINE_IN".equalsIgnoreCase(optional(request, "channel")) && optional(request, "tableId").isBlank()) throw new IllegalArgumentException("Elige la mesa donde comerás.");
        request.put("_customerId", accounts.customerFor(auth));
        return create(request, auth.getName());
    }

    @Transactional public Map<String, Object> create(Map<String, Object> data, String subject) {
        String channel = choice(data, "channel", "DINE_IN", "PICKUP", "TAKEAWAY", "DELIVERY");
        String name = str(data, "customerName"), phone = str(data, "customerPhone");
        // REGLA PEDIDOS-02: Ingresa un nombre y teléfono válidos.
        if (name.length() > 120 || name.length() < 2 || !phone.matches("[+0-9 ()-]{7,20}")) throw new IllegalArgumentException("Ingresa un nombre y teléfono válidos.");
        UUID requestKey = optional(data, "requestKey").isBlank() ? UUID.randomUUID() : UUID.fromString(str(data, "requestKey"));
        locales.bloquear(1L); // evita pedidos duplicados por doble clic
        var duplicate = pedidos.findByRequestKey(requestKey);
        if (duplicate.isPresent()) return get(duplicate.get().getId(), true);

        ZonaReparto zone = null; String address = null; BigDecimal fee = BigDecimal.ZERO;
        if (channel.equals("DELIVERY")) {
            address = str(data, "deliveryAddress");
            zone = data.containsKey("zoneId") ? zonas.findByIdAndActiveTrue(id(data, "zoneId")).orElseThrow(Validaciones::noExiste)
                : zonas.activaPorNombre(str(data, "deliveryZone")).orElseThrow(Validaciones::noExiste);
            fee = zone.getFee();
        }
        long customer;
        if (data.get("_customerId") instanceof Number known) customer = known.longValue();
        else customer = clientes.findByPhone(phone).orElseGet(() -> { Cliente c = new Cliente(); c.setFullName(name); c.setPhone(phone); return clientes.save(c); }).getId();

        PedidoCliente order = new PedidoCliente();
        order.setCustomerName(name); order.setCustomerPhone(phone); order.setCustomerSubject(subject); order.setChannel(channel); order.setStatus("RECEIVED");
        order.setLocationId(1L); order.setCustomerId(customer); order.setDeliveryAddress(address); order.setDeliveryZone(zone == null ? null : zone.getName());
        order.setDeliveryFee(fee); order.setNotes(optional(data, "notes")); order.setRequestKey(requestKey);
        order = pedidos.save(order);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (var detail : lines(data, "lines")) {
            long product = id(detail, "productId"); int quantity = integer(detail.get("quantity"), "Cantidad");
            Producto record = productos.findById(product).filter(p -> p.isActive() && p.isAvailable()).orElseThrow(Validaciones::noExiste);
            BigDecimal price = record.getPrice();
            List<OpcionProducto> elegidas = new ArrayList<>();
            for (long option : ids(detail.get("optionIds"))) {
                OpcionProducto selected = opciones.activaParaProducto(option, product).orElseThrow(Validaciones::noExiste);
                elegidas.add(selected); price = price.add(selected.getExtraPrice());
            }
            DetallePedido line = new DetallePedido();
            line.setOrderId(order.getId()); line.setProductId(product); line.setProductName(record.getName()); line.setQuantity(quantity); line.setUnitPrice(price); line.setNotes(optional(detail, "notes"));
            line = detalles.save(line);
            for (var option : elegidas) {
                OpcionDetallePedido extra = new OpcionDetallePedido();
                extra.setOrderLineId(line.getId()); extra.setOptionId(option.getId()); extra.setOptionName(option.getName()); extra.setExtraPrice(option.getExtraPrice());
                opcionesDetalle.save(extra);
            }
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(quantity)));
        }
        order.setSubtotal(subtotal); order.setTotal(subtotal.add(fee));
        pedidos.flush();

        long orderId = order.getId();
        if (!optional(data, "promotionId").isBlank()) customers.redeemPromotion(Map.of("promotionId", id(data, "promotionId"), "orderId", orderId));
        else if (!optional(data, "promotionCode").isBlank()) customers.applyCode(orderId, str(data, "promotionCode"));
        if (channel.equals("DINE_IN") && !optional(data, "tableId").isBlank())
            tables.seatOrder(orderId, id(data, "tableId"), name, customer, subject, optional(data, "partySize").isBlank() ? 0 : integer(data.get("partySize"), "Personas"));
        fulfillment.attachOrder(orderId, channel, address, zone);
        fulfillment.recordOrderStatus(orderId, null, "RECEIVED", subject == null ? "INVITADO" : subject);
        return get(orderId, true);
    }

    @Transactional(readOnly = true) public List<Map<String, Object>> list() { return resumen(pedidos.recientes(PageRequest.of(0, 300))); }
    @Transactional(readOnly = true) public List<Map<String, Object>> listMine(String subject) { return resumen(pedidos.deLaCuenta(subject, PageRequest.of(0, 300))); }
    private List<Map<String, Object>> resumen(List<PedidoCliente> lista) {
        var numeros = numero.todos();
        return lista.stream().map(o -> armar(o, false, numeros.get(o.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(long order, boolean includeToken) {
        PedidoCliente o = pedidos.findById(order).orElseThrow(Validaciones::noExiste);
        return armar(o, includeToken, numero.de(order));
    }

    /** Arma el pedido con las mismas claves que espera el frontend. */
    private Map<String, Object> armar(PedidoCliente o, boolean includeToken, Long number) {
        String tableCode = o.getTableId() == null ? null : mesas.findById(o.getTableId()).map(m -> m.getTableCode()).orElse(null);
        Map<String, Object> result = Filas.of("id", o.getId(), "number", number, "tableId", o.getTableId(), "tableCode", tableCode,
            "customerName", o.getCustomerName(), "customerPhone", o.getCustomerPhone(), "channel", o.getChannel(), "status", o.getStatus(),
            "subtotal", o.getSubtotal(), "discount", o.getDiscount(), "deliveryFee", o.getDeliveryFee(), "total", o.getTotal(), "createdAt", o.getCreatedAt(),
            "deliveryAddress", o.getDeliveryAddress(), "deliveryZone", o.getDeliveryZone(), "notes", o.getNotes());
        if (includeToken) result.put("trackingToken", o.getTrackingToken());
        var lineas = detalles.findByOrderIdOrderById(o.getId());
        Map<Long, List<Map<String, Object>>> extras = new HashMap<>();
        if (!lineas.isEmpty())
            for (var opt : opcionesDetalle.findByOrderLineIdInOrderByOptionId(lineas.stream().map(DetallePedido::getId).toList()))
                extras.computeIfAbsent(opt.getOrderLineId(), k -> new ArrayList<>()).add(Filas.of("name", opt.getOptionName(), "price", opt.getExtraPrice()));
        List<Map<String, Object>> details = new ArrayList<>();
        for (var l : lineas) details.add(Filas.of("id", l.getId(), "productId", l.getProductId(), "name", l.getProductName(), "quantity", l.getQuantity(),
            "unitPrice", l.getUnitPrice(), "subtotal", l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())), "notes", l.getNotes(),
            "product", Map.of("id", l.getProductId(), "name", Objects.toString(l.getProductName(), "Producto")), "options", extras.getOrDefault(l.getId(), List.of())));
        result.put("lines", details);
        result.put("paid", balances.paid(o.getId()));
        result.put("balance", balances.due(o.getId()));
        return result;
    }

    public long byToken(UUID token) { return pedidos.findByTrackingToken(token).orElseThrow(Validaciones::noExiste).getId(); }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(long order) {
        return eventos.findByOrderIdOrderById(order).stream().map(e -> Filas.of("Anterior", e.getOldStatus(), "Estado", e.getNewStatus(), "Responsable", e.getChangedBy(), "Fecha", e.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> tracking(UUID token) {
        long order = byToken(token);
        var result = get(order, false);
        result.put("history", history(order));
        result.put("dispatch", despachos.findByOrderId(order).stream().map(d -> Filas.of("status", d.getStatus(), "address", d.getAddress(), "zone", d.getZone(), "estimatedAt", d.getEstimatedAt())).toList());
        return result;
    }

    @Transactional public Map<String, Object> cancel(long order, String actor) { fulfillment.transition(order, "CANCELLED", actor); return get(order, false); }
    @Transactional public Map<String, Object> changeStatus(long order, String status, String actor) { fulfillment.transition(order, status.toUpperCase(Locale.ROOT), actor); return get(order, false); }

    @Transactional public Map<String, Object> discount(long order, Map<String, Object> data, String actor) {
        PedidoCliente record = pedidos.bloquear(order).orElseThrow(Validaciones::noExiste);
        // REGLA PEDIDOS-03: Los descuentos se autorizan antes del cobro y de la preparación.
        if (!record.getStatus().equals("RECEIVED") || balances.paid(order).signum() > 0) throw new IllegalStateException("Los descuentos se autorizan antes del cobro y de la preparación.");
        BigDecimal discount = money(data.get("amount"), "Descuento");
        // REGLA PEDIDOS-04: El descuento debe ser positivo y menor que el total.
        if (discount.signum() <= 0 || discount.compareTo(record.getTotal()) >= 0) throw new IllegalArgumentException("El descuento debe ser positivo y menor que el total.");
        DescuentoPedido d = new DescuentoPedido();
        d.setOrderId(order); d.setAmount(discount); d.setReason(str(data, "reason")); d.setAuthorizedBy(actor);
        descuentos.save(d);
        record.setDiscount(record.getDiscount().add(discount));
        record.setTotal(record.getTotal().subtract(discount));
        pedidos.flush();
        return Map.of("message", "Descuento autorizado registrado.", "order", get(order, false));
    }
}
