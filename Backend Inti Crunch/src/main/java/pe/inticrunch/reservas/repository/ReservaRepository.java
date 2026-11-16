package pe.inticrunch.reservas.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.reservas.entity.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> bloquear(Long id);
    /**
     * Mesas que no se pueden usar en ese horario: reservas que se cruzan y reservas cuyos
     * clientes ya llegaron (bloquean hasta que el personal marque que se fueron).
     */
    @Query("select distinct t from Reserva r join r.tableIds t where r.id <> :excluir and r.status in ('BOOKED','ARRIVED') and r.startsAt < :hasta and (r.endsAt > :desde or r.status = 'ARRIVED')")
    List<Long> mesasReservadas(Instant desde, Instant hasta, Long excluir);
    @Query("select distinct t from Reserva r join r.tableIds t where r.id <> :excluir and r.status = 'BOOKED' and r.startsAt < :hasta and r.endsAt > :desde")
    List<Long> mesasConReservaPendiente(Instant desde, Instant hasta, Long excluir);
    @Query("select r from Reserva r join r.tableIds t where t = :table and r.status = 'BOOKED' and r.startsAt < :limite and r.endsAt > :ahora")
    List<Reserva> reservadasPronto(Long table, Instant ahora, Instant limite);
    @Query("select r from Reserva r join r.tableIds t where t = :table and r.status = 'BOOKED' and r.endsAt > :ahora order by r.startsAt")
    List<Reserva> proximas(Long table, Instant ahora, Pageable page);
    @Query("select count(r) > 0 from Reserva r join r.tableIds t where t = :table and r.status in ('BOOKED','ARRIVED')")
    boolean mesaConReservaPendiente(Long table);
    @Query("select count(r) > 0 from Reserva r join r.tableIds t where t = :table")
    boolean mesaConHistorial(Long table);
    @Query("select r from Reserva r order by r.startsAt desc")
    List<Reserva> recientes(Pageable page);
    @Query("select r from Reserva r where r.customerSubject = :subject order by r.startsAt desc")
    List<Reserva> deLaCuenta(String subject);
    @Query("select r from Reserva r where r.customerSubject = :subject or r.customerId = :customer order by r.startsAt desc")
    List<Reserva> delCliente(String subject, Long customer);
    boolean existsByCustomerId(Long customerId);
}
