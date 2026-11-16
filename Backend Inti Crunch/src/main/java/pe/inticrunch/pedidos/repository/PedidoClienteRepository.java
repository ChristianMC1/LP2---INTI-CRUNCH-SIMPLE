package pe.inticrunch.pedidos.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.pedidos.entity.PedidoCliente;

public interface PedidoClienteRepository extends JpaRepository<PedidoCliente, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PedidoCliente o where o.id = :id")
    Optional<PedidoCliente> bloquear(Long id);
    /** Todos los identificadores en orden: sirven para numerar los pedidos 1, 2, 3… sin saltos. */
    @Query("select o.id from PedidoCliente o order by o.id")
    List<Long> todosLosIds();
    long countByIdLessThanEqual(Long id);
    Optional<PedidoCliente> findByRequestKey(UUID requestKey);
    Optional<PedidoCliente> findByTrackingToken(UUID trackingToken);
    @Query("select o from PedidoCliente o order by o.id desc")
    List<PedidoCliente> recientes(Pageable page);
    @Query("select o from PedidoCliente o where o.customerSubject = :subject order by o.id desc")
    List<PedidoCliente> deLaCuenta(String subject, Pageable page);
    @Query("select o from PedidoCliente o where o.customerSubject = :subject or o.customerId = :customer order by o.id desc")
    List<PedidoCliente> delCliente(String subject, Long customer, Pageable page);
    List<PedidoCliente> findByCustomerIdOrderByIdDesc(Long customerId);
    @Query("select o from PedidoCliente o where o.status <> 'CANCELLED' order by o.id desc")
    List<PedidoCliente> noCancelados(Pageable page);
    @Query("select o from PedidoCliente o where o.createdAt >= :desde and o.createdAt < :hasta order by o.createdAt desc")
    List<PedidoCliente> creadosEntre(Instant desde, Instant hasta);
    @Query("select o from PedidoCliente o where o.status not in ('COMPLETED','CANCELLED') and (o.tableId = :table or o.id in :visitOrders) order by o.id")
    List<PedidoCliente> activosDeMesa(Long table, Collection<Long> visitOrders);
    long countByStatus(String status);
    long countByCustomerIdAndStatusNot(Long customerId, String status);
}
