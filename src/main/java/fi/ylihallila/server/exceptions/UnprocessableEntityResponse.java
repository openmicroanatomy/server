package fi.ylihallila.server.exceptions;

import io.javalin.http.HttpResponseException;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public class UnprocessableEntityResponse extends HttpResponseException {

    public UnprocessableEntityResponse() {
        super(HttpStatus.UNPROCESSABLE_ENTITY_422, "Unprocessable entity", Collections.emptyMap());
    }

    public UnprocessableEntityResponse(@NotNull String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY_422, message, Collections.emptyMap());
    }

    public UnprocessableEntityResponse(@NotNull String message, @NotNull Map<String, String> details) {
        super(HttpStatus.UNPROCESSABLE_ENTITY_422, message, details);
    }
}
