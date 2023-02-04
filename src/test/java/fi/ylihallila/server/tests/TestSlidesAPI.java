package fi.ylihallila.server.tests;

import fi.ylihallila.server.Main;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSlidesAPI {

    private static String API_URL = "http://localhost:1337/api/v0";

    @BeforeAll
    static void init() throws IOException, InterruptedException {
        DummyDb.create();

        Main.main(new String[]{ "--insecure", "--port", "1337", "--test" });
    }

    @Test
    @Order(1)
    public void GetAllSlidesAsGuest() {
        var response = Unirest.get(API_URL + "/slides").asString();

        assertThat(response.getStatus()).isIn(401, 403);
        assertThat(response.getBody()).doesNotContain(DummyDb.SLIDE_A.getId(), DummyDb.SLIDE_B.getId());
    }

    @Test
    @Order(1)
    public void GetAllSlidesAsTeacher() {
        var response = Unirest.get(API_URL + "/slides")
                .basicAuth("teacher@example.com", "teacher")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains(DummyDb.SLIDE_A.getId(), DummyDb.SLIDE_B.getId());
    }

    @Test
    @Order(1)
    public void GetSlidePropertiesAsGuest() throws IOException {
        var response = Unirest.get(API_URL + "/slides/" + DummyDb.SLIDE_A.getId()).asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(
            Files.readString(Path.of("slides/" + DummyDb.SLIDE_A.getId() + ".properties")).replaceAll("\\s+", "")
        );
    }

    /* Editing Slides */

    @Test
    @Order(2)
    public void EditSlideSuccess() {
        var response = Unirest.patch(API_URL + "/slides/" + DummyDb.SLIDE_A.getId())
                .basicAuth("teacher@example.com", "teacher")
                .field("slide-name", "Slide A New Name")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(2)
    public void EditSlideFailureForbiddenNotLoggedIn() {
        var response = Unirest.patch(API_URL + "/slides/" + DummyDb.SLIDE_A.getId())
                .field("slide-name", "Not logged in")
                .asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(2)
    public void EditSlideFailureNotFound() {
        var response = Unirest.patch(API_URL + "/slides/404")
                .basicAuth("teacher@example.com", "teacher")
                .field("slide-name", "Not Found")
                .asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    @Order(2)
    public void EditSlideFailureForbiddenWrongOrganization() {
        var response = Unirest.patch(API_URL + "/slides/" + DummyDb.SLIDE_B.getId())
                .basicAuth("teacher@example.com", "teacher")
                .field("slide-name", "Wrong Organization")
                .asString();

        assertThat(response.getStatus()).isEqualTo(403);
    }

    /* Deleting Slides */

    @Test
    @Order(3)
    public void DeleteSlideSuccess() {
//        var response = Unirest.delete(API_URL + "/slides/" + DummyDb.SLIDE_DELETE.getId())
//                .basicAuth(tTeacher@example.com", "teacher")
//                .asString();
//
//        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @Order(3)
    public void DeleteSlideFailureForbiddenNotLoggedIn() {
        var response = Unirest.delete(API_URL + "/slides/" + DummyDb.SLIDE_A.getId())
                .asString();

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @Order(3)
    public void DeleteSlideFailureNotFound() {
        var response = Unirest.delete(API_URL + "/slides/404")
                .basicAuth("teacher@example.com", "teacher")
                .asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    @Order(3)
    public void DeleteSlideFailureForbiddenWrongOrganization() {
        var response = Unirest.delete(API_URL + "/slides/" + DummyDb.SLIDE_B.getId())
                .basicAuth("teacher@example.com", "teacher")
                .asString();

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
