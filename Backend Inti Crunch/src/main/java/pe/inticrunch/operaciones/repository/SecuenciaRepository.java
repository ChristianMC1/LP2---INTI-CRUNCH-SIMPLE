package pe.inticrunch.operaciones.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

/** Siguiente número de una secuencia de Oracle (por ejemplo, el correlativo de boletas). */
@Repository
public class SecuenciaRepository {
    private final EntityManager em;
    public SecuenciaRepository(EntityManager em) { this.em = em; }

    public long siguiente(String secuencia) {
        return ((Number) em.createNativeQuery("SELECT " + secuencia + ".NEXTVAL FROM dual").getSingleResult()).longValue();
    }
}
