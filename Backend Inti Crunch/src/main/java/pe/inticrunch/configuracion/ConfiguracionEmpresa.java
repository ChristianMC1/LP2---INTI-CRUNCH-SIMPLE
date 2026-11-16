package pe.inticrunch.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class ConfiguracionEmpresa {
    @Value("${inti-crunch.seller.name:Inti Crunch Juliaca S.A.C.}") public String name;
    @Value("${inti-crunch.seller.ruc:}") private String configuredRuc;
    @Value("${inti-crunch.seller.address:Urb. La Rinconada, Mz. B Lote 12, Juliaca, San Román, Puno}") public String address;
    @Value("${inti-crunch.seller.phone:951 000 999}") public String phone;
    public String ruc(){return configuredRuc==null||configuredRuc.isBlank()?"20000000001":configuredRuc;}
    public Map<String,Object> publicInfo(){return Map.of("name",name,"ruc",ruc(),"address",address,"phone",phone,"simulated",true,"taxRate",18);}
}
