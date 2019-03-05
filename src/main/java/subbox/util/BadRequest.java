package subbox.util;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class BadRequest extends RuntimeException {

    public BadRequest(String message) {
        super(message);
    }

}
