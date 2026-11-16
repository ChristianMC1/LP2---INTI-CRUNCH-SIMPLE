package pe.inticrunch.operaciones;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.caja.repository.AsignacionPagoRepository;
import pe.inticrunch.caja.repository.DevolucionRepository;
import pe.inticrunch.caja.repository.SesionCajaRepository;
import pe.inticrunch.catalogo.Producto;
import pe.inticrunch.catalogo.ProductoRepository;
import pe.inticrunch.clientes.repository.ClienteRepository;
import pe.inticrunch.clientes.repository.PromocionRepository;
import pe.inticrunch.preparacion.repository.DespachoRepository;
import pe.inticrunch.preparacion.repository.ZonaRepartoRepository;
import pe.inticrunch.inventario.repository.AlmacenRepository;
import pe.inticrunch.inventario.repository.InsumoRepository;
import pe.inticrunch.inventario.repository.RecetaRepository;
import pe.inticrunch.operaciones.repository.LocalRepository;
import pe.inticrunch.pedidos.NumeroPedido;
import pe.inticrunch.pedidos.repository.PedidoClienteRepository;
import pe.inticrunch.compras.entity.Proveedor;
import pe.inticrunch.compras.repository.OrdenCompraRepository;
import pe.inticrunch.compras.repository.ProveedorRepository;
import pe.inticrunch.compras.repository.SolicitudCompraRepository;
import pe.inticrunch.reservas.repository.MesaRepository;
import pe.inticrunch.personal.entity.Colaborador;
import pe.inticrunch.personal.repository.ColaboradorRepository;
import pe.inticrunch.personal.repository.TurnoRepository;

/** Listas para los selectores del panel (productos, mesas, cajeros…), según el rol de la persona. */
@RestController
@RequestMapping("/admin/lookups")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','CAJERO','OPERACIONES','ABASTECIMIENTO','REPARTIDOR')")
public class ConsultasController {
    private final ProductoRepository productos; private final ZonaRepartoRepository zonas; private final LocalRepository locales;
    private final InsumoRepository insumos; private final AlmacenRepository almacenes; private final RecetaRepository recetas;
    private final PedidoClienteRepository pedidos; private final AsignacionPagoRepository asignaciones; private final DevolucionRepository devoluciones;
    private final DespachoRepository despachos; private final ColaboradorRepository colaboradores; private final TurnoRepository turnos;
    private final SesionCajaRepository cajas; private final ClienteRepository clientes; private final PromocionRepository promociones;
    private final ProveedorRepository proveedores; private final SolicitudCompraRepository solicitudes; private final OrdenCompraRepository ordenes;
    private final MesaRepository mesas; private final NumeroPedido numero;

    public ConsultasController(ProductoRepository productos, ZonaRepartoRepository zonas, LocalRepository locales, InsumoRepository insumos, AlmacenRepository almacenes,
                            RecetaRepository recetas, PedidoClienteRepository pedidos, AsignacionPagoRepository asignaciones, DevolucionRepository devoluciones,
                            DespachoRepository despachos, ColaboradorRepository colaboradores, TurnoRepository turnos, SesionCajaRepository cajas, ClienteRepository clientes,
                            PromocionRepository promociones, ProveedorRepository proveedores, SolicitudCompraRepository solicitudes, OrdenCompraRepository ordenes,
                            MesaRepository mesas, NumeroPedido numero) {
        this.productos = productos; this.zonas = zonas; this.locales = locales; this.insumos = insumos; this.almacenes = almacenes; this.recetas = recetas; this.pedidos = pedidos;
        this.asignaciones = asignaciones; this.devoluciones = devoluciones; this.despachos = despachos; this.colaboradores = colaboradores; this.turnos = turnos; this.cajas = cajas;
        this.clientes = clientes; this.promociones = promociones; this.proveedores = proveedores; this.solicitudes = solicitudes; this.ordenes = ordenes; this.mesas = mesas; this.numero = numero;
    }

    private static Map<String, Object> opcion(Object id, String label, Object... extra) {
        Object[] kv = new Object[4 + extra.length];
        kv[0] = "id"; kv[1] = id; kv[2] = "label"; kv[3] = label;
        System.arraycopy(extra, 0, kv, 4, extra.length);
        return Filas.of(kv);
    }
    private static Map<Long, BigDecimal> porId(List<Object[]> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) map.put((Long) row[0], (BigDecimal) row[1]);
        return map;
    }

    @GetMapping @Transactional(readOnly = true)
    public Map<String, Object> choices(Authentication auth) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("products", productos.findByActivoTrueAndDisponibleTrueOrderByCategoriaAscNombreAsc().stream().sorted(Comparator.comparing(Producto::getName))
            .map(p -> opcion(p.getId(), p.getName(), "price", p.getPrice())).toList());
        result.put("zones", zonas.findByActiveTrueOrderByName().stream().map(z -> opcion(z.getId(), z.getName(), "fee", z.getFee())).toList());
        result.put("locations", locales.findByActiveTrueOrderByName().stream().map(l -> opcion(l.getId(), l.getName())).toList());
        if (has(auth, "ADMINISTRADOR", "SUPERVISOR", "OPERACIONES", "ABASTECIMIENTO")) {
            result.put("ingredients", insumos.findByActiveTrueOrderByName().stream().map(i -> opcion(i.getId(), i.getName() + " (" + i.getUnit() + ")")).toList());
            result.put("warehouses", almacenes.findByActiveTrueOrderById().stream().sorted(Comparator.comparing(a -> a.getName())).map(a -> opcion(a.getId(), a.getName())).toList());
            Map<Long, String> producto = productos.findAll().stream().collect(Collectors.toMap(Producto::getId, Producto::getName));
            result.put("recipes", recetas.findByActiveTrueOrderById().stream().filter(r -> producto.containsKey(r.getProductId()))
                .sorted(Comparator.comparing(r -> producto.get(r.getProductId()))).map(r -> opcion(r.getId(), producto.get(r.getProductId()) + " · v" + r.getVersion())).toList());
        }
        if (has(auth, "ADMINISTRADOR", "SUPERVISOR", "CAJERO", "OPERACIONES")) {
            var numeros = numero.todos();
            var pagado = porId(asignaciones.pagadoPorPedido());
            var devuelto = porId(devoluciones.devueltoPorPedido());
            result.put("orders", pedidos.noCancelados(org.springframework.data.domain.PageRequest.of(0, 300)).stream().map(o -> opcion(o.getId(), "N° " + numeros.get(o.getId()) + " · " + o.getCustomerName(),
                "status", o.getStatus(), "channel", o.getChannel(), "total", o.getTotal(),
                "paid", pagado.getOrDefault(o.getId(), BigDecimal.ZERO).subtract(devuelto.getOrDefault(o.getId(), BigDecimal.ZERO)))).toList());
            result.put("dispatches", despachos.pendientes().stream().map(d -> opcion(d.getId(), "Entrega del pedido N° " + numeros.get(d.getOrderId()) + " · " + (d.getAddress() == null ? "recojo" : d.getAddress()))).toList());
        }
        if (has(auth, "ADMINISTRADOR", "SUPERVISOR", "CAJERO")) {
            result.put("cashiers", colaboradores.activosConRol(List.of("CAJERO", "ADMINISTRADOR", "SUPERVISOR")).stream().map(e -> opcion(e.getId(), e.getFullName())).toList());
            result.put("shifts", turnos.vigentes().stream().map(s -> opcion(s.getId(), s.getName())).toList());
            result.put("sessions", cajas.abiertas().stream().map(s -> opcion(s.getId(), "Caja #" + s.getId() + " · " + s.getOpenedBy())).toList());
            result.put("customers", clientes.porNombre().stream().map(c -> opcion(c.getId(), c.getFullName())).toList());
            result.put("promotions", promociones.vigentes(java.time.Instant.now()).stream().map(p -> opcion(p.getId(), p.getName() + (p.getCode() == null ? "" : " · " + p.getCode()), "code", p.getCode())).toList());
        }
        if (has(auth, "ADMINISTRADOR", "SUPERVISOR", "ABASTECIMIENTO")) {
            result.put("suppliers", proveedores.findByActiveTrueOrderByName().stream().map(s -> opcion(s.getId(), s.getName())).toList());
            result.put("requests", solicitudes.findByStatusOrderByIdDesc("PENDING").stream().map(r -> opcion(r.getId(), "Solicitud #" + r.getId() + " · " + r.getNote())).toList());
            Map<Long, String> proveedor = proveedores.findAll().stream().collect(Collectors.toMap(Proveedor::getId, Proveedor::getName));
            result.put("purchaseOrders", ordenes.noCanceladas().stream().map(p -> opcion(p.getId(), "OC #" + p.getId() + " · " + proveedor.get(p.getSupplierId()))).toList());
        }
        if (has(auth, "ADMINISTRADOR", "SUPERVISOR", "OPERACIONES")) {
            result.put("couriers", colaboradores.disponiblesConRol(List.of("OPERACIONES", "REPARTIDOR")).stream().map(e -> opcion(e.getId(), e.getFullName())).toList());
            result.put("couriersAll", colaboradores.activosConRol(List.of("OPERACIONES", "REPARTIDOR")).stream().map(e -> opcion(e.getId(), e.getFullName())).toList());
        }
        if (has(auth, "ADMINISTRADOR", "SUPERVISOR", "CAJERO", "OPERACIONES"))
            result.put("tables", mesas.findByActiveTrueOrderById().stream().map(m -> opcion(m.getId(), m.getTableCode() + " · " + m.getCapacity() + " personas", "capacity", m.getCapacity())).toList());
        if (has(auth, "ADMINISTRADOR", "SUPERVISOR"))
            result.put("employees", colaboradores.activos().stream().filter(e -> !e.getRoles().isEmpty())
                .map((Colaborador e) -> opcion(e.getId(), e.getFullName(), "roles", String.join(",", new TreeSet<>(e.getRoles())))).toList());
        return result;
    }
    private boolean has(Authentication auth, String... roles) {
        return Arrays.stream(roles).anyMatch(role -> auth.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role)));
    }
}
