package pe.inticrunch.catalogo.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.catalogo.entity.OpcionProducto;

public interface OpcionProductoRepository extends JpaRepository<OpcionProducto, Long> {
    @Query("select distinct o from OpcionProducto o left join fetch o.productIds where o.active = true order by o.id")
    List<OpcionProducto> activasConProductos();
    @Query("select o from OpcionProducto o join o.productIds p where o.id = :option and p = :product and o.active = true")
    Optional<OpcionProducto> activaParaProducto(Long option, Long product);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OpcionProducto o where o.id = :id")
    Optional<OpcionProducto> bloquear(Long id);
}
