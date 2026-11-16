package pe.inticrunch.compras.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.compras.entity.SolicitudCompra;

public interface SolicitudCompraRepository extends JpaRepository<SolicitudCompra, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SolicitudCompra r where r.id = :id and r.status = 'PENDING'")
    Optional<SolicitudCompra> bloquearPendiente(Long id);
    @Query("select r from SolicitudCompra r order by r.id desc")
    List<SolicitudCompra> recientes(Pageable page);
    List<SolicitudCompra> findByStatusOrderByIdDesc(String status);
}
