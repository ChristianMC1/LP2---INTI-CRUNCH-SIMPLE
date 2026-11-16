package pe.inticrunch.identidad;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Conecta el backend con Keycloak: crea cuentas de clientes y colaboradores,
 * cambia contraseñas, asigna roles e inicia sesión con DNI y contraseña.
 */
@Service
public class AdministracionKeycloakService {
    private static final Logger log=LoggerFactory.getLogger(AdministracionKeycloakService.class);
    public static final Set<String> REALM_ROLES=Set.of("ADMINISTRADOR","SUPERVISOR","CAJERO","OPERACIONES","ABASTECIMIENTO","REPARTIDOR","CLIENTE");
    private final RestClient http=RestClient.create();
    private final Map<String,String> names=new ConcurrentHashMap<>();
    @Value("${inti-crunch.keycloak.url:${KEYCLOAK_URL:http://localhost:8180}}") private String url;
    @Value("${inti-crunch.keycloak.realm:inti-crunch}") private String realm;
    @Value("${inti-crunch.keycloak.web-client:inti-crunch-web}") private String webClient;
    @Value("${inti-crunch.keycloak.admin-realm:master}") private String adminRealm;
    @Value("${inti-crunch.keycloak.admin-user:${KC_ADMIN_USER:admin}}") private String adminUser;
    @Value("${inti-crunch.keycloak.admin-password:${KC_ADMIN_PASSWORD:admin_dev_only}}") private String adminPassword;
    private String adminToken;private Instant adminTokenExpires=Instant.EPOCH;

    /** Prepara Keycloak al iniciar: roles del sistema e inicio de sesión con DNI desde la web. */
    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap(){
        // Las tablas del panel muestran el nombre de la persona en vez de su código de Keycloak.
        pe.inticrunch.operaciones.Filas.actorNames(this::displayName);
        try{
            for(String role:REALM_ROLES){
                try{admin().get().uri(api("/roles/"+role)).retrieve().toBodilessEntity();}
                catch(HttpClientErrorException.NotFound missing){admin().post().uri(api("/roles")).contentType(MediaType.APPLICATION_JSON).body(Map.of("name",role)).retrieve().toBodilessEntity();log.info("Rol {} creado en Keycloak.",role);}
            }
            var clients=admin().get().uri(api("/clients?clientId="+webClient)).retrieve().body(new ParameterizedTypeReference<List<Map<String,Object>>>(){});
            if(clients!=null&&!clients.isEmpty()){
                Map<String,Object> client=new LinkedHashMap<>(clients.getFirst());
                if(!Boolean.TRUE.equals(client.get("directAccessGrantsEnabled"))){
                    client.put("directAccessGrantsEnabled",true);
                    admin().put().uri(api("/clients/"+client.get("id"))).contentType(MediaType.APPLICATION_JSON).body(client).retrieve().toBodilessEntity();
                    log.info("Inicio de sesión con DNI habilitado para el cliente {}.",webClient);
                }
            }
        }catch(Exception exception){log.warn("No se pudo preparar Keycloak automáticamente ({}). El registro y el inicio de sesión con DNI requieren Keycloak activo.",exception.getMessage());}
    }

    /** Inicia sesión en Keycloak con usuario (DNI) y contraseña. */
    public Map<String,Object> login(String username,String password){
        var form=new LinkedMultiValueMap<String,String>();
        form.add("grant_type","password");form.add("client_id",webClient);form.add("username",username);form.add("password",password);form.add("scope","openid profile email");
        try{
            return http.post().uri(url+"/realms/"+realm+"/protocol/openid-connect/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(new ParameterizedTypeReference<Map<String,Object>>(){});
        }catch(HttpClientErrorException exception){
            String body=exception.getResponseBodyAsString();
            if(body.contains("not fully set up")||body.contains("Account is not fully"))throw new IllegalStateException("Tu cuenta necesita completar datos en Keycloak. Pide ayuda al administrador.");
            if(body.contains("disabled"))throw new IllegalStateException("Tu cuenta está desactivada. Comunícate con el local.");
            if(body.contains("unauthorized_client")||body.contains("not allowed"))throw new IllegalStateException("El inicio de sesión con DNI aún no está habilitado. Reinicia el backend con Keycloak encendido.");
            throw new BadCredentialsException();
        }catch(RestClientException exception){throw new IllegalStateException("No se pudo conectar con Keycloak. Verifica que el servicio esté iniciado.");}
    }

    public Optional<Map<String,Object>> findByUsername(String username){
        var users=admin().get().uri(api("/users?exact=true&username="+encode(username))).retrieve().body(new ParameterizedTypeReference<List<Map<String,Object>>>(){});
        return users==null||users.isEmpty()?Optional.empty():Optional.of(users.getFirst());
    }
    public boolean emailInUse(String email,String exceptId){
        var users=admin().get().uri(api("/users?exact=true&email="+encode(email))).retrieve().body(new ParameterizedTypeReference<List<Map<String,Object>>>(){});
        return users!=null&&users.stream().anyMatch(user->!Objects.equals(user.get("id"),exceptId));
    }

    /** Crea una cuenta y devuelve su identificador (subject). */
    public String createUser(String username,String email,String firstName,String lastName,String password,Collection<String> roles){
        if(findByUsername(username).isPresent())throw new IllegalStateException("Ya existe una cuenta con el DNI o usuario "+username+".");
        if(email!=null&&!email.isBlank()&&emailInUse(email,null))throw new IllegalStateException("El correo "+email+" ya está registrado en otra cuenta.");
        Map<String,Object> user=new LinkedHashMap<>();
        user.put("username",username);user.put("enabled",true);user.put("emailVerified",true);user.put("firstName",firstName);user.put("lastName",lastName);
        if(email!=null&&!email.isBlank())user.put("email",email);
        user.put("requiredActions",List.of());
        user.put("credentials",List.of(Map.of("type","password","value",password,"temporary",false)));
        try{admin().post().uri(api("/users")).contentType(MediaType.APPLICATION_JSON).body(user).retrieve().toBodilessEntity();}
        catch(HttpClientErrorException exception){throw new IllegalArgumentException(keycloakMessage(exception,"No se pudo crear la cuenta en Keycloak."));}
        String id=findByUsername(username).map(found->found.get("id").toString()).orElseThrow(()->new IllegalStateException("Keycloak no devolvió la cuenta creada."));
        setRoles(id,roles);
        return id;
    }
    public void updateUser(String id,String email,String firstName,String lastName,boolean enabled){
        var current=new LinkedHashMap<>(user(id));
        if(email!=null&&!email.isBlank()){if(emailInUse(email,id))throw new IllegalStateException("El correo "+email+" ya está registrado en otra cuenta.");current.put("email",email);}
        current.put("firstName",firstName);current.put("lastName",lastName);current.put("enabled",enabled);current.remove("access");
        try{admin().put().uri(api("/users/"+id)).contentType(MediaType.APPLICATION_JSON).body(current).retrieve().toBodilessEntity();}
        catch(HttpClientErrorException exception){throw new IllegalArgumentException(keycloakMessage(exception,"No se pudo actualizar la cuenta en Keycloak."));}
        names.remove(id);
    }
    public void resetPassword(String id,String password){
        try{admin().put().uri(api("/users/"+id+"/reset-password")).contentType(MediaType.APPLICATION_JSON).body(Map.of("type","password","value",password,"temporary",false)).retrieve().toBodilessEntity();}
        catch(HttpClientErrorException exception){throw new IllegalArgumentException(keycloakMessage(exception,"Keycloak no aceptó la contraseña. Usa al menos 8 caracteres con letras y números."));}
    }
    public void setRoles(String id,Collection<String> wanted){
        var current=admin().get().uri(api("/users/"+id+"/role-mappings/realm")).retrieve().body(new ParameterizedTypeReference<List<Map<String,Object>>>(){});
        var remove=current==null?List.<Map<String,Object>>of():current.stream().filter(role->REALM_ROLES.contains(String.valueOf(role.get("name")))&&!wanted.contains(String.valueOf(role.get("name")))).toList();
        if(!remove.isEmpty())admin().method(org.springframework.http.HttpMethod.DELETE).uri(api("/users/"+id+"/role-mappings/realm")).contentType(MediaType.APPLICATION_JSON).body(remove).retrieve().toBodilessEntity();
        var present=current==null?Set.<String>of():new HashSet<>(current.stream().map(role->String.valueOf(role.get("name"))).toList());
        List<Map<String,Object>> add=new ArrayList<>();
        for(String role:wanted)if(!present.contains(role))add.add(admin().get().uri(api("/roles/"+role)).retrieve().body(new ParameterizedTypeReference<Map<String,Object>>(){}));
        if(!add.isEmpty())admin().post().uri(api("/users/"+id+"/role-mappings/realm")).contentType(MediaType.APPLICATION_JSON).body(add).retrieve().toBodilessEntity();
    }
    public void deleteUser(String id){
        try{admin().delete().uri(api("/users/"+id)).retrieve().toBodilessEntity();}catch(Exception exception){log.warn("No se pudo eliminar la cuenta {} de Keycloak: {}",id,exception.getMessage());}
    }
    public Map<String,Object> user(String id){
        try{return admin().get().uri(api("/users/"+id)).retrieve().body(new ParameterizedTypeReference<Map<String,Object>>(){});}
        catch(HttpClientErrorException.NotFound missing){throw new IllegalArgumentException("La cuenta de acceso ya no existe en Keycloak.");}
    }

    /** Convierte identificadores técnicos (UUID de Keycloak) en el nombre de la persona. */
    public String displayName(String value){
        if(value==null||!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))return value;
        return names.computeIfAbsent(value,id->{
            try{var user=user(id);String full=(Objects.toString(user.get("firstName"),"")+" "+Objects.toString(user.get("lastName"),"")).trim();return full.isBlank()?Objects.toString(user.get("username"),id):full;}
            catch(Exception exception){return "Usuario del sistema";}
        });
    }

    private synchronized RestClient admin(){
        if(adminToken==null||Instant.now().isAfter(adminTokenExpires)){
            var form=new LinkedMultiValueMap<String,String>();
            form.add("grant_type","password");form.add("client_id","admin-cli");form.add("username",adminUser);form.add("password",adminPassword);
            try{
                var token=http.post().uri(url+"/realms/"+adminRealm+"/protocol/openid-connect/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(new ParameterizedTypeReference<Map<String,Object>>(){});
                adminToken=Objects.toString(token.get("access_token"));
                adminTokenExpires=Instant.now().plusSeconds(Math.max(10,((Number)token.getOrDefault("expires_in",60)).longValue()-15));
            }catch(RestClientException exception){throw new IllegalStateException("No se pudo conectar con la administración de Keycloak. Revisa KC_ADMIN_USER y KC_ADMIN_PASSWORD.");}
        }
        String bearer="Bearer "+adminToken;
        return http.mutate().defaultHeader("Authorization",bearer).defaultStatusHandler(HttpStatusCode::isError,(request,response)->{
            byte[] body=response.getBody().readAllBytes();
            throw HttpClientErrorException.create(response.getStatusCode(),response.getStatusText(),response.getHeaders(),body,null);
        }).build();
    }
    private String api(String path){return url+"/admin/realms/"+realm+path;}
    private static String encode(String value){return java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8);}
    private static String keycloakMessage(HttpClientErrorException exception,String fallback){
        String body=exception.getResponseBodyAsString();
        if(body.contains("invalidPassword")||body.contains("password"))return "La contraseña no cumple la política de seguridad: usa al menos 8 caracteres con letras y números.";
        if(body.contains("email"))return "El correo no es válido o ya está registrado.";
        if(body.contains("username")||body.contains("User exists"))return "Ya existe una cuenta con ese DNI o usuario.";
        return fallback;
    }
    public static class BadCredentialsException extends RuntimeException{public BadCredentialsException(){super("DNI o contraseña incorrectos.");}}
}
