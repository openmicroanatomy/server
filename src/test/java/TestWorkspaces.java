import fi.ylihallila.server.Server;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestWorkspaces {

    @BeforeAll
    public static void init() {
        new Server();
    }

    @Test
    public void ListAllSlidesSuccess() {
        HttpResponse response = Unirest.get("http://localhost:7000/api/v0/list_slides").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(
            //<editor-fold desc="Testslide">
                "[\n" +
                "  \"Testslide.svs\"\n" +
                "]"
            //</editor-fold>
        );
    }

    @Test
    public void GetSlidePropertiesSuccess() {
        HttpResponse response = Unirest.get("http://localhost:7000/api/v0/properties/Testslide.svs").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(
            //<editor-fold desc="Properties">
                "{\n" +
                "  \"aperio.Left\": \"25.691574\",\n" +
                "  \"aperio.LineAreaYOffset\": \"-0.000313\",\n" +
                "  \"aperio.User\": \"b414003d-95c6-48b0-9369-8010ed517ba7\",\n" +
                "  \"aperio.Focus Offset\": \"0.000000\",\n" +
                "  \"aperio.Top\": \"23.449873\",\n" +
                "  \"openslide.level[0].tile-height\": \"240\",\n" +
                "  \"aperio.Time\": \"09:59:15\",\n" +
                "  \"aperio.ImageID\": \"1004486\",\n" +
                "  \"openslide.level[0].height\": \"2967\",\n" +
                "  \"aperio.Parmset\": \"USM Filter\",\n" +
                "  \"aperio.AppMag\": \"20\",\n" +
                "  \"tiff.ResolutionUnit\": \"inch\",\n" +
                "  \"openslide.objective-power\": \"20\",\n" +
                "  \"openslide.level[0].downsample\": \"1\",\n" +
                "  \"openslide.quickhash-1\": \"6335ea0e6cc54c2cba64bb265d3c713a50cd84484924e3a9c109558c13521d5c\",\n" +
                "  \"aperio.MPP\": \"0.4990\",\n" +
                "  \"openslide.level[0].width\": \"2220\",\n" +
                "  \"aperio.Originalheight\": \"33014\",\n" +
                "  \"openslide.level[0].tile-width\": \"240\",\n" +
                "  \"openslide.comment\": \"Aperio Image Library v11.2.1 \\r\\n46000x32914 [42673,5576 2220x2967] (240x240) JPEG/RGB Q\\u003d30;Aperio Image Library v10.0.51\\r\\n46920x33014 [0,100 46000x32914] (256x256) JPEG/RGB Q\\u003d30|AppMag \\u003d 20|StripeWidth \\u003d 2040|ScanScope ID \\u003d CPAPERIOCS|Filename \\u003d CMU-1|Date \\u003d 12/29/09|Time \\u003d 09:59:15|User \\u003d b414003d-95c6-48b0-9369-8010ed517ba7|Parmset \\u003d USM Filter|MPP \\u003d 0.4990|Left \\u003d 25.691574|Top \\u003d 23.449873|LineCameraSkew \\u003d -0.000424|LineAreaXOffset \\u003d 0.019265|LineAreaYOffset \\u003d -0.000313|Focus Offset \\u003d 0.000000|ImageID \\u003d 1004486|OriginalWidth \\u003d 46920|Originalheight \\u003d 33014|Filtered \\u003d 5|OriginalWidth \\u003d 46000|OriginalHeight \\u003d 32914\",\n" +
                "  \"aperio.LineAreaXOffset\": \"0.019265\",\n" +
                "  \"aperio.OriginalHeight\": \"32914\",\n" +
                "  \"openslide.mpp-x\": \"0.499\",\n" +
                "  \"aperio.Filename\": \"CMU-1\",\n" +
                "  \"aperio.Filtered\": \"5\",\n" +
                "  \"openslide.mpp-y\": \"0.499\",\n" +
                "  \"openslide.vendor\": \"aperio\",\n" +
                "  \"tiff.ImageDescription\": \"Aperio Image Library v11.2.1 \\r\\n46000x32914 [42673,5576 2220x2967] (240x240) JPEG/RGB Q\\u003d30;Aperio Image Library v10.0.51\\r\\n46920x33014 [0,100 46000x32914] (256x256) JPEG/RGB Q\\u003d30|AppMag \\u003d 20|StripeWidth \\u003d 2040|ScanScope ID \\u003d CPAPERIOCS|Filename \\u003d CMU-1|Date \\u003d 12/29/09|Time \\u003d 09:59:15|User \\u003d b414003d-95c6-48b0-9369-8010ed517ba7|Parmset \\u003d USM Filter|MPP \\u003d 0.4990|Left \\u003d 25.691574|Top \\u003d 23.449873|LineCameraSkew \\u003d -0.000424|LineAreaXOffset \\u003d 0.019265|LineAreaYOffset \\u003d -0.000313|Focus Offset \\u003d 0.000000|ImageID \\u003d 1004486|OriginalWidth \\u003d 46920|Originalheight \\u003d 33014|Filtered \\u003d 5|OriginalWidth \\u003d 46000|OriginalHeight \\u003d 32914\",\n" +
                "  \"aperio.StripeWidth\": \"2040\",\n" +
                "  \"aperio.ScanScope ID\": \"CPAPERIOCS\",\n" +
                "  \"aperio.OriginalWidth\": \"46000\",\n" +
                "  \"aperio.Date\": \"12/29/09\",\n" +
                "  \"openslide.level-count\": \"1\",\n" +
                "  \"aperio.LineCameraSkew\": \"-0.000424\"\n" +
                "}"
            //</editor-fold>
        );
    }

    @Test
    public void GetSlidePropertiesFailure() {
        HttpResponse response = Unirest.get("http://localhost:7000/api/v0/properties/Fail").asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    public void FetchWorkspaceSuccess() {
        HttpResponse response = Unirest.get("http://localhost:7000/api/v0/download_workspace").asString();

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
        HttpResponse response = Unirest.get("http://localhost:7000/api/v0/download_project/Test").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("application/zip");
    }

    @Test
    public void DownloadProjectFailure() {
        HttpResponse response = Unirest.get("http://localhost:7000/api/v0/download_project/Fail").asString();

        assertThat(response.getStatus()).isEqualTo(404);
    }
}
