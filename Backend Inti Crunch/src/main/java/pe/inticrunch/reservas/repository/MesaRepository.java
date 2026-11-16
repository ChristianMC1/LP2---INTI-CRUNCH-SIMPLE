package pe.inticrunch.reservas.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.reservas.entity.Mesa;

public interface MesaRepository extends JpaRepository<Mesa, Long> {
    List<Mesa> findByActiveTrueOrderById();
    Optional<Mesa> findByIdAndActiveTrue(Long id);
    @Query("select count(m) > 0 from Mesa m where upper(m.tableCode) = :code and m.active = true")
    boolean existeActiva(String code);
    @Query("select m from Mesa m where upper(m.tableCode) = :code")
    List<Mesa> buscarPorCodigo(String code);
    @Query("select count(m) > 0 from Mesa m where upper(m.tableCode) = :code and m.id <> :id")
    boolean otraConCodigo(String code, Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mesa m where m.id = :id")
    Optional<Mesa> bloquear(Long id);
}
