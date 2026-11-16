package pe.inticrunch.operaciones;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;

/**
 * Validaciones y lectura de los datos que llegan desde el panel o la web.
 * Los services heredan de esta clase; el acceso a la base lo hacen sus repositorios JPA.
 */
public abstract class Validaciones {
    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    /** Error estándar cuando un registro no existe (la API responde 404). */
    public static EmptyResultDataAccessException noExiste() { return new EmptyResultDataAccessException(1); }

    protected static String str(Map<String, Object> data, String key) { Object value = data.get(key); if (value == null || value.toString().isBlank()) throw new IllegalArgumentException("El campo " + key + " es obligatorio."); return value.toString().trim(); }
    protected static String optional(Map<String, Object> data, String key) { Object value = data.get(key); return value == null ? "" : value.toString().trim(); }
    protected static String nullable(Map<String, Object> data, String key) { String value = optional(data, key); return value.isBlank() ? null : value; }
    protected static long id(Map<String, Object> data, String key) { try { return Long.parseLong(data.get(key).toString()); } catch (Exception ex) { throw new IllegalArgumentException("El campo " + key + " debe ser un identificador válido."); } }
    protected static Long optionalId(Map<String, Object> data, String key) { return optional(data, key).isBlank() ? null : id(data, key); }
    protected static BigDecimal decimal(Object value, String field) { try { BigDecimal n = new BigDecimal(value.toString()); if (n.signum() < 0) throw new NumberFormatException(); return n; } catch (Exception ex) { throw new IllegalArgumentException(field + " debe ser un número no negativo."); } }
    protected static String choice(Map<String, Object> data, String key, String... allowed) { String value = str(data, key).toUpperCase(Locale.ROOT); if (!Set.of(allowed).contains(value)) throw new IllegalArgumentException("Valor no válido para " + key + "."); return value; }
    protected static BigDecimal positive(Object value, String field) { var result = decimal(value, field); if (result.signum() <= 0 || result.scale() > 3 || result.compareTo(new BigDecimal("99999999")) > 0) throw new IllegalArgumentException(field + " debe ser positivo y tener hasta 3 decimales."); return result; }
    protected static BigDecimal money(Object value, String field) { var result = decimal(value, field); if (result.stripTrailingZeros().scale() > 2 || result.compareTo(new BigDecimal("99999999")) > 0) throw new IllegalArgumentException(field + " debe tener hasta 2 decimales."); return result.setScale(2, RoundingMode.HALF_UP); }
    protected static int integer(Object value, String field) { try { int result = Integer.parseInt(String.valueOf(value)); if (result < 1 || result > 10000) throw new NumberFormatException(); return result; } catch (NumberFormatException exception) { throw new IllegalArgumentException(field + " debe ser un entero entre 1 y 10000."); } }
    protected static List<Map<String, Object>> lines(Map<String, Object> data, String key) {
        if (!(data.get(key) instanceof List<?> values) || values.isEmpty() || values.size() > 100) throw new IllegalArgumentException("Agrega entre 1 y 100 elementos en " + key + ".");
        return values.stream().map(value -> { if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("Detalle inválido."); Map<String, Object> result = new LinkedHashMap<>(); map.forEach((name, item) -> result.put(name.toString(), item)); return result; }).toList();
    }
    protected static List<Long> ids(Object raw) {
        if (!(raw instanceof List<?> values)) return List.of();
        try { return values.stream().map(value -> Long.parseLong(value.toString())).distinct().sorted().toList(); }
        catch (Exception exception) { throw new IllegalArgumentException("Selección no válida."); }
    }
    protected static Instant instant(Object value) { try { return Instant.parse(String.valueOf(value)); } catch (Exception exception) { throw new IllegalArgumentException("Fecha u hora no válida."); } }
    protected static LocalDate date(Map<String, Object> data, String key) { String value = optional(data, key); if (value.isBlank()) return null; try { return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value); } catch (Exception exception) { throw new IllegalArgumentException("Fecha no válida."); } }
    protected static boolean flag(Map<String, Object> data, String key) { Object value = data.get(key); return !Boolean.FALSE.equals(value) && !"false".equals(String.valueOf(value)); }

    /** Fechas del día y del mes en Juliaca (America/Lima). */
    public static LocalDate hoy() { return LocalDate.now(LIMA); }
    public static Instant inicioDe(LocalDate day) { return day.atStartOfDay(LIMA).toInstant(); }
    public static Instant inicioDeHoy() { return inicioDe(hoy()); }
    public static Instant inicioDeManana() { return inicioDe(hoy().plusDays(1)); }
    public static Instant inicioDelMes() { return inicioDe(hoy().withDayOfMonth(1)); }
    public static String fechaCorta(LocalDate day) { return day.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    protected static BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
