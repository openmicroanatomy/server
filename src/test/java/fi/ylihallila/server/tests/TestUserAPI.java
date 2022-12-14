package fi.ylihallila.server.tests;

import fi.ylihallila.server.Main;
import fi.ylihallila.server.commons.Roles;
import io.javalin.plugin.json.JavalinJson;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestUserAPI {

    private static String API_URL = "http://localhost:1337/api/v0";

    @BeforeAll
    static void init() throws IOException, InterruptedException {
        DummyDb.create();

        Main.main(new String[]{ "--insecure", "--port", "1337", "--test" });
    }

    @Test
    @Order(1)
    public void GetAllUsers() {
        var response = Unirest.get(API_URL + "/users")
                .basicAuth("admin@example.com", "admin")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody().length()).isEqualTo(JavalinJson.toJson(List.of(DummyDb.ADMIN, DummyDb.TEACHER)).length());
    }

    @Test
    @Order(1)
    public void GetUserTeacher() {
        var response = Unirest.get(API_URL + "/users/70e99eac-b439-4a73-967e-2d83870b8326")
                .basicAuth("admin@example.com", "admin")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody().length()).isEqualTo(JavalinJson.toJson(DummyDb.TEACHER).length());
    }

    @Test
    @Order(1)
    public void GetNonexistentUser() {
        var response = Unirest.get(API_URL + "/users/error")
                .basicAuth("admin@example.com", "admin")
                .asString();

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
        var response = Unirest.get(API_URL + "/auth/login")
                .basicAuth("teacher@example.com", "teacher")
                .asString();

        assertThat(response.getBody().length()).isEqualTo(JavalinJson.toJson(DummyDb.TEACHER).length());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(1)
    public void VerifyJWTFails() {
        var response = Unirest.get(API_URL + "/auth/verify")
                .header("Token", "Invalid JWT Token")
                .asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(1)
    public void LoginFailure() {
        var response = Unirest.get(API_URL + "/auth/login")
                .basicAuth("Wrong", "Credentials")
                .asString();

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
        var response = Unirest.patch(API_URL + "/users/" + DummyDb.TEACHER.getId())
                .basicAuth("admin@example.com", "admin")
                .field(Roles.MANAGE_SLIDES.name(), false)
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(3)
    public void UserRolesReallyEdited() {
        var response = Unirest.get(API_URL + "/users/" + DummyDb.TEACHER.getId())
                .basicAuth("admin@example.com", "admin")
                .asString();

        assertThat(response.getBody()).doesNotContain(Roles.MANAGE_SLIDES.name());
    }

    @Test
    @Order(4)
    public void AdminCanMakeAnotherUserAdmin() {
        var response = Unirest.patch(API_URL + "/users/" + DummyDb.TEACHER.getId())
                .basicAuth("admin@example.com", "admin")
                .field(Roles.ADMIN.name(), true)
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
