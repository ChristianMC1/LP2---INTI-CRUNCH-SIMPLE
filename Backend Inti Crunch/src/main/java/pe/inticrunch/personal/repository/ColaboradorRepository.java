package pe.inticrunch.personal.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.inticrunch.personal.entity.Colaborador;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Colaborador e where e.id = :id")
    Optional<Colaborador> bloquear(Long id);
    @Query("select distinct e from Colaborador e join e.roles r where e.active = true and r in :roles order by e.fullName")
    List<Colaborador> activosConRol(Collection<String> roles);
    @Query("select distinct e from Colaborador e join e.roles r where e.active = true and e.available = true and r in :roles order by e.fullName")
    List<Colaborador> disponiblesConRol(Collection<String> roles);
    @Query("select e from Colaborador e order by e.id")
    List<Colaborador> todos();
    @Query("select e from Colaborador e where e.active = true order by e.fullName")
    List<Colaborador> activos();
    Optional<Colaborador> findByKeycloakSubject(String keycloakSubject);
    boolean existsByDni(String dni);
    @Query("select count(e) > 0 from Colaborador e where e.dni = :dni and e.id <> :id")
    boolean otroConDni(String dni, Long id);
    @Query("select e.username from Colaborador e where e.dni = :dni and e.username is not null")
    List<String> usuarioPorDni(String dni);
}
