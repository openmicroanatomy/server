package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.Database;
import fi.ylihallila.server.Util;
import fi.ylihallila.server.models.resolvers.UserIdResolver;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table( name = "users" )
//@JsonIdentityInfo(scope = User.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = UserIdResolver.class)
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
    private Organization organization;

    /**
     * Roles for this user.
     */
    @Column
    @Enumerated
    @ElementCollection(targetClass = Roles.class, fetch = FetchType.EAGER)
    private Set<Roles> roles;

    public User(String id, String name, Set<Roles> roles, Organization organization) {
        this.id = id;
        this.name = name;
        this.roles = roles;
        this.organization = organization;
    }

    public User() {}

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
        Session session = Database.getSession();
        session.beginTransaction();

        Organization organization = session.find(Organization.class, id);

        session.getTransaction().commit();
        session.close();

        setOrganization(organization);
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Set<Roles> getRoles() {
        return roles;
    }

    public void setRoles(Set<Roles> roles) {
        this.roles = roles;
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


