package pe.inticrunch.catalogo;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.catalogo.entity.OpcionProducto;
import pe.inticrunch.catalogo.repository.OpcionProductoRepository;
import pe.inticrunch.preparacion.repository.ZonaRepartoRepository;
import pe.inticrunch.operaciones.Filas;
import pe.inticrunch.operaciones.Validaciones;

/** Opciones de producto (extras), zonas públicas y edición de productos de la carta. */
@Service
public class CatalogoService extends Validaciones {
    private final OpcionProductoRepository opciones;
    private final ProductoRepository productos;
    private final ZonaRepartoRepository zonas;
    public CatalogoService(OpcionProductoRepository opciones, ProductoRepository productos, ZonaRepartoRepository zonas) { this.opciones = opciones; this.productos = productos; this.zonas = zonas; }

    /** Una fila por cada producto que admite la opción. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicOptions() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OpcionProducto o : opciones.activasConProductos())
            for (Long product : o.getProductIds().stream().sorted().toList()) rows.add(Filas.of("id", o.getId(), "name", o.getName(), "price", o.getExtraPrice(), "productId", product));
        return rows;
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicZones() {
        return zonas.findByActiveTrueOrderById().stream().map(z -> Filas.of("id", z.getId(), "name", z.getName(), "coverage", z.getCoverage(), "fee", z.getFee(), "estimatedMinutes", z.getEstimatedMinutes())).toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> allOptions() {
        Map<Long, String> nombres = productos.findAll().stream().collect(Collectors.toMap(Producto::getId, Producto::getName));
        return opciones.activasConProductos().stream().map(o -> Filas.of("ID", o.getId(), "Opción", o.getName(), "Recargo", o.getExtraPrice(),
            "Productos", o.getProductIds().isEmpty() ? null : Filas.join(", ", o.getProductIds().stream().sorted().map(nombres::get).toList()))).toList();
    }
    private void llenar(OpcionProducto option, Map<String, Object> data, String sinProductos) {
        var products = ids(data.get("productIds"));
        if (products.isEmpty()) throw new IllegalArgumentException(sinProductos);
        BigDecimal price = money(data.getOrDefault("extraPrice", 0), "Recargo");
        option.setName(str(data, "name")); option.setExtraPrice(price);
        option.getProductIds().clear(); option.getProductIds().addAll(products);
    }
    @Transactional public Map<String, Object> createOption(Map<String, Object> data) {
        OpcionProducto option = new OpcionProducto();
        llenar(option, data, "Selecciona los productos que admiten la opción.");
        return Map.of("id", opciones.save(option).getId(), "message", "Opción de producto creada.");
    }
    @Transactional public Map<String, Object> updateOption(long id, Map<String, Object> data) {
        OpcionProducto option = opciones.bloquear(id).orElseThrow(Validaciones::noExiste);
        llenar(option, data, "Selecciona al menos un producto para esta opción.");
        return Map.of("message", "Opción actualizada.");
    }
    @Transactional public Map<String, Object> deleteOption(long id) {
        opciones.bloquear(id).orElseThrow(Validaciones::noExiste).setActive(false);
        return Map.of("message", "Opción eliminada de la carta. Los pedidos anteriores la conservan.");
    }
    @Transactional public Map<String, Object> updateProduct(long id, Map<String, Object> data) {
        Producto product = productos.bloquear(id).orElseThrow(Validaciones::noExiste);
        BigDecimal price = money(data.get("price"), "Precio");
        // REGLA CATALOGO-01: El precio debe ser positivo.
        if (price.signum() <= 0) throw new IllegalArgumentException("El precio debe ser positivo.");
        product.actualizar(str(data, "name"), str(data, "description"), price, str(data, "category"), optional(data, "imageUrl"),
            !Boolean.FALSE.equals(data.get("active")), !Boolean.FALSE.equals(data.get("available")));
        return Map.of("message", "Producto actualizado.");
    }
    @Transactional public Map<String, Object> deleteProduct(long id) {
        productos.bloquear(id).orElseThrow(Validaciones::noExiste).retirar();
        return Map.of("message", "Producto retirado de la carta. Su historial de ventas se conserva.");
    }
}
