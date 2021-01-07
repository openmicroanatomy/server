package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.hibernate.EnumSetType;
import fi.ylihallila.server.util.PasswordHelper;
import fi.ylihallila.server.util.Util;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Parameter;

import javax.persistence.Entity;
import javax.persistence.*;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.EnumSet;

@Entity
@TypeDef(name = "enum-set", defaultForType = EnumSet.class, typeClass = EnumSetType.class)
@DiscriminatorValue("User")
public class User extends Owner {

    /**
     * ID is defined as the Azure Active Directory Tenant ID
     * @see <a href="https://docs.microsoft.com/fi-fi/onedrive/find-your-office-365-tenant-id">Microsoft documentation</a>
     */
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * Name of this organization or user, visible in QuPath.
     */
    private String name;

    /**
     * Email for this user. Also used to identify user.
     */
    private String email;

    /**
     * Human readable presentation of @var organizationId;
     */
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Organization organization;

    /**
     * If true, this user authenticates via OAuth and has no password.
     */
    @Column(nullable = true)
    private boolean oauth;

    /**
     * Password, encrypted with PBKDF2WithHmacSHA1.
     * @see fi.ylihallila.server.util.PasswordHelper
     */
    @JsonIgnore
    private String password;

    /**
     * Roles for this user.
     */
    @Column(length = 20)
    @Type(type = "enum-set", parameters = {
        @Parameter(
            name = "enumClass",
            value = "fi.ylihallila.server.commons.Roles"
        )
    })
    private EnumSet<Roles> roles;

    public User() {}

    public User(String id, String name, String email, EnumSet<Roles> roles, Organization organization) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.roles = roles;
        this.organization = organization;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(String id) {
        setOrganization(Util.getOrganization(id));
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public boolean usesOAuth() {
        return oauth;
    }

    public void setOAuth(boolean oauth) {
        this.oauth = oauth;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void hashPassword(String password) {
        try {
            this.password = PasswordHelper.generateStrongPassword(password).toString();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    public EnumSet<Roles> getRoles() {
        return roles;
    }

    public void setRoles(EnumSet<Roles> roles) {
        this.roles = roles;
    }

    public boolean hasRole(Roles role) {
        return getRoles().contains(role);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", organization='" + organization + '\'' +
                ", roles=" + roles +
                '}';
    }
}


