package fi.ylihallila.server.tests;

import fi.ylihallila.server.Main;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestWorkspacesAPI {

    private static String API_URL = "http://localhost:1337/api/v0";

    @BeforeAll
    static void init() throws IOException, InterruptedException {
        DummyDb.create();

        Main.main(new String[]{ "--insecure", "--port", "1337", "--test" });
    }

    /* Getting Workspaces */

    @Test
    @Order(1)
    public void GetAllWorkspaces() {
        var response = Unirest.get(API_URL + "/workspaces").asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(1)
    public void GetAllWorkspacesDifferentWhenLoggedIn() {
        var response1 = Unirest.get(API_URL + "/workspaces").asString();
        var response2 = Unirest.get(API_URL + "/workspaces")
                .basicAuth("teacher@example.com", "teacher")
                .asString();

        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(response2.getStatus()).isEqualTo(200);
        assertThat(response1).isNotEqualTo(response2);
    }

    @Test
    @Order(1)
    public void GetAllWorkspacesReturnsMyProjectsWhenLoggedIn() {
        var response = Unirest.get(API_URL + "/workspaces")
                .basicAuth("teacher@example.com", "teacher").asString();

        assertThat(response.getBody()).contains("Personal Workspace");
    }

    @Test
    @Order(1)
    public void GetSpecificWorkspaceSuccess() {
        var response = Unirest.get(API_URL + "/workspaces/" + DummyDb.WORKSPACE_A.getId()).asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(1)
    public void GetSpecificWorkspaceFailureNotFound() {
        var response = Unirest.get(API_URL + "/workspaces/404").asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }


    /* Create Workspaces */

    @Test
    @Order(2)
    public void CreateWorkspaceSuccess() {
        var response = Unirest.post(API_URL + "/workspaces")
                .basicAuth("teacher@example.com", "teacher")
                .field("workspace-name", "New Workspace")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(2)
    public void CreateWorkspaceFailureUnauthorizedNotLoggedIn() {
        var response = Unirest.post(API_URL + "/workspaces")
                .field("workspace-name", "Error")
                .asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    /* Edit Workspaces */

    @Test
    @Order(3)
    public void EditWorkspaceSuccess() {
        var response = Unirest.patch(API_URL + "/workspaces/" + DummyDb.WORKSPACE_A.getId())
                .basicAuth("teacher@example.com", "teacher")
                .field("workspace-name", "New Workspace A")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(3)
    public void EditWorkspaceFailureNotFound() {
        var response = Unirest.patch(API_URL + "/workspaces/404")
                .basicAuth("teacher@example.com", "teacher")
                .field("workspace-name", "New Workspace A")
                .asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    @Order(3)
    public void EditWorkspaceFailureUnauthorizedNotLoggedIn() {
        var response = Unirest.patch(API_URL + "/workspaces/" + DummyDb.WORKSPACE_A.getId())
                .field("", "")
                .asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(3)
    public void EditWorkspaceFailureUnauthorizedWrongOrganization() {
        var response = Unirest.patch(API_URL + "/workspaces/" + DummyDb.WORKSPACE_B.getId())
                .basicAuth("teacher@example.com", "teacher")
                .field("workspace-name", "New Workspace B")
                .asString();

        assertThat(response.getStatus()).isEqualTo(403);
    }

    /* Deleting */

    @Test
    @Order(4)
    public void DeleteWorkspaceSuccess() {
        var response = Unirest.delete(API_URL + "/workspaces/" + DummyDb.WORKSPACE_A.getId())
                .basicAuth("teacher@example.com", "teacher")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(4)
    public void DeleteWorkspaceFailureUnauthorizedNotLoggedIn() {
        var response = Unirest.delete(API_URL + "/workspaces/" + DummyDb.WORKSPACE_B.getId())
                .asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(4)
    public void DeleteWorkspaceFailureUnauthorizedWrongOrganization() {
        var response = Unirest.delete(API_URL + "/workspaces/" + DummyDb.WORKSPACE_B.getId())
                .basicAuth("teacher@example.com", "teacher")
                .asString();

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @Order(4)
    public void DeleteWorkspaceFailureNotFound() {
        var response = Unirest.delete(API_URL + "/workspaces/404")
                .basicAuth("teacher@example.com", "teacher")
                .asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }
}
