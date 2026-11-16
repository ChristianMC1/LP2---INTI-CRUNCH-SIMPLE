package pe.inticrunch.inventario.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.inventario.entity.Lote;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    Optional<Lote> findByWarehouseIdAndIngredientIdAndBatchCode(Long warehouseId, Long ingredientId, String batchCode);
    /** Lotes que se pueden usar, primero los que vencen antes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.warehouseId = :warehouse and l.ingredientId = :ingredient and l.quantity > 0 and (:incluirVencidos = true or l.expiresOn is null or l.expiresOn >= :hoy) order by l.expiresOn asc nulls last, l.id")
    List<Lote> utilizables(Long warehouse, Long ingredient, boolean incluirVencidos, LocalDate hoy);
    @Query("select l from Lote l where l.quantity > 0 order by l.expiresOn asc nulls last, l.id")
    List<Lote> conExistencia();
    @Query("select l.ingredientId, min(l.expiresOn) from Lote l where l.quantity > 0 group by l.ingredientId")
    List<Object[]> proximoVencimientoPorInsumo();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.id = :id")
    Optional<Lote> bloquear(Long id);
}
