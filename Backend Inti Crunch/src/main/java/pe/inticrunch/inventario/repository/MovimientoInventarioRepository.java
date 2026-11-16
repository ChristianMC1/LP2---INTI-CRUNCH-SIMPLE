package pe.inticrunch.inventario.repository;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.inventario.entity.MovimientoInventario;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    @Query("select m from MovimientoInventario m order by m.id desc")
    List<MovimientoInventario> recientes(Pageable page);
    @Query("select m from MovimientoInventario m where m.createdBy in :actores order by m.id desc")
    List<MovimientoInventario> registradosPor(Collection<String> actores, Pageable page);
}
