package pe.inticrunch.pedidos;

import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/orders")
public class PedidoPublicoController {

    private final PedidoService service;

    public PedidoPublicoController(PedidoService service) {
        this.service=service;
    }

    @PostMapping
    public ResponseEntity<Map<String,Object>> create(@RequestBody Map<String,Object> data,Authentication auth) {
        if(auth==null||!auth.isAuthenticated()||auth instanceof AnonymousAuthenticationToken)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,"Inicia sesión o crea tu cuenta para hacer tu pedido.");
        Map<String,Object> request=new LinkedHashMap<>(data);
        request.remove("_customerId");
        return ResponseEntity.status(201).body(service.createForAccount(request,auth));
    }

    @GetMapping("/{token}")
    public Map<String,Object> track(@PathVariable UUID token) {
        return service.tracking(token);
    }
}
