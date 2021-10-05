package fi.ylihallila.server.util;

import fi.ylihallila.server.commons.Roles;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A class to represent non-existent Users and Organizations. These are currently used for creating fake user
 * objects for unauthenticated users, as some methods require a User object to check for e.g. read access. These dummy
 * objects have a random UUID assigned each time and have no read/write permissions to any workspace.
 */
public class Guest {

    public static class User extends fi.ylihallila.server.models.User {

        public User() {
            super(UUID.randomUUID().toString(), "Guest", "guest@localhost", EnumSet.noneOf(Roles.class), new Guest.Organization());
        }

        @Override
        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class Organization extends fi.ylihallila.server.models.Organization {

        public Organization() {
            super(UUID.randomUUID().toString(), "Guest Organization");
        }
    }
}
