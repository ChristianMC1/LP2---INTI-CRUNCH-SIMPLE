package pe.inticrunch.inventario.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.inventario.entity.Receta;

public interface RecetaRepository extends JpaRepository<Receta, Long> {
    List<Receta> findByActiveTrueOrderById();
    List<Receta> findByProductIdAndActiveTrue(Long productId);
    List<Receta> findByProductId(Long productId);
    @Query("select coalesce(max(r.version), 0) + 1 from Receta r where r.productId = :product")
    int siguienteVersion(Long product);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Receta r where r.id = :id")
    Optional<Receta> bloquear(Long id);
    @Query("select distinct r.productId from Receta r, IngredienteReceta ri where ri.recipeId = r.id and ri.ingredientId = :ingredient and r.active = true")
    List<Long> productosQueUsan(Long ingredient);
}
