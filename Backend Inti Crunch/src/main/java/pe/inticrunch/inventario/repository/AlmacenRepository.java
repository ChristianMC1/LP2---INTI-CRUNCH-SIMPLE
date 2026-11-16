package pe.inticrunch.inventario.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.inventario.entity.Almacen;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
    List<Almacen> findByActiveTrueOrderById();
    Optional<Almacen> findByIdAndActiveTrue(Long id);
    @Query("select count(a) > 0 from Almacen a where lower(a.name) = lower(:name) and a.active = true")
    boolean existeActivo(String name);
    @Query("select a from Almacen a where lower(a.name) = lower(:name)")
    List<Almacen> buscarPorNombre(String name);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Almacen a where a.id = :id")
    Optional<Almacen> bloquear(Long id);
}
