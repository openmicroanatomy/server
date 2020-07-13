package fi.ylihallila.server.repositories;

import fi.ylihallila.server.models.Project;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.models.Workspace;
import fi.ylihallila.server.repositories.impl.*;

public class Repos {

    private static final WorkspaceRepositoryJson workspaceRepo = new WorkspaceRepositoryJson();
    private static final ProjectRepositoryJson   projectRepo   = new ProjectRepositoryJson();
    private static final UserRepositoryJson      userRepo      = new UserRepositoryJson();

    public static Repository<Workspace> getWorkspaceRepo() {
        return workspaceRepo;
    }

    public static Repository<Project> getProjectRepo() {
        return projectRepo;
    }

    public static Repository<User> getUserRepo() {
        return userRepo;
    }
}
