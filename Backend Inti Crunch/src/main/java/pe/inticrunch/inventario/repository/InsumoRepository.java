package pe.inticrunch.inventario.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.inventario.entity.Insumo;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    List<Insumo> findByActiveTrueOrderById();
    List<Insumo> findByActiveTrueOrderByName();
    Optional<Insumo> findByIdAndActiveTrue(Long id);
    boolean existsBySku(String sku);
    @Query("select count(i) > 0 from Insumo i where lower(i.name) = lower(:name) and i.active = true")
    boolean existeActivo(String name);
    @Query("select i from Insumo i where lower(i.name) = lower(:name) order by i.id")
    List<Insumo> buscarPorNombre(String name);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id = :id")
    Optional<Insumo> bloquear(Long id);
}
