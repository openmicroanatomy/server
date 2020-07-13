package fi.ylihallila.server.repositories.impl;

import fi.ylihallila.server.Util;
import fi.ylihallila.server.models.Project;
import fi.ylihallila.server.models.Workspace;
import fi.ylihallila.server.repositories.AbstractJsonRepository;
import fi.ylihallila.server.repositories.Repos;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectRepositoryJson extends AbstractJsonRepository<Project> {

    public ProjectRepositoryJson() {
        super(Path.of("projects.json"), Util.getMapper().getTypeFactory().constructParametricType(List.class, Project.class));
    }

    /**
     * Removes the project and also removes any orphans left in Workspaces.
     *
     * @param id id of the project to delete.
     */
    @Override public void deleteById(String id) {
        super.deleteById(id);

        for (Workspace workspace : Repos.getWorkspaceRepo().list()) {
            workspace.removeProject(id);
        }

        Repos.getWorkspaceRepo().commit();
    }

    @Override
    public Optional<Project> getById(String id) {
        for (Project project : getData()) {
            if (project.getId().equalsIgnoreCase(id)) {
                return Optional.of(project);
            }
        }

        return Optional.empty();
    }

    @Override
    public void update(Project newProject) {
        Project oldProject = getById(newProject.getId()).orElseThrow();

        oldProject.setName(newProject.getName());
        oldProject.setOwner(newProject.getOwner());
        oldProject.setDescription(newProject.getDescription());
        oldProject.setModifiedAt(System.currentTimeMillis());
    }

    @Override
    public void commit() {
        super.commit();
        Repos.getWorkspaceRepo().refresh();
    }
}
