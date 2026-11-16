package pe.inticrunch.caja.repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.caja.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByRequestKey(UUID requestKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :id and p.status = 'CONFIRMED'")
    Optional<Pago> bloquearConfirmado(Long id);
    @Query("select p from Pago p order by p.id desc")
    List<Pago> recientes(Pageable page);
    List<Pago> findByCreatedByInOrderByIdDesc(Collection<String> createdBy);
    @Query("select coalesce(sum(p.amount), 0) from Pago p where p.status = 'CONFIRMED' and p.createdAt >= :desde and p.createdAt < :hasta")
    BigDecimal cobradoEntre(Instant desde, Instant hasta);
    @Query("select coalesce(sum(p.amount), 0) from Pago p where p.status = 'CONFIRMED'")
    BigDecimal totalCobrado();
    @Query("select p from Pago p where p.status = 'CONFIRMED' and p.createdAt >= :desde")
    List<Pago> confirmadosDesde(Instant desde);
}
