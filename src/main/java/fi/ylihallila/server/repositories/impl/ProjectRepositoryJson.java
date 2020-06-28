package fi.ylihallila.server.repositories.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.ylihallila.server.gson.Project;
import fi.ylihallila.server.repositories.AbstractJsonRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectRepositoryJson extends AbstractJsonRepository<Project> {

    public ProjectRepositoryJson() {
        super(Path.of("projects.json"), new ObjectMapper().getTypeFactory().constructParametricType(List.class, Project.class));
    }

    public List<Project> getByOwner(String ownerId) {
        List<Project> projects = new ArrayList<>();

        for (Project project : getData()) {
            if (project.getOwner().equals(ownerId)) {
                projects.add(project);
            }
        }

        return projects;
    }

    @Override
    public Optional<Project> getById(String id) {
        for (Project project : getData()) {
            if (project.getId().equals(id)) {
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
}
