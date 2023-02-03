package fi.ylihallila.server.controllers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.ylihallila.server.util.Database;
import io.javalin.http.Context;
import io.javalin.plugin.rendering.vue.JavalinVue;
import io.javalin.plugin.rendering.vue.VueComponent;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.Session;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public class DashboardController extends Controller {

    public void index(Context ctx) {
        if (isServerSetup()) {
            dashboard(ctx);
        } else {
            setup(ctx);
        }
    }

    /**
     * Check whether the server is ready to be used i.e. an (administrator) user and an organization exists.
     * @return true if ready to be used for production.
     */
    private boolean isServerSetup() {
        Session session = Database.openSession();
        session.beginTransaction();

        long organizations = (long) session.createQuery("SELECT COUNT (*) FROM Organization").getSingleResult();
        long users = (long) session.createQuery("SELECT COUNT (*) FROM User").getSingleResult();

        session.getTransaction().commit();
        session.close();

        return organizations > 0 && users > 0;
    }

    private void setup(Context ctx) {
        new VueComponent("setup").handle(ctx);
    }

    private void dashboard(Context ctx) {
        JavalinVue.stateFunction = c -> new ObjectMapper().convertValue(getServerState(), Map.class);
        new VueComponent("dashboard").handle(ctx);
    }

    /* Private API */

    /**
     * Collects information about the server state from different sources into one location.
     * @return a {@link ServerState} object
     */
    private ServerState getServerState() {
        ServerState state = new ServerState();

        Path root = new File(System.getProperty("user.dir", ".")).toPath().getRoot();

        if (root != null) {
            File diskPartition = root.toFile();

            state.DiskSpaceFree = diskPartition.getFreeSpace();
            state.DiskTotalSpace = diskPartition.getTotalSpace();
        }

        state.RAMTotal = Runtime.getRuntime().maxMemory();
        state.RAMUsage = Runtime.getRuntime().totalMemory();

        state.CPUs = Runtime.getRuntime().availableProcessors();

        Session session = Database.openSession();
        session.beginTransaction();

        state.Users         = (long) session.createQuery("SELECT COUNT (*) FROM User").getSingleResult();
        state.Organizations = (long) session.createQuery("SELECT COUNT (*) FROM Organization").getSingleResult();
        state.Workspaces    = (long) session.createQuery("SELECT COUNT (*) FROM Workspace").getSingleResult();
        state.Courses       = (long) session.createQuery("SELECT COUNT (*) FROM Subject").getSingleResult();
        state.Lessons       = (long) session.createQuery("SELECT COUNT (*) FROM Project").getSingleResult();
        state.Slides        = (long) session.createQuery("SELECT COUNT (*) FROM Slide").getSingleResult();

        session.getTransaction().commit();
        session.close();

        return state;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
    private static class ServerState {

        @JsonProperty
        public long DiskSpaceFree;

        @JsonProperty
        public long DiskTotalSpace;

        @JsonProperty
        public long RAMUsage;

        @JsonProperty
        public long RAMTotal;

        @JsonProperty
        public int CPUs;

        @JsonProperty
        public long Users;

        @JsonProperty
        public long Organizations;

        @JsonProperty
        public long Workspaces;

        @JsonProperty
        public long Courses;

        @JsonProperty
        public long Lessons;

        @JsonProperty
        public long Slides;

    }
}
