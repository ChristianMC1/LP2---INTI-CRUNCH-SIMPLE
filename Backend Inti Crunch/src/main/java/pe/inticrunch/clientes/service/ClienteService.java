package pe.inticrunch.clientes.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.caja.repository.AsignacionPagoRepository;
import pe.inticrunch.catalogo.Producto;
import pe.inticrunch.catalogo.ProductoRepository;
import pe.inticrunch.catalogo.repository.OpcionProductoRepository;
import pe.inticrunch.clientes.entity.*;
import pe.inticrunch.clientes.repository.*;
import pe.inticrunch.identidad.CuentaService;
import pe.inticrunch.operaciones.Filas;
import pe.inticrunch.operaciones.Validaciones;
import pe.inticrunch.pedidos.NumeroPedido;
import pe.inticrunch.pedidos.entity.DetallePedido;
import pe.inticrunch.pedidos.entity.PedidoCliente;
import pe.inticrunch.pedidos.repository.*;
import pe.inticrunch.reservas.repository.ReservaRepository;

@Service
public class ClienteService extends Validaciones {
    private final ClienteRepository clientes;
    private final PromocionRepository promociones;
    private final UsoPromocionRepository usos;
    private final MovimientoPuntosRepository puntos;
    private final PuntosPorClienteRepository puntosPorCliente;
    private final ResumenClientesRepository resumen;
    private final PedidoClienteRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final AsignacionPagoRepository asignaciones;
    private final ReservaRepository reservas;
    private final ProductoRepository productos;
    private final OpcionProductoRepository opciones;
    private final NumeroPedido numero;

    public ClienteService(ClienteRepository clientes, PromocionRepository promociones, UsoPromocionRepository usos, MovimientoPuntosRepository puntos, PuntosPorClienteRepository puntosPorCliente,
                          ResumenClientesRepository resumen, PedidoClienteRepository pedidos, DetallePedidoRepository detalles, AsignacionPagoRepository asignaciones,
                          ReservaRepository reservas, ProductoRepository productos, OpcionProductoRepository opciones, NumeroPedido numero) {
        this.clientes = clientes; this.promociones = promociones; this.usos = usos; this.puntos = puntos; this.puntosPorCliente = puntosPorCliente; this.resumen = resumen;
        this.pedidos = pedidos; this.detalles = detalles; this.asignaciones = asignaciones; this.reservas = reservas; this.productos = productos; this.opciones = opciones; this.numero = numero;
    }

    // ---------- Clientes ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> customers() {
        Map<Long, Object[]> totales = new HashMap<>();
        for (Object[] row : resumen.pedidosYConsumoPorCliente()) totales.put((Long) row[0], row);
        Map<Long, Number> puntosDe = new HashMap<>();
        for (Object[] row : puntosPorCliente.puntosPorCliente()) puntosDe.put((Long) row[0], (Number) row[1]);
        return clientes.primeros(PageRequest.of(0, 300)).stream().map(c -> {
            Object[] t = totales.get(c.getId());
            return Filas.of("ID", c.getId(), "Cliente", c.getFullName(), "DNI", c.getDni(), "Teléfono", c.getPhone(), "Correo", c.getEmail(), "Preferencias", c.getPreferences(),
                "Pedidos", t == null ? 0L : ((Number) t[1]).longValue(), "Consumo", t == null ? BigDecimal.ZERO : t[2], "Puntos", puntosDe.getOrDefault(c.getId(), 0).longValue());
        }).toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> customerHistory(long customer) {
        var numeros = numero.todos();
        return pedidos.findByCustomerIdOrderByIdDesc(customer).stream().map(o -> Filas.of("Pedido", numeros.get(o.getId()), "Fecha", o.getCreatedAt(), "Canal", o.getChannel(), "Estado", o.getStatus(), "Total", o.getTotal())).toList();
    }
    private static String dniValido(Map<String, Object> data) {
        String dni = optional(data, "dni");
        // REGLA CLIENTES-01: El DNI debe tener 8 números.
        if (!dni.isBlank() && !dni.matches("[0-9]{8}")) throw new IllegalArgumentException("El DNI debe tener 8 números.");
        return dni.isBlank() ? null : dni;
    }
    @Transactional public Map<String, Object> createCustomer(Map<String, Object> data) {
        String dni = dniValido(data);
        String phone = CuentaService.cleanPhone(str(data, "phone"));
        // REGLA CLIENTES-02: Ya hay un cliente con el celular 
        if (clientes.existsByPhone(phone)) throw new IllegalStateException("Ya hay un cliente con el celular " + phone + ".");
        Cliente c = new Cliente();
        c.setFullName(str(data, "fullName")); c.setPhone(phone); c.setEmail(optional(data, "email")); c.setPreferences(optional(data, "preferences")); c.setDni(dni);
        return Map.of("id", clientes.save(c).getId(), "message", "Cliente registrado.");
    }
    @Transactional public Map<String, Object> updateCustomer(long customer, Map<String, Object> data) {
        Cliente c = clientes.bloquear(customer).orElseThrow(Validaciones::noExiste);
        String dni = dniValido(data);
        c.setFullName(str(data, "fullName")); c.setPhone(CuentaService.cleanPhone(str(data, "phone"))); c.setEmail(optional(data, "email")); c.setPreferences(optional(data, "preferences")); c.setDni(dni);
        return Map.of("message", "Cliente actualizado.");
    }
    @Transactional public Map<String, Object> deleteCustomer(long customer) {
        Cliente c = clientes.bloquear(customer).orElseThrow(Validaciones::noExiste);
        // REGLA CLIENTES-03: Este cliente tiene una cuenta web; no se puede eliminar desde aquí.
        if (c.getSubject() != null) throw new IllegalStateException("Este cliente tiene una cuenta web; no se puede eliminar desde aquí.");
        if (!pedidos.findByCustomerIdOrderByIdDesc(customer).isEmpty() || reservas.existsByCustomerId(customer) || puntos.existsByCustomerId(customer))
            throw new IllegalStateException(c.getFullName() + " ya tiene pedidos o reservas; se conserva para el historial.");
        clientes.delete(c);
        return Map.of("message", "Cliente eliminado.");
    }

    // ---------- Puntos y usos ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> points() {
        var numeros = numero.todos();
        Map<Long, String> nombre = clientes.findAll().stream().collect(Collectors.toMap(Cliente::getId, Cliente::getFullName));
        return puntos.recientes(PageRequest.of(0, 300)).stream().map(m -> Filas.of("ID", m.getId(), "Cliente", nombre.get(m.getCustomerId()), "Pedido", m.getOrderId() == null ? null : numeros.get(m.getOrderId()),
            "Puntos", m.getPoints(), "Motivo", m.getReason(), "Fecha", m.getCreatedAt())).toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> usages() {
        var numeros = numero.todos();
        Map<Long, String> cliente = clientes.findAll().stream().collect(Collectors.toMap(Cliente::getId, Cliente::getFullName));
        Map<Long, String> promocion = promociones.findAll().stream().collect(Collectors.toMap(Promocion::getId, Promocion::getName));
        return usos.recientes(PageRequest.of(0, 300)).stream().map(u -> Filas.of("Promoción", promocion.get(u.getPromotionId()), "Cliente", cliente.get(u.getCustomerId()),
            "Pedido", numeros.get(u.getOrderId()), "Descuento", u.getDiscount(), "Fecha", u.getRedeemedAt())).toList();
    }

    // ---------- Promociones ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> promotions() {
        return promociones.todas().stream().map(p -> Filas.of("ID", p.getId(), "Promoción", p.getName(), "Código", p.getCode(), "Porcentaje", p.getDiscountPercent(), "Monto", p.getDiscountAmount(),
            "Desde", p.getStartsAt(), "Hasta", p.getEndsAt(), "Activa", p.getActive(), "Imagen", p.getImageUrl())).toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicPromotions() {
        return promociones.vigentes(Instant.now()).stream().map(p -> Filas.of("id", p.getId(), "name", p.getName(), "code", p.getCode(), "discountPercent", p.getDiscountPercent(),
            "discountAmount", p.getDiscountAmount(), "minimumOrder", p.getMinimumOrder(), "endsAt", p.getEndsAt(), "imageUrl", p.getImageUrl())).toList();
    }
    @Transactional(readOnly = true)
    public Map<String, Object> promotionConditions(long promotion) {
        Promocion p = promociones.findById(promotion).orElseThrow(Validaciones::noExiste);
        return Filas.of("minimumOrder", p.getMinimumOrder(), "maximumUses", p.getMaximumUses(), "productIds", p.getProductIds().stream().sorted().toList());
    }
    private String validarImagen(Object valor) {
        if (valor == null) return null;
        String imagen = valor.toString();
        // REGLA CLIENTES-04: La imagen debe pesar menos de 1.5 MB.
        if (imagen.length() > 2_000_000) throw new IllegalArgumentException("La imagen debe pesar menos de 1.5 MB.");
        // REGLA CLIENTES-05: Selecciona una imagen válida.
        if (!imagen.startsWith("data:image/") && !imagen.startsWith("https://") && !imagen.startsWith("http://")) throw new IllegalArgumentException("Selecciona una imagen válida.");
        return imagen;
    }
    /** Lee y valida los datos comunes de una promoción (crear o editar). */
    private void llenar(Promocion p, Map<String, Object> data) {
        Object percent = data.get("discountPercent"), amount = data.get("discountAmount");
        // REGLA CLIENTES-06: Indica porcentaje o monto de descuento, no ambos.
        if ((percent == null) == (amount == null)) throw new IllegalArgumentException("Indica porcentaje o monto de descuento, no ambos.");
        BigDecimal porcentaje = percent == null ? null : decimal(percent, "discountPercent"), monto = amount == null ? null : decimal(amount, "discountAmount");
        // REGLA CLIENTES-07: El porcentaje máximo es 100.
        if (porcentaje != null && porcentaje.compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalArgumentException("El porcentaje máximo es 100.");
        Instant start = Instant.parse(str(data, "startsAt")), end = Instant.parse(str(data, "endsAt"));
        // REGLA CLIENTES-08: La fecha de fin debe ser posterior al inicio.
        if (!end.isAfter(start)) throw new IllegalArgumentException("La fecha de fin debe ser posterior al inicio.");
        p.setName(str(data, "name")); p.setCode(data.get("code") == null ? null : data.get("code").toString());
        p.setDiscountPercent(porcentaje); p.setDiscountAmount(monto); p.setStartsAt(start); p.setEndsAt(end);
    }
    @Transactional public Map<String, Object> createPromotion(Map<String, Object> data) {
        Promocion p = new Promocion();
        llenar(p, data);
        p.setImageUrl(validarImagen(data.get("imageUrl")));
        return Map.of("id", promociones.save(p).getId(), "message", "Promoción creada.");
    }
    @Transactional public Map<String, Object> updatePromotion(long promotionId, Map<String, Object> data) {
        Promocion p = promociones.bloquear(promotionId).orElseThrow(Validaciones::noExiste);
        llenar(p, data);
        if (data.get("imageUrl") != null) p.setImageUrl(validarImagen(data.get("imageUrl")));
        return Map.of("id", promotionId, "message", "Promoción actualizada.");
    }
    @Transactional public Map<String, Object> updatePromotionStatus(long promotionId, Map<String, Object> data) {
        Object activa = data.get("active");
        // REGLA CLIENTES-09: Indica el estado activo de la promoción.
        if (!(activa instanceof Boolean value)) throw new IllegalArgumentException("Indica el estado activo de la promoción.");
        promociones.findById(promotionId).orElseThrow(() -> new IllegalArgumentException("No existe la promoción.")).setActive(value);
        return Map.of("id", promotionId, "active", activa, "message", value ? "Promoción activada." : "Promoción desactivada.");
    }
    @Transactional public Map<String, Object> promotionProducts(long promotion, Map<String, Object> data) {
        Promocion p = promociones.bloquear(promotion).orElseThrow(Validaciones::noExiste);
        p.getProductIds().clear();
        for (long product : ids(data.get("productIds"))) { productos.findById(product).filter(Producto::isActive).orElseThrow(Validaciones::noExiste); p.getProductIds().add(product); }
        p.setMinimumOrder(money(data.getOrDefault("minimumOrder", 0), "Consumo mínimo"));
        p.setMaximumUses(data.get("maximumUses") == null ? null : integer(data.get("maximumUses"), "Límite de usos"));
        return Map.of("message", "Condiciones y productos actualizados.");
    }
    @Transactional public Map<String, Object> deletePromotion(long promotion) {
        Promocion p = promociones.bloquear(promotion).orElseThrow(Validaciones::noExiste);
        if (usos.existsByPromotionId(promotion)) { p.setActive(false); return Map.of("message", "La promoción ya se usó; se desactivó y se conserva en el historial."); }
        promociones.delete(p);
        return Map.of("message", "Promoción eliminada.");
    }

    /** Calcula, sin guardar, cuánto descuenta un cupón para el carrito indicado. */
    @Transactional(readOnly = true)
    public Map<String, Object> preview(Map<String, Object> data) {
        String code = optional(data, "code");
        // REGLA CLIENTES-10: Escribe o elige un cupón.
        if (code.isBlank()) throw new IllegalArgumentException("Escribe o elige un cupón.");
        var vigentes = promociones.vigentePorCodigo(code, Instant.now());
        if (vigentes.isEmpty()) return Map.of("valid", false, "discount", 0, "message", "El cupón " + code.toUpperCase(Locale.ROOT) + " no existe o ya venció.");
        Promocion offer = vigentes.getFirst();
        Set<Long> aplica = offer.getProductIds();
        BigDecimal subtotal = BigDecimal.ZERO, eligible = BigDecimal.ZERO;
        for (var line : lines(data, "lines")) {
            long product = id(line, "productId"); int quantity = integer(line.get("quantity"), "Cantidad");
            var producto = productos.findById(product);
            if (producto.isEmpty()) continue;
            BigDecimal price = producto.get().getPrice();
            for (long option : ids(line.get("optionIds"))) price = price.add(opciones.findById(option).map(o -> o.getExtraPrice()).orElse(BigDecimal.ZERO));
            BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
            subtotal = subtotal.add(total);
            if (aplica.isEmpty() || aplica.contains(product)) eligible = eligible.add(total);
        }
        BigDecimal minimum = nz(offer.getMinimumOrder());
        if (subtotal.compareTo(minimum) < 0) return Map.of("valid", false, "discount", 0, "message", "Este cupón pide un consumo mínimo de S/ " + minimum + ". Te faltan S/ " + minimum.subtract(subtotal) + ".");
        if (offer.getMaximumUses() != null && usos.countByPromotionId(offer.getId()) >= offer.getMaximumUses()) return Map.of("valid", false, "discount", 0, "message", "Este cupón ya se agotó.");
        if (eligible.signum() == 0) return Map.of("valid", false, "discount", 0, "message", "Tu pedido no tiene productos de esta promoción.");
        BigDecimal discount = descuento(offer, eligible).min(eligible).min(subtotal.subtract(new BigDecimal("0.01")));
        return Map.of("valid", discount.signum() > 0, "discount", discount, "promotion", offer.getName(), "code", offer.getCode(), "message", "¡Cupón aplicado! Ahorras S/ " + discount + ".");
    }
    private static BigDecimal descuento(Promocion offer, BigDecimal eligible) {
        return offer.getDiscountPercent() != null ? eligible.multiply(offer.getDiscountPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : offer.getDiscountAmount();
    }

    @Transactional public void applyCode(long order, String code) {
        Promocion promotion = promociones.porCodigo(code).stream().findFirst().orElseThrow(Validaciones::noExiste);
        redeemPromotion(Map.of("promotionId", promotion.getId(), "orderId", order));
    }
    /** Aplica la promoción al pedido (antes de prepararlo y antes del primer pago). */
    @Transactional public Map<String, Object> redeemPromotion(Map<String, Object> data) {
        long promotion = id(data, "promotionId"), order = id(data, "orderId");
        PedidoCliente purchase = pedidos.bloquear(order).orElseThrow(Validaciones::noExiste);
        // REGLA CLIENTES-11: Solo se aplica una promoción antes de preparar el pedido y sin otros descuentos.
        if (!purchase.getStatus().equals("RECEIVED") || purchase.getDiscount().signum() > 0) throw new IllegalStateException("Solo se aplica una promoción antes de preparar el pedido y sin otros descuentos.");
        // REGLA CLIENTES-12: Aplica la promoción antes del primer pago.
        if (asignaciones.existsByOrderId(order)) throw new IllegalStateException("Aplica la promoción antes del primer pago.");
        Instant ahora = Instant.now();
        Promocion offer = promociones.bloquear(promotion).filter(p -> Boolean.TRUE.equals(p.getActive()) && !ahora.isBefore(p.getStartsAt()) && !ahora.isAfter(p.getEndsAt())).orElseThrow(Validaciones::noExiste);
        // REGLA CLIENTES-13: El pedido no alcanza el consumo mínimo de la promoción.
        if (purchase.getSubtotal().compareTo(nz(offer.getMinimumOrder())) < 0) throw new IllegalArgumentException("El pedido no alcanza el consumo mínimo de la promoción.");
        // REGLA CLIENTES-14: Se alcanzó el límite de usos de la promoción.
        if (offer.getMaximumUses() != null && usos.countByPromotionId(promotion) >= offer.getMaximumUses()) throw new IllegalStateException("Se alcanzó el límite de usos de la promoción.");
        Set<Long> aplica = offer.getProductIds();
        BigDecimal eligible = detalles.findByOrderIdOrderById(order).stream().filter(l -> aplica.isEmpty() || aplica.contains(l.getProductId()))
            .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        // REGLA CLIENTES-15: El pedido no contiene productos de la promoción.
        if (eligible.signum() == 0) throw new IllegalArgumentException("El pedido no contiene productos de la promoción.");
        BigDecimal discount = descuento(offer, eligible).min(eligible).min(purchase.getTotal().subtract(new BigDecimal("0.01")));
        // REGLA CLIENTES-16: La promoción no produce un descuento válido.
        if (discount.signum() <= 0) throw new IllegalArgumentException("La promoción no produce un descuento válido.");
        purchase.setTotal(purchase.getTotal().subtract(discount));
        purchase.setDiscount(discount);
        UsoPromocion uso = new UsoPromocion(); uso.setPromotionId(promotion); uso.setCustomerId(purchase.getCustomerId()); uso.setOrderId(order); uso.setDiscount(discount);
        usos.save(uso);
        return Map.of("discount", discount, "message", "Promoción aplicada.");
    }
}
