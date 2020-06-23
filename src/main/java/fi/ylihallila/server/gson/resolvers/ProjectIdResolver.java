package fi.ylihallila.server.gson.resolvers;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.ylihallila.server.gson.Project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ProjectIdResolver implements ObjectIdResolver {
    @Override
    public void bindItem(ObjectIdGenerator.IdKey idKey, Object o) {

    }

    @Override
    public Object resolveId(ObjectIdGenerator.IdKey idKey) {
        try {
            List<Project> projects = new ObjectMapper().readValue(Path.of("projects.json").toFile(), new TypeReference<>() { });
            String id = (String) idKey.key;

            return projects.stream().filter(project -> project.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
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
