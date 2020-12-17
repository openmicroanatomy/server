package fi.ylihallila.server.tests;

import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.Application;
import io.javalin.plugin.json.JavalinJson;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestUserAPI {

    private static String API_URL = "http://localhost:1337/api/v0";
    private static Application app;

    @BeforeAll
    static void init() {
        app = new Application();

        DummyDb.create();
    }


    @Test
    @Order(1)
    public void GetAllUsers() {
        var response = Unirest.get(API_URL + "/users").basicAuth("Admin", "admin").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody().length()).isEqualTo(JavalinJson.toJson(List.of(DummyDb.ADMIN, DummyDb.TEACHER)).length());
    }

    @Test
    @Order(1)
    public void GetUserTeacher() {
        var response = Unirest.get(API_URL + "/users/70e99eac-b439-4a73-967e-2d83870b8326").basicAuth("Admin", "admin").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody().length()).isEqualTo(JavalinJson.toJson(DummyDb.TEACHER).length());
    }

    @Test
    @Order(1)
    public void GetNonexistentUser() {
        var response = Unirest.get(API_URL + "/users/error").basicAuth("Admin", "admin").asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    @Order(1)
    public void GetUserReturns401ForGuest() {
        var response = Unirest.get(API_URL + "/users/70e99eac-b439-4a73-967e-2d83870b8326").asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(1)
    public void LoginSuccess() {
        var response = Unirest.get(API_URL + "/users/login").basicAuth("Teacher", "teacher").asString();

        Map<String, Object> data = new HashMap<>();

        data.put("userId", DummyDb.TEACHER.getId());
        data.put("organizationId", DummyDb.TEACHER.getOrganization().getId());
        data.put("roles", DummyDb.TEACHER.getRoles());

        assertThat(response.getBody().length()).isEqualTo(JavalinJson.toJson(data).length());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(1)
    public void VerifyJWTFails() {
        var response = Unirest.get(API_URL + "/users/verify")
                .header("Token", "Invalid JWT Token").asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(1)
    public void LoginFailure() {
        var response = Unirest.get(API_URL + "/users/login").basicAuth("Wrong", "Credentials").asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    /*
     *
     *  TODO: The following tests modify the database. Is this bad?
     *
     */

    @Test
    @Order(2)
    public void EditUserRolesSuccess() {
        var response = Unirest.put(API_URL + "/users/70e99eac-b439-4a73-967e-2d83870b8326")
                .basicAuth("Admin", "admin")
                .field(Roles.MANAGE_PERSONAL_PROJECTS.name(), false)
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(2)
    public void UserRolesReallyEdited() {
        var response = Unirest.get(API_URL + "/users/70e99eac-b439-4a73-967e-2d83870b8326")
                .basicAuth("Admin", "admin")
                .asString();

        DummyDb.TEACHER.getRoles().remove(Roles.MANAGE_PERSONAL_PROJECTS);

        assertThat(response.getBody().length()).isEqualTo(JavalinJson.toJson(DummyDb.TEACHER).length());

        DummyDb.TEACHER.getRoles().add(Roles.MANAGE_PERSONAL_PROJECTS);
    }

    @Test
    @Order(2)
    public void EditUserRolesFailure() {
        var response = Unirest.put(API_URL + "/users/70e99eac-b439-4a73-967e-2d83870b8326")
                .basicAuth("Admin", "admin")
                .field(Roles.ADMIN.name(), true)
                .asString();

        assertThat(response.getStatus()).isEqualTo(400);
    }
}
