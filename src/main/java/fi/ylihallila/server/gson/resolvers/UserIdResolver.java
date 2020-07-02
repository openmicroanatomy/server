package fi.ylihallila.server.gson.resolvers;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import fi.ylihallila.server.Util;
import fi.ylihallila.server.gson.User;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class UserIdResolver implements ObjectIdResolver {
    @Override
    public void bindItem(ObjectIdGenerator.IdKey idKey, Object o) {

    }

    @Override
    public Object resolveId(ObjectIdGenerator.IdKey idKey) {
        try {
            List<User> tenants = Util.getMapper().readValue(Path.of("users.json").toFile(), new TypeReference<>() { });
            String id = (String) idKey.key;

            return tenants.stream().filter(tenant -> tenant.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ObjectIdResolver newForDeserialization(Object o) {
        return this;
    }

    @Override
    public boolean canUseFor(ObjectIdResolver objectIdResolver) {
        return false;
    }
}

