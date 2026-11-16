package pe.inticrunch.inventario.repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.inventario.entity.SaldoAlmacen;

public interface SaldoAlmacenRepository extends JpaRepository<SaldoAlmacen, SaldoAlmacen.Clave> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SaldoAlmacen s where s.warehouseId = :warehouse and s.ingredientId = :ingredient")
    Optional<SaldoAlmacen> bloquear(Long warehouse, Long ingredient);
    Optional<SaldoAlmacen> findByWarehouseIdAndIngredientId(Long warehouseId, Long ingredientId);
    List<SaldoAlmacen> findByIngredientId(Long ingredientId);
    List<SaldoAlmacen> findByQuantityGreaterThan(BigDecimal quantity);
    long countByWarehouseIdAndQuantityGreaterThan(Long warehouseId, BigDecimal quantity);
    boolean existsByWarehouseIdAndQuantityGreaterThan(Long warehouseId, BigDecimal quantity);
}
