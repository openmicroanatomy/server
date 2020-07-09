package fi.ylihallila.server.gson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.Util;
import fi.ylihallila.server.gson.resolvers.UserIdResolver;

import java.util.Set;

@JsonIdentityInfo(scope = User.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = UserIdResolver.class)
public class User {

    /**
     * ID is defined as the Azure Active Directory Tenant ID
     * @see <a href="https://docs.microsoft.com/fi-fi/onedrive/find-your-office-365-tenant-id">Microsoft documentation</a>
     */
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
    private String organization;

    /**
     * Azure AD tenant GUID. Provided by Microsoft Open ID
     */
    private String organizationId;

    /**
     * Roles for this user.
     */
    private Set<Roles> roles;

    public User(String id, String name, String organizationId, Set<Roles> roles) {
        this.id = id;
        this.name = name;
        this.organizationId = organizationId;
        this.roles = roles;
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

    /**
     * Returns the users organization name.
     */
    @JsonSerialize
    public String getOrganization() {
        return Util.getHumanReadableName(organizationId).orElse("Unknown (" + organizationId + ")");
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organization) {
        this.organizationId = organization;
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
                ", organizationId='" + organizationId + '\'' +
                ", roles=" + roles +
                '}';
    }
}
