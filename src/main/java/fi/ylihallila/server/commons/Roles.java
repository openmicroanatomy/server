package fi.ylihallila.server.commons;

import io.javalin.core.security.Role;

import java.util.Set;

public enum Roles implements Role {

    ANYONE("Anyone"),
    ADMIN("Administrative tasks"),
    MODERATOR("Moderator tasks"),

    MANAGE_USERS("Manage users"),
    MANAGE_SLIDES("Manage slides");

    private String description;

    Roles(String description) {
        this.description = description;
    }

    private final static Set<Roles> MODIFIABLE_ROLES = Set.of(ADMIN, MODERATOR, MANAGE_USERS, MANAGE_SLIDES);

    /**
     * List of roles which can be modified by administrators / users with MANAGE_USERS permission.
     */
    public static Set<Roles> getModifiableRoles() {
        return MODIFIABLE_ROLES;
    }

    public String getDescription() {
        return description;
    }
}
