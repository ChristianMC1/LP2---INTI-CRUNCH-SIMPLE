package pe.inticrunch.configuracion;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Convierte los nombres de los campos Java a columnas de Oracle (customerName -> customer_name).
 * Caso especial: "number" es palabra reservada en Oracle, por eso la columna se llama "NUMBER" (entre comillas).
 */
public class NombresColumnas extends CamelCaseToUnderscoresNamingStrategy {
    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        if (name != null && name.getText().equalsIgnoreCase("number")) return Identifier.toIdentifier("NUMBER", true);
        return super.toPhysicalColumnName(name, context);
    }
}
