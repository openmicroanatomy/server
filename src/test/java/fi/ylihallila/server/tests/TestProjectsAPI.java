package fi.ylihallila.server.tests;

import fi.ylihallila.server.Application;
import io.javalin.plugin.json.JavalinJson;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestProjectsAPI {

    private static String API_URL = "http://localhost:1337/api/v0";
    private static Application app;

    @BeforeAll
    static void init() throws IOException {
        app = new Application();

        DummyDb.create();

        Files.createDirectory(Path.of("projects"));
        Files.createDirectory(Path.of("backups"));

        Files.copy(
            Path.of("Dummy.zip"),
            Path.of("projects/" + DummyDb.PROJECT_B.getId() + ".zip")
        );

        Files.copy(
            Path.of("Dummy.zip"),
            Path.of("projects/" + DummyDb.PROJECT_A.getId() + ".zip")
        );
    }

    // TODO: Test Personal Projects and Uploads

    @AfterAll
    static void destroy() throws IOException {
        FileUtils.deleteDirectory(new File("projects"));
        FileUtils.deleteDirectory(new File("backups"));
    }

    @Test
    @Order(1)
    public void GetAllProjectsSuccess() {
        var response = Unirest.get(API_URL + "/projects").basicAuth("Admin", "admin").asString();

        assertThat(response.getBody().length())
                .isEqualTo(JavalinJson.toJson(List.of(DummyDb.PROJECT_A, DummyDb.PROJECT_B)).length());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(1)
    public void GetAllProjectsUnauthorized() {
        var response = Unirest.get(API_URL + "/projects").asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(1)
    public void DownloadProjectSuccess() {
        var response = Unirest.get(API_URL + "/projects/" + DummyDb.PROJECT_A.getId()).asBytes();

        assertThat(response.getBody().length).isEqualTo(1055);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(1)
    public void DownloadProjectFailure() {
        var response = Unirest.get(API_URL + "/projects/error").asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    @Order(2)
    public void UpdateProjectSuccessSameOrganization() {
        var response = Unirest.put(API_URL + "/projects/" + DummyDb.PROJECT_A.getId())
                .basicAuth("Teacher", "teacher")
                .field("name", "New Project Name")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(2)
    public void UpdateProjectFailureWrongOrganization() {
        var response = Unirest.put(API_URL + "/projects/" + DummyDb.PROJECT_B.getId())
                .basicAuth("Teacher", "teacher")
                .field("name", "New Project Name")
                .asString();

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @Order(2)
    public void UpdateProjectFailureUnauthorized() {
        var response = Unirest.put(API_URL + "/projects/" + DummyDb.PROJECT_A.getId())
                .field("name", "New Project Name")
                .asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(2)
    public void UpdateProjectSuccessAdminOverride() {
        var response = Unirest.put(API_URL + "/projects/" + DummyDb.PROJECT_B.getId())
                .basicAuth("Admin", "admin")
                .field("name", "New Project Name")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(2)
    public void CreateProjectSuccess() {
        var response = Unirest.post(API_URL + "/projects")
                .basicAuth("Teacher", "teacher")
                .field("workspace-id", DummyDb.WORKSPACE_A.getId())
                .field("project-name", "New Project")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(2)
    public void CreateProjectUnauthorized() {
        var response = Unirest.post(API_URL + "/projects")
                .field("workspace-id", DummyDb.WORKSPACE_A.getId())
                .field("project-name", "New Project")
                .asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(3)
    public void DeleteProjectSuccess() {
        var response = Unirest.delete(API_URL + "/projects/" + DummyDb.PROJECT_A.getId())
                .basicAuth("Teacher", "teacher")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(3)
    public void DeleteProjectUnauthorizedGuest() {
        var response = Unirest.delete(API_URL + "/projects/" + DummyDb.PROJECT_B.getId())
                .asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(3)
    public void DeleteProjectForbiddenWrongOrganization() {
        var response = Unirest.delete(API_URL + "/projects/" + DummyDb.PROJECT_B.getId())
                .basicAuth("Teacher", "teacher")
                .asString();

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @Order(3)
    public void DeleteProjectNotFound() {
        var response = Unirest.delete(API_URL + "/projects/404")
                .basicAuth("Teacher", "teacher")
                .asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }
}
