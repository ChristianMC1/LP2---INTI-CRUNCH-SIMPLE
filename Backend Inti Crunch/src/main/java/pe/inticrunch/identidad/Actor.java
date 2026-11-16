package pe.inticrunch.identidad;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/** Nombre legible de quien realiza una operación (en lugar del UUID técnico de Keycloak). */
public final class Actor {
    private Actor(){}
    public static String name(Authentication auth){
        if(auth==null)return "Sistema";
        if(auth.getPrincipal() instanceof Jwt jwt){
            String name=jwt.getClaimAsString("name");
            if(name!=null&&!name.isBlank())return name;
            String user=jwt.getClaimAsString("preferred_username");
            if(user!=null&&!user.isBlank())return user;
        }
        return auth.getName();
    }
}
