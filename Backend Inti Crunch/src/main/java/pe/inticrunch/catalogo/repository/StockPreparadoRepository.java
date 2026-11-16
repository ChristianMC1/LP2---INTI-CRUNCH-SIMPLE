package pe.inticrunch.catalogo.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.catalogo.entity.StockPreparado;

public interface StockPreparadoRepository extends JpaRepository<StockPreparado, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockPreparado s where s.productId = :product")
    Optional<StockPreparado> bloquear(Long product);
}
