package fi.ylihallila.server;

import fi.ylihallila.server.gson.User;
import fi.ylihallila.server.repositories.Repos;

import java.util.Map;
import java.util.Optional;

public class Util {

    private static Map<String, String> knownTenants = Map.of(
        "9f9ce49a-5101-4aa3-8c75-0d5935ad6525", "University of Oulu"
    );

    public static Optional<String> idToName(String id) {
        if (knownTenants.containsKey(id)) {
            return Optional.of(knownTenants.get(id));
        }

        return Repos.getUserRepo().getById(id).map(User::getName);
    }

}
