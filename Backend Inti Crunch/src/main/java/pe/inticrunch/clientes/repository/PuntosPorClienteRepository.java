package pe.inticrunch.clientes.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.Repository;
import pe.inticrunch.clientes.entity.MovimientoPuntos;

public interface PuntosPorClienteRepository extends Repository<MovimientoPuntos, Long> {
    @Query("select m.customerId, sum(m.points) from MovimientoPuntos m group by m.customerId")
    List<Object[]> puntosPorCliente();
}
