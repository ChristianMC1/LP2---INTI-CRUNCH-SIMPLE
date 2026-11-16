package pe.inticrunch.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.inventario.entity.LineaProduccion;

public interface LineaProduccionRepository extends JpaRepository<LineaProduccion, LineaProduccion.Clave> {}
