package pe.inticrunch.reservas.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.reservas.entity.EventoReserva;

public interface EventoReservaRepository extends JpaRepository<EventoReserva, Long> {
    List<EventoReserva> findByReservationIdOrderById(Long reservationId);
    @Modifying
    @Query("delete from EventoReserva e where e.reservationId = :reservation")
    void borrarDeReserva(Long reservation);
}
