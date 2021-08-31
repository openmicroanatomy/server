package fi.ylihallila.server.models;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
public class PasswordResetRequest {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * The user whose password can be reset with this request.
     */
    @ManyToOne
    private User user;

    /**
     * Random secure string of 32 characters.
     */
    private String token;

    /**
     * Unix timestamp when this request will expire and is invalid.
     */
    private Long expiryDate;

    public PasswordResetRequest() {}

    public PasswordResetRequest(User user) {
        this.user = user;
        this.token = RandomStringUtils.random(32, 0, 0, true, true, null, new SecureRandom());
        this.expiryDate = Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli();
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public Long getExpiryDate() {
        return expiryDate;
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() > getExpiryDate();
    }
}
