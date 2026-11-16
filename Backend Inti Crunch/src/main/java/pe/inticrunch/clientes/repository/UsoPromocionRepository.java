package pe.inticrunch.clientes.repository;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.clientes.entity.UsoPromocion;

public interface UsoPromocionRepository extends JpaRepository<UsoPromocion, UsoPromocion.Clave> {
    long countByPromotionId(Long promotionId);
    boolean existsByPromotionId(Long promotionId);
    @Query("select u from UsoPromocion u order by u.redeemedAt desc")
    List<UsoPromocion> recientes(Pageable page);
}
