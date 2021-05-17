package fi.ylihallila.server.tests;

import fi.ylihallila.server.Main;
import io.javalin.plugin.json.JavalinJson;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestWorkspacesAPI {

    private static String API_URL = "http://localhost:1337/api/v0";

    @BeforeAll
    static void init() throws IOException, InterruptedException {
        Main.main(new String[]{ "--insecure" });

        DummyDb.create();
    }

    /* Getting Workspaces */

    @Test
    @Order(1)
    public void GetAllWorkspaces() {
        var response = Unirest.get(API_URL + "/workspaces").asString();

        assertThat(response.getBody().length())
                .isEqualTo(JavalinJson.toJson(List.of(DummyDb.WORKSPACE_A, DummyDb.WORKSPACE_B)).length());
    }


    @Test
    @Order(1)
    public void GetAllWorkspacesReturnsMyProjectsWhenLoggedIn() {
        var response = Unirest.get(API_URL + "/workspaces")
                .basicAuth("teacher@example.com", "teacher").asString();

        assertThat(response.getBody().length())
                .isGreaterThan(JavalinJson.toJson(List.of(DummyDb.WORKSPACE_A, DummyDb.WORKSPACE_B)).length());
        assertThat(response.getBody()).contains("My Projects");
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
        var response = Unirest.put(API_URL + "/workspaces/" + DummyDb.WORKSPACE_A.getId())
                .basicAuth("teacher@example.com", "teacher")
                .field("workspace-name", "New Workspace A")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(3)
    public void EditWorkspaceFailureNotFound() {
        var response = Unirest.put(API_URL + "/workspaces/404")
                .basicAuth("teacher@example.com", "teacher")
                .field("workspace-name", "New Workspace A")
                .asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    @Order(3)
    public void EditWorkspaceFailureUnauthorizedNotLoggedIn() {
        var response = Unirest.put(API_URL + "/workspaces/" + DummyDb.WORKSPACE_A.getId())
                .field("", "")
                .asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(3)
    public void EditWorkspaceFailureUnauthorizedWrongOrganization() {
        var response = Unirest.put(API_URL + "/workspaces/" + DummyDb.WORKSPACE_B.getId())
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
