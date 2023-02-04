package fi.ylihallila.server;

import fi.ylihallila.server.generators.PropertiesGenerator;
import fi.ylihallila.server.generators.TileGenerator;
import fi.ylihallila.server.generators.Tiler;
import fi.ylihallila.server.util.*;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.flywaydb.core.Flyway;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static Application app;

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 1 && args[0].equalsIgnoreCase("--tiler")) {
            new Tiler();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("--generate")) {
            new TileGenerator(args[1]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("--properties")) {
            new PropertiesGenerator(args[1]);
        } else {
            CommandLineParser parser = new CommandLineParser(args);

            Constants.SERVER_PORT = parser.hasArgument("port") ? parser.getInt("port") : 7777; // Default port 7777

            preflight();

            try {
                app = new Application();

                // TODO: Disable input for Unit tests because otherwise tests never run, as the @BeforeAll method
                //       waits for main() to return but it never returns because it waits for input.
                if (!parser.hasFlag("test")) {
                    readInput();
                }
            } catch (Exception e) {
                if (e.getCause() != null && e.getCause().getClass() == IllegalStateException.class) {
                    logger.info("Add a valid keystore to launch the server in secure mode.");
                }

                logger.error("Error while launching server", e);
            }
        }
    }

    /**
     * Read input from terminal and process any valid commands.
     */
    private static void readInput() {
        Scanner in = new Scanner(System.in);

        while (in.hasNextLine()) {
            String input = in.nextLine();

            if (input.isBlank()) {
                continue;
            }

            if (input.equalsIgnoreCase("stop")) {
                app.stop();
            } else {
                logger.info("Unknown command: {}", input);
            }
        }
    }

    /**
     * A series of operations and checks before the server is ready to run.
     */
    private static void preflight() {
        createDirectories();
        createConfigurationFile();
        migrateDatabase();
        checkDatabaseConnection();
        checkIsServerInitialized();
    }

    /**
     * Create all the necessary directories. Exits if creation fails.
     */
    private static void createDirectories() {
        try {
            Files.createDirectories(Path.of("projects"));
            Files.createDirectories(Path.of("slides"));
            Files.createDirectories(Path.of("pending"));
            Files.createDirectories(Path.of("tiles"));
            Files.createDirectories(Path.of("backups"));
            Files.createDirectories(Path.of("temp"));
            Files.createDirectories(Path.of("logos"));
            Files.createDirectories(Path.of("uploads"));
        } catch (IOException e) {
            logger.error("Error while creating directories -- cannot continue, exiting", e);
            System.exit(0);
        }
    }

    /**
     * Copies the reference configuration file from the .jar file if one does not exist.
     */
    private static void createConfigurationFile() {
        Path config = Path.of(Constants.CONFIGURATION_FILE);

        if (Files.exists(config)) {
            return;
        }

        try (var reference = Main.class.getResourceAsStream("/reference.conf")) {
            Files.write(config, reference.readAllBytes());
        } catch (IOException | NullPointerException e) {
            logger.error("Error while creating configuration file -- cannot continue, exiting", e);
            System.exit(0);
        }
    }

    /**
     * Creates the database schema or updates it to the latest version.
     * This must be run <b>before</b> Hibernate is instantiated due to how Flyway operates.
     */
    private static void migrateDatabase() {
        Flyway.configure()
              .dataSource("jdbc:h2:./database;DB_CLOSE_DELAY=-1", "sa", null) // Use the same as in hibernate.cfg.xml
              .load()
              .migrate();
    }

    /**
     * Validates that Hibernate has a valid database connection. Exits if database connection fails.
     */
    private static void checkDatabaseConnection() {
        try {
            Database.openSession().close();
        } catch (Exception e) {
            logger.error("Error while opening database connection -- cannot continue, exiting", e);
            System.exit(0);
        }
    }

    /**
     * Checks that an organization and a user exists, which means that the server is ready to be used.
     */
    private static void checkIsServerInitialized() {
        Session session = Database.openSession();
        session.beginTransaction();

        long organizations = session.createQuery("SELECT COUNT(*) FROM Organization", Long.class).getSingleResult();
        long users = session.createQuery("SELECT COUNT(*) FROM User", Long.class).getSingleResult();

        session.getTransaction().commit();
        session.close();

        Constants.IS_SETUP = organizations > 0 && users > 0;

        if (!(Constants.IS_SETUP)) {
            System.out.println("========================================");
            System.out.println("Server not initialized! Open the dashboard to initialize server.");
            System.out.println("Go to https://openmicroanatomy.github.io/docs/ for more details.");
            System.out.println("========================================");
        }
    }
}
