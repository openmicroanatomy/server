package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Simple class to print errors via @see Context.json();
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Error {

    private String error;

    public Error(String error) {
        this.error = error;
    }
}
