package pe.inticrunch.preparacion.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.preparacion.entity.ZonaReparto;

public interface ZonaRepartoRepository extends JpaRepository<ZonaReparto, Long> {
    List<ZonaReparto> findByActiveTrueOrderById();
    List<ZonaReparto> findByActiveTrueOrderByName();
    Optional<ZonaReparto> findByIdAndActiveTrue(Long id);
    @Query("select z from ZonaReparto z where lower(z.name) = lower(:name) and z.active = true")
    Optional<ZonaReparto> activaPorNombre(String name);
    @Query("select z from ZonaReparto z where lower(z.name) = lower(:name)")
    List<ZonaReparto> buscarPorNombre(String name);
    @Query("select count(z) > 0 from ZonaReparto z where lower(z.name) = lower(:name) and z.id <> :id")
    boolean otraConNombre(String name, Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select z from ZonaReparto z where z.id = :id")
    Optional<ZonaReparto> bloquear(Long id);
}
