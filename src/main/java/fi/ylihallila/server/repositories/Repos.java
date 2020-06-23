package fi.ylihallila.server.repositories;

import fi.ylihallila.server.repositories.impl.*;

public class Repos {

    private static final WorkspaceRepositoryJson workspaceRepo = new WorkspaceRepositoryJson();
    private static final ProjectRepositoryJson   projectRepo   = new ProjectRepositoryJson();
    private static final UserRepositoryJson      userRepo      = new UserRepositoryJson();

    public static WorkspaceRepositoryJson getWorkspaceRepo() {
        return workspaceRepo;
    }

    public static ProjectRepositoryJson getProjectRepo() {
        return projectRepo;
    }

    public static UserRepositoryJson getUserRepo() {
        return userRepo;
    }
}
