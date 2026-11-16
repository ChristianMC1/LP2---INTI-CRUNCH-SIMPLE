package pe.inticrunch.caja.controller;

import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.inticrunch.caja.service.CajaService;
import pe.inticrunch.pedidos.PedidoService;

@RestController
@RequestMapping("/public/orders/{token}")
public class PagoPublicoController {

    private final CajaService cash;

    private final PedidoService orders;

    public PagoPublicoController(CajaService cash,PedidoService orders) {
        this.cash=cash;
        this.orders=orders;
    }

    @PostMapping("/payments")
    public Map<String,Object> pay(@PathVariable UUID token,@RequestBody Map<String,Object> data) {
        return cash.collectPublic(orders.byToken(token),data);
    }

    @GetMapping("/documents")
    public List<Map<String,Object>> documents(@PathVariable UUID token) {
        return cash.orderDocuments(orders.byToken(token));
    }

    @GetMapping(value="/documents/{id}/pdf",produces="application/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID token,@PathVariable long id) {
        return ResponseEntity.ok().header("Content-Disposition","attachment; filename=\"inti-crunch-"+id+".pdf\"").body(cash.publicDocument(id,orders.byToken(token)));
    }
}
