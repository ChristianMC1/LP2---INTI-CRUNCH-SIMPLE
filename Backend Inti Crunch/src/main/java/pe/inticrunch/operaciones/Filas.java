package pe.inticrunch.operaciones;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Arma las filas (mapas columna → valor) que el panel muestra en sus tablas.
 * Las columnas de personas ("Responsable", "Cajero"…) muestran el nombre en lugar del código de Keycloak.
 */
public final class Filas {
    private static volatile UnaryOperator<String> actorNames = value -> value;
    private static final Set<String> ACTOR_COLUMNS = Set.of("Responsable", "Registrado por", "Recibido por", "Cajero", "Autorizado por", "Creado por");
    private Filas() {}

    /** Registra cómo convertir un identificador de Keycloak en el nombre de la persona. */
    public static void actorNames(UnaryOperator<String> resolver) { actorNames = resolver; }

    /** Fila con columnas en orden: Filas.of("ID", 1, "Mesa", "M1", …). Acepta valores nulos. */
    public static Map<String, Object> of(Object... keyValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            String key = keyValues[i].toString();
            Object value = keyValues[i + 1];
            if (ACTOR_COLUMNS.contains(key) && value instanceof String text && text.length() == 36) value = actorNames.apply(text);
            row.put(key, value);
        }
        return row;
    }

    /** Une textos con un separador, ignorando los vacíos. */
    public static String join(String separator, Collection<String> parts) {
        return String.join(separator, parts.stream().filter(part -> part != null && !part.isBlank()).toList());
    }
}
