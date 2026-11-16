package pe.inticrunch.clientes.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.clientes.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id")
    Optional<Cliente> bloquear(Long id);
    Optional<Cliente> findByPhone(String phone);
    Optional<Cliente> findBySubject(String subject);
    List<Cliente> findByDni(String dni);
    boolean existsByPhone(String phone);
    boolean existsByDniAndSubjectIsNotNull(String dni);
    boolean existsByPhoneAndSubjectIsNotNull(String phone);
    @Query("select c from Cliente c where c.phone = :phone and c.subject is null")
    List<Cliente> sinCuentaConCelular(String phone);
    @Query("select c from Cliente c where c.phone = :phone or c.dni = :dni order by c.id")
    List<Cliente> porCelularODni(String phone, String dni);
    @Query("select c from Cliente c order by c.id")
    List<Cliente> primeros(Pageable page);
    @Query("select c from Cliente c order by c.fullName")
    List<Cliente> porNombre();
}
