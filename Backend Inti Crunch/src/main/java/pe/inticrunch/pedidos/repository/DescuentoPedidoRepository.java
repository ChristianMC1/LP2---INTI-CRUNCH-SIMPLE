package pe.inticrunch.pedidos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.inticrunch.pedidos.entity.DescuentoPedido;

public interface DescuentoPedidoRepository extends JpaRepository<DescuentoPedido, Long> {}
