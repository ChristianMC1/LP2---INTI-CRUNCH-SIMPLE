package pe.inticrunch.configuracion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ManejadorErrores {
    @ExceptionHandler(org.springframework.dao.EmptyResultDataAccessException.class)
    ProblemDetail notFound(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,"El registro no existe o no está disponible.");
    }
    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,org.springframework.dao.CannotAcquireLockException.class,org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    ProblemDetail dataConflict(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"El registro está duplicado, tiene referencias activas o cambió durante la operación. Actualiza los datos y revisa la selección.");
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,java.time.DateTimeException.class})
    ProblemDetail malformed(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Revisa el formato de los datos y las fechas.");
    }
    @ExceptionHandler(pe.inticrunch.identidad.AdministracionKeycloakService.BadCredentialsException.class)
    ProblemDetail badCredentials(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    ProblemDetail status(org.springframework.web.server.ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict(IllegalStateException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
