package pe.inticrunch.inventario.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.inticrunch.inventario.entity.*;
import pe.inticrunch.inventario.repository.*;
import pe.inticrunch.operaciones.Validaciones;

/** Existencias por almacén y por ingreso (lote), siempre usando primero lo que vence antes. */
@Service
public class StockService extends Validaciones {
    private final AlmacenRepository almacenes;
    private final InsumoRepository insumos;
    private final SaldoAlmacenRepository saldosAlmacen;
    private final SaldoLocalRepository saldosLocal;
    private final LoteRepository lotes;
    public StockService(AlmacenRepository almacenes, InsumoRepository insumos, SaldoAlmacenRepository saldosAlmacen, SaldoLocalRepository saldosLocal, LoteRepository lotes) {
        this.almacenes = almacenes; this.insumos = insumos; this.saldosAlmacen = saldosAlmacen; this.saldosLocal = saldosLocal; this.lotes = lotes;
    }

    /** Bloquea el insumo y el saldo del almacén para que dos operaciones no lo cambien a la vez. */
    private SaldoAlmacen bloquear(long warehouse, long ingredient) {
        almacenes.findByIdAndActiveTrue(warehouse).orElseThrow(Validaciones::noExiste);
        Insumo insumo = insumos.bloquear(ingredient).orElseThrow(Validaciones::noExiste);
        if (!Boolean.TRUE.equals(insumo.getActive())) throw noExiste();
        return saldosAlmacen.bloquear(warehouse, ingredient).orElseGet(() -> {
            SaldoAlmacen nuevo = new SaldoAlmacen();
            nuevo.setWarehouseId(warehouse); nuevo.setIngredientId(ingredient); nuevo.setQuantity(BigDecimal.ZERO);
            return saldosAlmacen.saveAndFlush(nuevo);
        });
    }

    @Transactional public void add(long warehouse, long ingredient, BigDecimal quantity, String batch, LocalDate expiry) {
        SaldoAlmacen saldo = bloquear(warehouse, ingredient);
        // REGLA INVENTARIO-10: La cantidad debe ser positiva.
        if (quantity.signum() <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        String code = batch == null || batch.isBlank() ? "SIN-LOTE" : batch;
        var existente = lotes.findByWarehouseIdAndIngredientIdAndBatchCode(warehouse, ingredient, code);
        if (existente.isPresent() && !Objects.equals(existente.get().getExpiresOn(), expiry)) {
            // Mismo día pero otra fecha de vencimiento: se guarda como un ingreso aparte.
            code = code + " · vence " + (expiry == null ? "sin fecha" : fechaCorta(expiry));
            existente = lotes.findByWarehouseIdAndIngredientIdAndBatchCode(warehouse, ingredient, code);
            // REGLA INVENTARIO-11: Ya hay un ingreso con ese nombre y otra fecha de vencimiento.
            if (existente.isPresent() && !Objects.equals(existente.get().getExpiresOn(), expiry)) throw new IllegalArgumentException("Ya hay un ingreso con ese nombre y otra fecha de vencimiento.");
        }
        Lote lote = existente.orElseGet(Lote::new);
        if (lote.getId() == null) { lote.setWarehouseId(warehouse); lote.setIngredientId(ingredient); lote.setBatchCode(code); lote.setExpiresOn(expiry); lote.setQuantity(BigDecimal.ZERO); }
        lote.setQuantity(lote.getQuantity().add(quantity));
        lotes.save(lote);
        saldo.setQuantity(saldo.getQuantity().add(quantity));
        sincronizar(ingredient);
    }

    @Transactional public void remove(long warehouse, long ingredient, BigDecimal quantity, boolean allowExpired) {
        SaldoAlmacen saldo = bloquear(warehouse, ingredient);
        List<Lote> usables = lotes.utilizables(warehouse, ingredient, allowExpired, hoy());
        BigDecimal disponible = usables.stream().map(Lote::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        // REGLA INVENTARIO-12: Stock utilizable insuficiente para 
        if (disponible.compareTo(quantity) < 0) throw new IllegalStateException("Stock utilizable insuficiente para " + insumos.findById(ingredient).map(Insumo::getName).orElse("el insumo") + " en el almacén seleccionado.");
        BigDecimal pendiente = quantity;
        for (Lote lote : usables) {
            if (pendiente.signum() == 0) break;
            BigDecimal usado = pendiente.min(lote.getQuantity());
            lote.setQuantity(lote.getQuantity().subtract(usado));
            pendiente = pendiente.subtract(usado);
        }
        saldo.setQuantity(saldo.getQuantity().subtract(quantity));
        sincronizar(ingredient);
    }

    @Transactional public void transfer(long source, long target, long ingredient, BigDecimal quantity) {
        // REGLA INVENTARIO-13: Selecciona dos almacenes distintos.
        if (source == target) throw new IllegalArgumentException("Selecciona dos almacenes distintos.");
        bloquear(Math.min(source, target), ingredient); bloquear(Math.max(source, target), ingredient);
        // Copia de los ingresos antes de descontar, para pasar cada parte con su misma fecha de vencimiento.
        record Parte(String code, LocalDate expiry, BigDecimal quantity) {}
        List<Parte> partes = lotes.utilizables(source, ingredient, false, hoy()).stream().map(l -> new Parte(l.getBatchCode(), l.getExpiresOn(), l.getQuantity())).toList();
        remove(source, ingredient, quantity, false);
        BigDecimal pendiente = quantity;
        for (Parte parte : partes) {
            if (pendiente.signum() == 0) break;
            BigDecimal movido = pendiente.min(parte.quantity());
            add(target, ingredient, movido, parte.code(), parte.expiry());
            pendiente = pendiente.subtract(movido);
        }
    }

    @Transactional public void discardLot(long lot, long warehouse, long ingredient, BigDecimal quantity) {
        SaldoAlmacen saldo = bloquear(warehouse, ingredient);
        Lote lote = lotes.bloquear(lot).orElseThrow(Validaciones::noExiste);
        lote.setQuantity(BigDecimal.ZERO);
        saldo.setQuantity(saldo.getQuantity().compareTo(quantity) > 0 ? saldo.getQuantity().subtract(quantity) : BigDecimal.ZERO);
        sincronizar(ingredient);
    }

    /** Recalcula la existencia total del insumo en el local sumando todos sus almacenes. */
    private void sincronizar(long ingredient) {
        saldosAlmacen.flush();
        Map<Long, Long> localDeAlmacen = new HashMap<>();
        almacenes.findAll().forEach(a -> localDeAlmacen.put(a.getId(), a.getLocationId()));
        Map<Long, BigDecimal> porLocal = new HashMap<>();
        for (SaldoAlmacen saldo : saldosAlmacen.findByIngredientId(ingredient))
            porLocal.merge(localDeAlmacen.getOrDefault(saldo.getWarehouseId(), 1L), saldo.getQuantity(), BigDecimal::add);
        porLocal.forEach((location, total) -> {
            SaldoLocal saldo = saldosLocal.findByLocationIdAndIngredientId(location, ingredient).orElseGet(() -> {
                SaldoLocal nuevo = new SaldoLocal(); nuevo.setLocationId(location); nuevo.setIngredientId(ingredient); nuevo.setVersion(0L); return nuevo;
            });
            saldo.setQuantity(total);
            saldo.setVersion(saldo.getVersion() == null ? 1L : saldo.getVersion() + 1);
            saldosLocal.save(saldo);
        });
    }
}
