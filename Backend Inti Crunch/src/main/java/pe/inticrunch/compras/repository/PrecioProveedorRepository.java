package pe.inticrunch.compras.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.compras.entity.PrecioProveedor;

public interface PrecioProveedorRepository extends JpaRepository<PrecioProveedor, PrecioProveedor.Clave> {}
