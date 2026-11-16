package pe.inticrunch.compras.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.compras.entity.LineaRecepcion;

public interface LineaRecepcionRepository extends JpaRepository<LineaRecepcion, LineaRecepcion.Clave> {
    List<LineaRecepcion> findByReceiptId(Long receiptId);
}
