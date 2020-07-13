package fi.ylihallila.server.repositories.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.models.Project;
import fi.ylihallila.server.models.Workspace;
import fi.ylihallila.server.repositories.AbstractJsonRepository;
import io.javalin.http.NotFoundResponse;

import java.nio.file.Path;
import java.util.*;

public class WorkspaceRepositoryJson extends AbstractJsonRepository<Workspace> {

    public WorkspaceRepositoryJson() {
        super(Path.of(Constants.WORKSPACE_FILE), new ObjectMapper().getTypeFactory().constructParametricType(List.class, Workspace.class));
    }

    @Override
    public Optional<Workspace> getById(String id) {
        for (Workspace workspace : getData()) {
            if (workspace.getId().equals(id)) {
                return Optional.of(workspace);
            }
        }

        return Optional.empty();
    }

    @Override
    public void update(Workspace updated) {
        Optional<Workspace> query = getById(updated.getId());

        if (query.isPresent()) {
            Workspace workspace = query.get();
            workspace.setName(updated.getName());
            workspace.setProjects(updated.getProjects());
        }
    }
}
