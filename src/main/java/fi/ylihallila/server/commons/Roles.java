package fi.ylihallila.server.commons;

import io.javalin.core.security.Role;

import java.util.Set;

public enum Roles implements Role {

    ANYONE("Anyone"),
    ADMIN("Administrative tasks"),

    MANAGE_USERS("Manage users"),

    MANAGE_SLIDES("Manage slides"),

    MANAGE_PERSONAL_PROJECTS("Manage personal projects"),
    MANAGE_PROJECTS("Manage projects");

    private String description;

    Roles(String description) {
        this.description = description;
    }

    private final static Set<Roles> MODIFIABLE_ROLES = Set.of(MANAGE_USERS, MANAGE_SLIDES, MANAGE_PERSONAL_PROJECTS, MANAGE_PROJECTS);

    /**
     * List of which roles can be added / removed by users with MANAGE_USERS role
     * @return Set<Roles>
     */
    public static Set<Roles> getModifiableRoles() {
        return MODIFIABLE_ROLES;
    }

    public String getDescription() {
        return description;
    }
}
