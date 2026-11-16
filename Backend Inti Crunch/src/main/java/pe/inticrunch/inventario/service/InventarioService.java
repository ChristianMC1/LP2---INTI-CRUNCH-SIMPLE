package pe.inticrunch.inventario.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.catalogo.Producto;
import pe.inticrunch.catalogo.ProductoRepository;
import pe.inticrunch.catalogo.entity.StockPreparado;
import pe.inticrunch.catalogo.repository.StockPreparadoRepository;
import pe.inticrunch.inventario.entity.*;
import pe.inticrunch.inventario.repository.*;
import pe.inticrunch.operaciones.Filas;
import pe.inticrunch.operaciones.Validaciones;
import pe.inticrunch.pedidos.entity.DetallePedido;
import pe.inticrunch.pedidos.repository.DetallePedidoRepository;

@Service
public class InventarioService extends Validaciones {
    private final StockService stock;
    private final InsumoRepository insumos;
    private final AlmacenRepository almacenes;
    private final SaldoAlmacenRepository saldosAlmacen;
    private final SaldoLocalRepository saldosLocal;
    private final LoteRepository lotes;
    private final MovimientoInventarioRepository movimientos;
    private final LineaMovimientoRepository lineasMovimiento;
    private final RecetaRepository recetas;
    private final IngredienteRecetaRepository ingredientesReceta;
    private final ProduccionRepository producciones;
    private final LineaProduccionRepository lineasProduccion;
    private final ConsumoInsumoPedidoRepository consumosInsumo;
    private final ConsumoPreparadoPedidoRepository consumosPreparado;
    private final StockPreparadoRepository preparados;
    private final ProductoRepository productos;
    private final DetallePedidoRepository detalles;

    public InventarioService(StockService stock, InsumoRepository insumos, AlmacenRepository almacenes, SaldoAlmacenRepository saldosAlmacen, SaldoLocalRepository saldosLocal,
                             LoteRepository lotes, MovimientoInventarioRepository movimientos, LineaMovimientoRepository lineasMovimiento, RecetaRepository recetas,
                             IngredienteRecetaRepository ingredientesReceta, ProduccionRepository producciones, LineaProduccionRepository lineasProduccion,
                             ConsumoInsumoPedidoRepository consumosInsumo, ConsumoPreparadoPedidoRepository consumosPreparado, StockPreparadoRepository preparados,
                             ProductoRepository productos, DetallePedidoRepository detalles) {
        this.stock = stock; this.insumos = insumos; this.almacenes = almacenes; this.saldosAlmacen = saldosAlmacen; this.saldosLocal = saldosLocal; this.lotes = lotes;
        this.movimientos = movimientos; this.lineasMovimiento = lineasMovimiento; this.recetas = recetas; this.ingredientesReceta = ingredientesReceta;
        this.producciones = producciones; this.lineasProduccion = lineasProduccion; this.consumosInsumo = consumosInsumo; this.consumosPreparado = consumosPreparado;
        this.preparados = preparados; this.productos = productos; this.detalles = detalles;
    }

    private Map<Long, Insumo> insumosPorId() { return insumos.findAll().stream().collect(Collectors.toMap(Insumo::getId, Function.identity())); }
    private Map<Long, String> nombresAlmacen() { return almacenes.findAll().stream().collect(Collectors.toMap(Almacen::getId, Almacen::getName)); }
    private Map<Long, String> nombresProducto() { return productos.findAll().stream().collect(Collectors.toMap(Producto::getId, Producto::getName)); }
    private static String cantidad(BigDecimal value) { return value == null ? "0" : value.stripTrailingZeros().toPlainString(); }

    // ---------- Insumos ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> ingredients() {
        Map<Long, BigDecimal> hay = saldosLocal.findByLocationId(1L).stream().collect(Collectors.toMap(SaldoLocal::getIngredientId, SaldoLocal::getQuantity, BigDecimal::add));
        Map<Long, LocalDate> vence = new HashMap<>();
        for (Object[] row : lotes.proximoVencimientoPorInsumo()) if (row[1] != null) vence.put((Long) row[0], (LocalDate) row[1]);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Insumo i : insumos.findByActiveTrueOrderById()) {
            BigDecimal cantidad = hay.getOrDefault(i.getId(), BigDecimal.ZERO);
            String estado = cantidad.signum() <= 0 ? "Agotado" : cantidad.compareTo(i.getMinimumStock()) <= 0 ? "Stock bajo" : "Disponible";
            result.add(Filas.of("ID", i.getId(), "Código", i.getSku(), "Insumo", i.getName(), "Categoría", i.getCategory(), "Hay", cantidad, "Unidad", i.getUnit(),
                "Mínimo", i.getMinimumStock(), "Próximo vencimiento", vence.get(i.getId()), "Estado", estado));
        }
        return result;
    }
    @Transactional public Map<String, Object> createIngredient(Map<String, Object> data, String actor) {
        String name = str(data, "name");
        // REGLA INVENTARIO-01: Ya existe un insumo llamado 
        if (insumos.existeActivo(name)) throw new IllegalArgumentException("Ya existe un insumo llamado " + name + ".");
        String unit = str(data, "unit");
        BigDecimal minimo = decimal(data.getOrDefault("minimumStock", 0), "Mínimo");
        String categoria = optional(data, "category").isBlank() ? "General" : str(data, "category");
        // Si el insumo existió antes y se retiró, se reactiva en lugar de duplicarlo.
        Insumo insumo = insumos.buscarPorNombre(name).stream().findFirst().orElse(null);
        if (insumo == null) {
            insumo = new Insumo();
            insumo.setSku(nuevoCodigo(name, nullable(data, "sku")));
            insumo.setVersion(0L);
        }
        insumo.setName(name); insumo.setUnit(unit); insumo.setMinimumStock(minimo); insumo.setCategory(categoria); insumo.setActive(true);
        insumo = insumos.saveAndFlush(insumo);
        long ingredient = insumo.getId();
        if (saldosLocal.findByLocationIdAndIngredientId(1L, ingredient).isEmpty()) {
            SaldoLocal saldo = new SaldoLocal(); saldo.setLocationId(1L); saldo.setIngredientId(ingredient); saldo.setQuantity(BigDecimal.ZERO); saldo.setVersion(0L);
            saldosLocal.save(saldo);
        }
        BigDecimal inicial = optional(data, "initialQuantity").isBlank() ? BigDecimal.ZERO : decimal(data.get("initialQuantity"), "Cantidad inicial");
        if (inicial.signum() > 0) {
            LocalDate expiry = date(data, "expiresOn");
            // REGLA INVENTARIO-02: La fecha de vencimiento no puede estar en el pasado.
            if (expiry != null && expiry.isBefore(hoy())) throw new IllegalArgumentException("La fecha de vencimiento no puede estar en el pasado.");
            long warehouse = optional(data, "warehouseId").isBlank() ? 1 : id(data, "warehouseId");
            MovimientoInventario movimiento = nuevoMovimiento("IN", "INS-" + ingredient, "Cantidad inicial del insumo", actor, warehouse, null);
            stock.add(warehouse, ingredient, inicial, "Ingreso " + fechaCorta(hoy()), expiry);
            nuevaLinea(movimiento.getId(), ingredient, inicial, null);
        }
        return Map.of("id", ingredient, "message", "Insumo " + name + " registrado.");
    }
    private String nuevoCodigo(String name, String sku) {
        if (sku != null) return sku.toUpperCase(Locale.ROOT);
        String prefijo = (name.replaceAll("[^A-Za-zÑñ]", "").toUpperCase(Locale.ROOT) + "XXX").substring(0, 3);
        long next = insumos.count() + 1;
        String code = prefijo + "-" + String.format("%03d", next);
        while (insumos.existsBySku(code)) code = prefijo + "-" + String.format("%03d", ++next);
        return code;
    }
    @Transactional public Map<String, Object> updateIngredient(long ingredient, Map<String, Object> data) {
        Insumo insumo = insumos.bloquear(ingredient).orElseThrow(Validaciones::noExiste);
        insumo.setName(str(data, "name")); insumo.setUnit(str(data, "unit"));
        insumo.setMinimumStock(decimal(data.getOrDefault("minimumStock", 0), "Mínimo"));
        insumo.setCategory(optional(data, "category").isBlank() ? "General" : str(data, "category"));
        return Map.of("message", "Insumo actualizado.");
    }
    @Transactional public Map<String, Object> deleteIngredient(long ingredient) {
        Insumo insumo = insumos.bloquear(ingredient).orElseThrow(Validaciones::noExiste);
        var nombres = nombresProducto();
        var usados = recetas.productosQueUsan(ingredient).stream().map(id -> nombres.getOrDefault(id, "Producto")).toList();
        if (!usados.isEmpty()) throw new IllegalStateException(insumo.getName() + " se usa en la receta de: " + String.join(", ", usados) + ". Quítalo de esas recetas primero.");
        insumo.setActive(false);
        return Map.of("message", "Insumo " + insumo.getName() + " retirado. Su historial se conserva.");
    }
    /** Conteo físico: se escribe lo que realmente hay y el sistema hace el ajuste. */
    @Transactional public Map<String, Object> countIngredient(long ingredient, Map<String, Object> data, String actor) {
        long warehouse = optional(data, "warehouseId").isBlank() ? 1 : id(data, "warehouseId");
        insumos.findByIdAndActiveTrue(ingredient).orElseThrow(Validaciones::noExiste);
        BigDecimal contado = decimal(data.get("quantity"), "Cantidad contada");
        BigDecimal antes = saldosAlmacen.findByWarehouseIdAndIngredientId(warehouse, ingredient).map(SaldoAlmacen::getQuantity).orElse(BigDecimal.ZERO);
        BigDecimal diferencia = contado.subtract(antes);
        if (diferencia.signum() == 0) return Map.of("message", "El conteo coincide; no hubo cambios.");
        MovimientoInventario movimiento = nuevoMovimiento(diferencia.signum() > 0 ? "ADJUST_IN" : "ADJUST_OUT", "CONTEO", optional(data, "note").isBlank() ? "Conteo físico" : str(data, "note"), actor, warehouse, null);
        if (diferencia.signum() > 0) stock.add(warehouse, ingredient, diferencia, "Conteo " + fechaCorta(hoy()), date(data, "expiresOn"));
        else stock.remove(warehouse, ingredient, diferencia.negate(), true);
        nuevaLinea(movimiento.getId(), ingredient, diferencia.abs(), null);
        return Map.of("message", "Conteo guardado: ahora hay " + cantidad(contado) + ".");
    }

    // ---------- Almacenes ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> warehouses() {
        return almacenes.findByActiveTrueOrderById().stream()
            .map(a -> Filas.of("ID", a.getId(), "Almacén", a.getName(), "Insumos guardados", saldosAlmacen.countByWarehouseIdAndQuantityGreaterThan(a.getId(), BigDecimal.ZERO)))
            .toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> balances() {
        var insumo = insumosPorId(); var almacen = nombresAlmacen();
        return saldosAlmacen.findByQuantityGreaterThan(BigDecimal.ZERO).stream()
            .sorted(Comparator.comparing((SaldoAlmacen s) -> s.getWarehouseId()).thenComparing(s -> insumo.get(s.getIngredientId()).getName()))
            .map(s -> Filas.of("Almacén", almacen.get(s.getWarehouseId()), "Insumo", insumo.get(s.getIngredientId()).getName(), "Cantidad", s.getQuantity(), "Unidad", insumo.get(s.getIngredientId()).getUnit()))
            .toList();
    }
    @Transactional public Map<String, Object> createWarehouse(Map<String, Object> data) {
        String name = str(data, "name");
        // REGLA INVENTARIO-03: Ya existe un almacén llamado 
        if (almacenes.existeActivo(name)) throw new IllegalArgumentException("Ya existe un almacén llamado " + name + ".");
        var anterior = almacenes.buscarPorNombre(name).stream().findFirst();
        if (anterior.isPresent()) { Almacen a = anterior.get(); a.setName(name); a.setActive(true); return Map.of("id", a.getId(), "message", "Almacén " + name + " reactivado."); }
        Almacen almacen = new Almacen(); almacen.setLocationId(1L); almacen.setName(name); almacen.setActive(true);
        return Map.of("id", almacenes.save(almacen).getId(), "message", "Almacén creado.");
    }
    @Transactional public Map<String, Object> updateWarehouse(long warehouse, Map<String, Object> data) {
        almacenes.bloquear(warehouse).orElseThrow(Validaciones::noExiste).setName(str(data, "name"));
        return Map.of("message", "Almacén actualizado.");
    }
    @Transactional public Map<String, Object> deleteWarehouse(long warehouse) {
        Almacen almacen = almacenes.bloquear(warehouse).orElseThrow(Validaciones::noExiste);
        // REGLA INVENTARIO-04: El almacén principal no se puede quitar: la cocina descuenta de él.
        if (warehouse == 1) throw new IllegalStateException("El almacén principal no se puede quitar: la cocina descuenta de él.");
        // REGLA INVENTARIO-05: El almacén todavía guarda insumos. Muévelos a otro almacén antes de quitarlo.
        if (saldosAlmacen.existsByWarehouseIdAndQuantityGreaterThan(warehouse, BigDecimal.ZERO)) throw new IllegalStateException("El almacén todavía guarda insumos. Muévelos a otro almacén antes de quitarlo.");
        almacen.setActive(false);
        return Map.of("message", "Almacén retirado.");
    }

    // ---------- Vencimientos (lotes) ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> lots() {
        var insumo = insumosPorId(); var almacen = nombresAlmacen(); LocalDate today = hoy();
        return lotes.conExistencia().stream().map(l -> {
            Long dias = l.getExpiresOn() == null ? null : ChronoUnit.DAYS.between(today, l.getExpiresOn());
            String estado = l.getExpiresOn() == null ? "Sin fecha" : dias < 0 ? "Vencido" : dias <= 3 ? "Por vencer" : "Disponible";
            Insumo i = insumo.get(l.getIngredientId());
            return Filas.of("ID", l.getId(), "Insumo", i.getName(), "Cantidad", l.getQuantity(), "Unidad", i.getUnit(), "Almacén", almacen.get(l.getWarehouseId()),
                "Ingreso", l.getBatchCode(), "Vence", l.getExpiresOn(), "Días que faltan", dias, "Estado", estado);
        }).toList();
    }
    @Transactional public Map<String, Object> updateLot(long lot, Map<String, Object> data) {
        lotes.bloquear(lot).orElseThrow(Validaciones::noExiste).setExpiresOn(date(data, "expiresOn"));
        return Map.of("message", "Fecha de vencimiento actualizada.");
    }
    @Transactional public Map<String, Object> discardLot(long lot, String actor) {
        Lote lote = lotes.bloquear(lot).filter(l -> l.getQuantity().signum() > 0).orElseThrow(Validaciones::noExiste);
        BigDecimal cantidad = lote.getQuantity();
        MovimientoInventario movimiento = nuevoMovimiento("WASTE", "LOTE-" + lot, "Desechado por vencimiento o mal estado", actor, lote.getWarehouseId(), null);
        stock.discardLot(lot, lote.getWarehouseId(), lote.getIngredientId(), cantidad);
        nuevaLinea(movimiento.getId(), lote.getIngredientId(), cantidad, null);
        return Map.of("message", "Se desechó lo que quedaba de ese ingreso.");
    }

    // ---------- Movimientos ----------
    private MovimientoInventario nuevoMovimiento(String kind, String reference, String note, String actor, Long warehouse, Long target) {
        MovimientoInventario m = new MovimientoInventario();
        m.setLocationId(1L); m.setKind(kind); m.setReference(reference); m.setNote(note); m.setCreatedBy(actor); m.setWarehouseId(warehouse); m.setTargetWarehouseId(target);
        return movimientos.saveAndFlush(m);
    }
    private void nuevaLinea(long movement, long ingredient, BigDecimal quantity, BigDecimal unitCost) {
        LineaMovimiento linea = new LineaMovimiento();
        linea.setMovementId(movement); linea.setIngredientId(ingredient); linea.setQuantity(quantity); linea.setUnitCost(unitCost);
        lineasMovimiento.save(linea);
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> inventoryMovements() {
        var insumo = insumosPorId(); var almacen = nombresAlmacen();
        var lista = movimientos.recientes(PageRequest.of(0, 300));
        Map<Long, List<LineaMovimiento>> lineas = lineasMovimiento.findByMovementIdInOrderById(lista.stream().map(MovimientoInventario::getId).toList()).stream().collect(Collectors.groupingBy(LineaMovimiento::getMovementId));
        return lista.stream().map(m -> {
            String que = lineas.getOrDefault(m.getId(), List.of()).stream().map(l -> cantidad(l.getQuantity()) + " " + insumo.get(l.getIngredientId()).getUnit() + " " + insumo.get(l.getIngredientId()).getName()).collect(Collectors.joining(", "));
            return Filas.of("Movimiento", m.getId(), "Tipo", m.getKind(), "Qué se movió", que, "Almacén", almacen.get(m.getWarehouseId()), "Referencia", m.getReference(), "Nota", m.getNote(), "Responsable", m.getCreatedBy(), "Fecha", m.getCreatedAt());
        }).toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> movementLines(long movement) {
        var insumo = insumosPorId();
        return lineasMovimiento.findByMovementIdOrderById(movement).stream().map(l -> Filas.of("Insumo", insumo.get(l.getIngredientId()).getName(), "Cantidad", l.getQuantity(), "Costo", l.getUnitCost())).toList();
    }
    @Transactional public Map<String, Object> createMovement(Map<String, Object> data, String actor) {
        String kind = choice(data, "kind", "IN", "OUT", "WASTE", "TRANSFER", "ADJUST_IN", "ADJUST_OUT");
        long warehouse = data.containsKey("warehouseId") ? id(data, "warehouseId") : 1;
        Long target = kind.equals("TRANSFER") ? id(data, "targetWarehouseId") : null;
        String note = str(data, "note");
        var detalle = lines(data, "lines").stream().sorted(Comparator.comparing(line -> id(line, "ingredientId"))).toList();
        MovimientoInventario movimiento = nuevoMovimiento(kind, optional(data, "reference"), note, actor, warehouse, target);
        for (var line : detalle) {
            long ingredient = id(line, "ingredientId"); BigDecimal quantity = positive(line.get("quantity"), "Cantidad");
            if (kind.equals("TRANSFER")) stock.transfer(warehouse, target, ingredient, quantity);
            else if (Set.of("IN", "ADJUST_IN").contains(kind)) {
                LocalDate expiry = date(line, "expiresOn");
                // REGLA INVENTARIO-06: No se puede ingresar un lote vencido.
                if (expiry != null && expiry.isBefore(hoy())) throw new IllegalArgumentException("No se puede ingresar un lote vencido.");
                stock.add(warehouse, ingredient, quantity, optional(line, "batchCode").isBlank() ? "Ingreso " + fechaCorta(hoy()) : optional(line, "batchCode"), expiry);
            } else stock.remove(warehouse, ingredient, quantity, kind.equals("WASTE") || kind.equals("ADJUST_OUT"));
            nuevaLinea(movimiento.getId(), ingredient, quantity, line.get("unitCost") == null ? null : money(line.get("unitCost"), "Costo"));
        }
        return Map.of("id", movimiento.getId(), "message", "Movimiento registrado; existencias actualizadas.");
    }

    // ---------- Recetas ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> recipes() {
        var producto = nombresProducto(); var insumo = insumosPorId();
        var lista = recetas.findByActiveTrueOrderById();
        Map<Long, List<IngredienteReceta>> partes = ingredientesReceta.findByRecipeIdIn(lista.stream().map(Receta::getId).toList()).stream().collect(Collectors.groupingBy(IngredienteReceta::getRecipeId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Receta r : lista) {
            int listas = preparados.findById(r.getProductId()).map(StockPreparado::getQuantity).orElse(0);
            String texto = partes.getOrDefault(r.getId(), List.of()).stream()
                .sorted(Comparator.comparing(p -> insumo.get(p.getIngredientId()).getName()))
                .map(p -> p.getQuantity().divide(BigDecimal.valueOf(r.getServings()), 3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " " + insumo.get(p.getIngredientId()).getUnit() + " de " + insumo.get(p.getIngredientId()).getName())
                .collect(Collectors.joining(" · "));
            result.add(Filas.of("ID", r.getId(), "Producto", producto.get(r.getProductId()), "_productId", r.getProductId(), "Rinde (porciones)", r.getServings(), "Listas para vender", listas, "Ingredientes por porción", texto));
        }
        return result;
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> recipeLines(long recipe) {
        var insumo = insumosPorId();
        return ingredientesReceta.findByRecipeIdOrderByIngredientId(recipe).stream()
            .sorted(Comparator.comparing(p -> insumo.get(p.getIngredientId()).getName()))
            .map(p -> Filas.of("_ingredientId", p.getIngredientId(), "Insumo", insumo.get(p.getIngredientId()).getName(), "Cantidad", p.getQuantity(), "Unidad", insumo.get(p.getIngredientId()).getUnit()))
            .toList();
    }
    @Transactional public Map<String, Object> deleteRecipe(long recipe) {
        recetas.bloquear(recipe).orElseThrow(Validaciones::noExiste).setActive(false);
        return Map.of("message", "Receta retirada. La cocina no podrá preparar ese producto hasta que registres una nueva receta.");
    }
    /** Guarda una nueva versión de la receta y desactiva la anterior. */
    @Transactional public Map<String, Object> createRecipe(Map<String, Object> data) {
        long product = id(data, "productId"); int servings = integer(data.getOrDefault("servings", 1), "Porciones");
        productos.findById(product).filter(Producto::isActive).orElseThrow(Validaciones::noExiste);
        var detalle = lines(data, "lines");
        int version = recetas.siguienteVersion(product);
        recetas.findByProductId(product).forEach(r -> r.setActive(false));
        Receta receta = new Receta(); receta.setProductId(product); receta.setVersion(version); receta.setServings(servings); receta.setActive(true);
        receta = recetas.saveAndFlush(receta);
        for (var line : detalle) {
            long ingredient = id(line, "ingredientId");
            insumos.findByIdAndActiveTrue(ingredient).orElseThrow(Validaciones::noExiste);
            IngredienteReceta parte = new IngredienteReceta();
            parte.setRecipeId(receta.getId()); parte.setIngredientId(ingredient); parte.setQuantity(positive(line.get("quantity"), "Cantidad"));
            ingredientesReceta.save(parte);
        }
        return Map.of("id", receta.getId(), "message", "Receta guardada. Desde ahora la cocina usará estas cantidades.");
    }

    // ---------- Producción ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> productionRuns() {
        var producto = nombresProducto();
        Map<Long, Long> productoDeReceta = recetas.findAll().stream().collect(Collectors.toMap(Receta::getId, Receta::getProductId));
        return producciones.recientes(PageRequest.of(0, 300)).stream().map(p -> Filas.of("ID", p.getId(), "Producto", producto.get(productoDeReceta.get(p.getRecipeId())), "Producidas", p.getProducedServings(),
            "Estado", p.getStatus(), "Responsable", p.getCreatedBy(), "Fecha", p.getCreatedAt(), "Nota", p.getNote())).toList();
    }
    @Transactional public Map<String, Object> createProduction(Map<String, Object> data, String actor) {
        long recipeId = id(data, "recipeId"); int servings = integer(data.get("servings"), "Porciones"); long warehouse = data.containsKey("warehouseId") ? id(data, "warehouseId") : 1;
        Receta receta = recetas.bloquear(recipeId).filter(r -> Boolean.TRUE.equals(r.getActive())).orElseThrow(Validaciones::noExiste);
        var componentes = ingredientesReceta.findByRecipeIdOrderByIngredientId(recipeId);
        // REGLA INVENTARIO-07: La receta no tiene insumos.
        if (componentes.isEmpty()) throw new IllegalStateException("La receta no tiene insumos.");
        Produccion produccion = new Produccion();
        produccion.setLocationId(1L); produccion.setRecipeId(recipeId); produccion.setPlannedServings(servings); produccion.setProducedServings(servings); produccion.setNote(optional(data, "note")); produccion.setCreatedBy(actor);
        produccion = producciones.saveAndFlush(produccion);
        MovimientoInventario movimiento = nuevoMovimiento("OUT", "PROD-" + produccion.getId(), "Consumo de producción", actor, warehouse, null);
        for (IngredienteReceta componente : componentes) {
            BigDecimal requerido = componente.getQuantity().multiply(BigDecimal.valueOf(servings)).divide(BigDecimal.valueOf(receta.getServings()), 3, RoundingMode.UP);
            stock.remove(warehouse, componente.getIngredientId(), requerido, false);
            LineaProduccion linea = new LineaProduccion();
            linea.setProductionRunId(produccion.getId()); linea.setIngredientId(componente.getIngredientId()); linea.setConsumedQuantity(requerido);
            lineasProduccion.save(linea);
            nuevaLinea(movimiento.getId(), componente.getIngredientId(), requerido, null);
        }
        StockPreparado listo = preparadoDe(receta.getProductId());
        listo.setQuantity(listo.getQuantity() + servings);
        return Map.of("id", produccion.getId(), "message", "Producción terminada. Las ventas usarán primero las porciones preparadas.");
    }
    private StockPreparado preparadoDe(long product) {
        return preparados.bloquear(product).orElseGet(() -> {
            StockPreparado nuevo = new StockPreparado(); nuevo.setProductId(product); nuevo.setQuantity(0);
            return preparados.saveAndFlush(nuevo);
        });
    }

    /** Al empezar a preparar un pedido: usa primero porciones listas y luego descuenta la receta. */
    @Transactional public void consumeOrder(long order, String actor) {
        MovimientoInventario movimiento = null;
        for (DetallePedido linea : detalles.findByOrderIdOrderByProductIdAscIdAsc(order)) {
            long lineId = linea.getId(), product = linea.getProductId(); int pendiente = linea.getQuantity();
            if (consumosInsumo.existsByOrderLineId(lineId) || consumosPreparado.existsById(lineId)) continue;
            StockPreparado listo = preparadoDe(product);
            int usadas = Math.min(listo.getQuantity(), pendiente);
            if (usadas > 0) {
                listo.setQuantity(listo.getQuantity() - usadas);
                ConsumoPreparadoPedido consumo = new ConsumoPreparadoPedido(); consumo.setOrderLineId(lineId); consumo.setQuantity(usadas);
                consumosPreparado.save(consumo);
                pendiente -= usadas;
            }
            if (pendiente == 0) continue;
            var activas = recetas.findByProductIdAndActiveTrue(product);
            // REGLA INVENTARIO-08: Configura una receta para el producto #
            if (activas.isEmpty()) throw new IllegalStateException("Configura una receta para el producto #" + product + " antes de preparar el pedido.");
            Receta receta = activas.getFirst();
            var componentes = ingredientesReceta.findByRecipeIdOrderByIngredientId(receta.getId());
            // REGLA INVENTARIO-09: La receta no tiene ingredientes.
            if (componentes.isEmpty()) throw new IllegalStateException("La receta no tiene ingredientes.");
            if (movimiento == null) movimiento = nuevoMovimiento("OUT", "PED-" + order, "Consumo por pedido", actor, 1L, null);
            for (IngredienteReceta componente : componentes) {
                BigDecimal requerido = componente.getQuantity().multiply(BigDecimal.valueOf(pendiente)).divide(BigDecimal.valueOf(receta.getServings()), 3, RoundingMode.UP);
                stock.remove(1, componente.getIngredientId(), requerido, false);
                ConsumoInsumoPedido consumo = new ConsumoInsumoPedido(); consumo.setOrderLineId(lineId); consumo.setIngredientId(componente.getIngredientId()); consumo.setConsumedQuantity(requerido);
                consumosInsumo.save(consumo);
                nuevaLinea(movimiento.getId(), componente.getIngredientId(), requerido, null);
            }
        }
    }
}
