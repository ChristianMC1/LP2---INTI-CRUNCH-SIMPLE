package pe.inticrunch.compras.repository;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.compras.entity.Recepcion;

public interface RecepcionRepository extends JpaRepository<Recepcion, Long> {
    @Query("select r from Recepcion r order by r.id desc")
    List<Recepcion> recientes(Pageable page);
    @Query("select r from Recepcion r where r.receivedBy in :actores order by r.id desc")
    List<Recepcion> recibidasPor(Collection<String> actores, Pageable page);
}
