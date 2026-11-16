package pe.inticrunch.caja.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.caja.entity.LineaComprobante;

public interface LineaComprobanteRepository extends JpaRepository<LineaComprobante, Long> {
    List<LineaComprobante> findByDocumentIdOrderById(Long documentId);
}
