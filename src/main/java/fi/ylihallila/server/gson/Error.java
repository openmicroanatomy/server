package fi.ylihallila.server.gson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Error {

    private String error;

    public Error(String error) {
        this.error = error;
    }
}
