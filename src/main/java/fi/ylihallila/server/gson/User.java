package fi.ylihallila.server.gson;

import com.fasterxml.jackson.annotation.*;
import fi.ylihallila.server.authentication.Roles;
import fi.ylihallila.server.gson.resolvers.UserIdResolver;

import java.util.List;

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
     * Azure AD tenant GUID. Provided by Microsoft Open ID
     */
    private String organizationId;

    /**
     * Roles for this user.
     */
    private List<Roles> roles;

    public User(String id, String name, String organizationId, List<Roles> roles) {
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

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organization) {
        this.organizationId = organization;
    }

    public List<Roles> getRoles() {
        return roles;
    }

    public void setRoles(List<Roles> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", roles=" + roles +
                '}';
    }
}
