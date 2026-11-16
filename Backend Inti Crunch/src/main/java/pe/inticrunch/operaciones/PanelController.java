package pe.inticrunch.operaciones;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.inticrunch.caja.repository.DevolucionRepository;
import pe.inticrunch.caja.repository.PagoRepository;
import pe.inticrunch.catalogo.Producto;
import pe.inticrunch.catalogo.ProductoRepository;
import pe.inticrunch.preparacion.repository.ComandaRepository;
import pe.inticrunch.inventario.entity.Insumo;
import pe.inticrunch.inventario.repository.InsumoRepository;
import pe.inticrunch.inventario.repository.SaldoLocalRepository;
import pe.inticrunch.pedidos.NumeroPedido;
import pe.inticrunch.pedidos.entity.DetallePedido;
import pe.inticrunch.pedidos.entity.PedidoCliente;
import pe.inticrunch.pedidos.repository.DetallePedidoRepository;
import pe.inticrunch.pedidos.repository.PedidoClienteRepository;

/** Resumen del día para el panel; el administrador ve además sus ganancias (total y por mes). */
@RestController
@RequestMapping("/admin/dashboard")
public class PanelController {
    private static final Set<String> CERRADOS = Set.of("COMPLETED", "CANCELLED");
    private final PagoRepository pagos;
    private final DevolucionRepository devoluciones;
    private final PedidoClienteRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final ProductoRepository productos;
    private final ComandaRepository comandas;
    private final SaldoLocalRepository saldos;
    private final InsumoRepository insumos;
    private final NumeroPedido numero;

    public PanelController(PagoRepository pagos, DevolucionRepository devoluciones, PedidoClienteRepository pedidos, DetallePedidoRepository detalles, ProductoRepository productos,
                               ComandaRepository comandas, SaldoLocalRepository saldos, InsumoRepository insumos, NumeroPedido numero) {
        this.pagos = pagos; this.devoluciones = devoluciones; this.pedidos = pedidos; this.detalles = detalles; this.productos = productos; this.comandas = comandas;
        this.saldos = saldos; this.insumos = insumos; this.numero = numero;
    }

    @GetMapping @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO','OPERACIONES','ABASTECIMIENTO')")
    public Map<String, Object> resumen(Authentication authentication) {
        Instant hoy = Validaciones.inicioDeHoy(), manana = Validaciones.inicioDeManana();
        List<PedidoCliente> deHoy = pedidos.creadosEntre(hoy, manana); // más recientes primero
        List<PedidoCliente> activos = deHoy.stream().filter(o -> !CERRADOS.contains(o.getStatus())).toList();
        List<PedidoCliente> validos = deHoy.stream().filter(o -> !o.getStatus().equals("CANCELLED")).toList();

        Map<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("ventasHoy", pagos.cobradoEntre(hoy, manana).subtract(devoluciones.devueltoEntre(hoy, manana)));
        metricas.put("pedidosActivos", (long) activos.size());
        metricas.put("ticketPromedio", validos.isEmpty() ? BigDecimal.ZERO
            : validos.stream().map(PedidoCliente::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(validos.size()), 2, RoundingMode.HALF_UP));
        var listas = comandas.listasEntre(hoy, manana);
        metricas.put("minutosPreparacion", listas.stream().mapToDouble(k -> Duration.between(k.getReceivedAt(), k.getReadyAt()).getSeconds() / 60.0).average().orElse(0));
        metricas.put("pedidosHoy", (long) deHoy.size());
        metricas.put("pedidosListos", deHoy.stream().filter(o -> o.getStatus().equals("READY")).count());

        // Últimos 5 pedidos en curso, con lo que llevan
        var ultimos = activos.stream().limit(5).toList();
        Map<Long, String> producto = productos.findAll().stream().collect(Collectors.toMap(Producto::getId, Producto::getName));
        Map<Long, List<DetallePedido>> lineas = ultimos.isEmpty() ? Map.of()
            : detalles.findByOrderIdInOrderById(ultimos.stream().map(PedidoCliente::getId).toList()).stream().collect(Collectors.groupingBy(DetallePedido::getOrderId));
        var numeros = numero.todos();
        List<Map<String, Object>> recientes = ultimos.stream().map(o -> Filas.of("id", o.getId(), "number", numeros.get(o.getId()), "customerName", o.getCustomerName(),
            "channel", o.getChannel(), "status", o.getStatus(), "total", o.getTotal(), "createdAt", o.getCreatedAt(),
            "detalle", lineas.getOrDefault(o.getId(), List.of()).stream().map(l -> l.getQuantity() + " × " + producto.get(l.getProductId())).collect(Collectors.joining(" · ")))).toList();

        Map<String, Long> porCanal = new TreeMap<>(deHoy.stream().collect(Collectors.groupingBy(PedidoCliente::getChannel, Collectors.counting())));
        List<Map<String, Object>> canales = porCanal.entrySet().stream().map(e -> Filas.of("canal", e.getKey(), "pedidos", e.getValue())).toList();

        // Insumos en o por debajo del mínimo (los más críticos primero)
        Map<Long, Insumo> insumo = insumos.findAll().stream().collect(Collectors.toMap(Insumo::getId, i -> i));
        List<Map<String, Object>> bajoStock = saldos.findByLocationId(1L).stream().filter(b -> insumo.containsKey(b.getIngredientId()))
            .filter(b -> b.getQuantity().compareTo(insumo.get(b.getIngredientId()).getMinimumStock()) <= 0)
            .sorted(Comparator.comparing(b -> {
                BigDecimal minimo = insumo.get(b.getIngredientId()).getMinimumStock();
                return minimo.signum() == 0 ? Double.MAX_VALUE : b.getQuantity().doubleValue() / minimo.doubleValue();
            }))
            .limit(5).map(b -> Filas.of("insumo", insumo.get(b.getIngredientId()).getName(), "cantidad", b.getQuantity(), "minimo", insumo.get(b.getIngredientId()).getMinimumStock())).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sucursal", Map.of("nombre", "La Rinconada", "ciudad", "Juliaca, Perú"));
        result.put("metricas", metricas);
        result.put("pedidos", recientes);
        result.put("canales", canales);
        result.put("insumosBajos", bajoStock);
        boolean admin = authentication != null && authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR"));
        if (admin) result.put("ganancias", ganancias());
        return result;
    }

    /** Ganancias = cobros confirmados − devoluciones: total, mes actual y los últimos 12 meses. */
    private Map<String, Object> ganancias() {
        Map<String, Object> ganancias = new LinkedHashMap<>();
        Instant mes = Validaciones.inicioDelMes();
        ganancias.put("total", pagos.totalCobrado().subtract(devoluciones.totalDevuelto()));
        ganancias.put("mensual", pagos.cobradoEntre(mes, Instant.now().plusSeconds(86400)).subtract(devoluciones.devueltoEntre(mes, Instant.now().plusSeconds(86400))));
        YearMonth ahora = YearMonth.now(Validaciones.LIMA);
        Instant desde = Validaciones.inicioDe(ahora.minusMonths(11).atDay(1));
        Map<String, BigDecimal> porMes = new TreeMap<>();
        for (var p : pagos.confirmadosDesde(desde)) porMes.merge(YearMonth.from(p.getCreatedAt().atZone(Validaciones.LIMA)).toString(), p.getAmount(), BigDecimal::add);
        for (var r : devoluciones.findByCreatedAtGreaterThanEqual(desde)) porMes.merge(YearMonth.from(r.getCreatedAt().atZone(Validaciones.LIMA)).toString(), r.getAmount().negate(), BigDecimal::add);
        List<Map<String, Object>> series = new ArrayList<>();
        for (int back = 11; back >= 0; back--) { var ym = ahora.minusMonths(back); series.add(Map.of("mes", ym.toString(), "total", porMes.getOrDefault(ym.toString(), BigDecimal.ZERO))); }
        ganancias.put("porMes", series);
        ganancias.put("pedidosCompletados", pedidos.countByStatus("COMPLETED"));
        return ganancias;
    }
}
