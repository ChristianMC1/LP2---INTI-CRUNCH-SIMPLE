package pe.inticrunch.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.inventario.entity.ConsumoInsumoPedido;

public interface ConsumoInsumoPedidoRepository extends JpaRepository<ConsumoInsumoPedido, ConsumoInsumoPedido.Clave> {
    boolean existsByOrderLineId(Long orderLineId);
}
