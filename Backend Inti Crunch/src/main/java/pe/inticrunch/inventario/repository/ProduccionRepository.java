package pe.inticrunch.inventario.repository;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.inventario.entity.Produccion;

public interface ProduccionRepository extends JpaRepository<Produccion, Long> {
    @Query("select p from Produccion p order by p.id desc")
    List<Produccion> recientes(Pageable page);
}
