package subbox.services;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static subbox.util.Maps.orderedMapOf;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    @ResponseStatus(BAD_REQUEST)
    public Map<String, ?> handle(ConstraintViolationException exception) {
        return orderedMapOf(
                "timestamp", ZonedDateTime.now(),
                "status", BAD_REQUEST.value(),
                "error", BAD_REQUEST.getReasonPhrase(),
                "message", exception.getMessage()
        );
    }

}
