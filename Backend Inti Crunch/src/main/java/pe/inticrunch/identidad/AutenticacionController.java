package pe.inticrunch.identidad;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public
class AutenticacionController {

    private final CuentaService accounts;

    private final HistorialService history;

    public AutenticacionController(CuentaService accounts,HistorialService history) {
        this.accounts=accounts;
        this.history=history;
    }

    @PostMapping("/public/auth/register")
    public Map<String,Object> register(@RequestBody Map<String,Object> data) {
        return accounts.register(data);
    }

    @PostMapping("/public/auth/login")
    public Map<String,Object> login(@RequestBody Map<String,Object> data) {
        return accounts.login(data);
    }

    @GetMapping("/me/history")
    @PreAuthorize("isAuthenticated()")
    public Map<String,Object> history(Authentication auth) {
        return history.history(auth);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String,Object> me(Authentication auth) {
        return accounts.profile(auth);
    }
}
