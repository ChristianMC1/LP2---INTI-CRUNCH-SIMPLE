package pe.inticrunch.inventario.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.inventario.entity.SaldoLocal;

public interface SaldoLocalRepository extends JpaRepository<SaldoLocal, Long> {
    Optional<SaldoLocal> findByLocationIdAndIngredientId(Long locationId, Long ingredientId);
    List<SaldoLocal> findByLocationId(Long locationId);
}
