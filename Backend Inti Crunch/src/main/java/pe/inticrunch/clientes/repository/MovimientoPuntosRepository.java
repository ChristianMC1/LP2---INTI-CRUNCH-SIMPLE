package pe.inticrunch.clientes.repository;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.clientes.entity.MovimientoPuntos;

public interface MovimientoPuntosRepository extends JpaRepository<MovimientoPuntos, Long> {
    boolean existsByOrderIdAndReason(Long orderId, String reason);
    @Query("select coalesce(sum(m.points), 0) from MovimientoPuntos m where m.orderId = :order")
    int puntosDelPedido(Long order);
    @Query("select coalesce(sum(m.points), 0) from MovimientoPuntos m where m.customerId = :customer")
    int puntosDelCliente(Long customer);
    @Query("select m from MovimientoPuntos m order by m.id desc")
    List<MovimientoPuntos> recientes(Pageable page);
    @Query("select m from MovimientoPuntos m where m.customerId = :customer order by m.id desc")
    List<MovimientoPuntos> delCliente(Long customer, Pageable page);
    boolean existsByCustomerId(Long customerId);
}
