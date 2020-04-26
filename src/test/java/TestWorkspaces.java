import fi.ylihallila.server.SecureServer;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestWorkspaces {

    @BeforeAll
    public static void init() {
        new SecureServer();
    }

    @Test
    public void ListAllSlidesSuccess() {
        HttpResponse response = Unirest.get("http://localhost:7777/api/v0/slides").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(
            //<editor-fold desc="Testslide">
                "[\"Testslide.svs\"]"
            //</editor-fold>
        );
    }

    @Test
    public void GetSlidePropertiesSuccess() {
        HttpResponse response = Unirest.get("http://localhost:7777/api/v0/slides/Testslide.svs").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(
            //<editor-fold desc="Properties">
                "{\r\n" +
                "  \"Test\": \"Yes\"" + "\r\n" +
                "}"
            //</editor-fold>
        );
    }

    @Test
    public void GetSlidePropertiesFailure() {
        HttpResponse response = Unirest.get("http://localhost:7777/api/v0/slides/Fail").asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    public void FetchWorkspaceSuccess() {
        HttpResponse response = Unirest.get("http://localhost:7777/api/v0/workspaces").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(
            //<editor-fold desc="Workspace">
                "{\r\n" +
                "  \"workspaces\": [\r\n" +
                "    {\r\n" +
                "      \"name\": \"Test\",\r\n" +
                "      \"projects\": [\r\n" +
                "        {\r\n" +
                "          \"id\": \"Test\",\r\n" +
                "          \"server\": \"http://localhost:7000\",\r\n" +
                "          \"name\": \"Test\",\r\n" +
                "          \"description\": \"\",\r\n" +
                "          \"thumbnail\": \"\"\r\n" +
                "        }\r\n" +
                "      ]\r\n" +
                "    }\r\n" +
                "  ]\r\n" +
                "}"
            //</editor-fold>
        );
    }

    @Test
    public void DownloadProjectSuccess() {
        HttpResponse response = Unirest.get("http://localhost:7777/api/v0/projects/Test").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("application/zip");
    }

    @Test
    public void DownloadProjectFailure() {
        HttpResponse response = Unirest.get("http://localhost:7777/api/v0/projects/Fail").asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }
}
