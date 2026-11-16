package pe.inticrunch.inventario.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.inventario.entity.IngredienteReceta;

public interface IngredienteRecetaRepository extends JpaRepository<IngredienteReceta, IngredienteReceta.Clave> {
    List<IngredienteReceta> findByRecipeIdOrderByIngredientId(Long recipeId);
    List<IngredienteReceta> findByRecipeIdIn(Collection<Long> recipeIds);
}
