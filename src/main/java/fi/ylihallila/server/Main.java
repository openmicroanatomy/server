package fi.ylihallila.server;

import com.google.gson.Gson;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.generators.PropertiesGenerator;
import fi.ylihallila.server.generators.TileGenerator;
import fi.ylihallila.server.generators.Tiler;
import fi.ylihallila.server.models.Organization;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.*;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.Scanner;
import java.util.UUID;

import org.flywaydb.core.Flyway;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static Application app;

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 1 && args[0].equalsIgnoreCase("--tiler")) {
            new Tiler();
        } else if (args.length == 1 && args[0].equalsIgnoreCase("--debug")) {
            new SimpleDebugger();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("--generate")) {
            new TileGenerator(args[1]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("--properties")) {
            new PropertiesGenerator(args[1]);
        } else {
            CommandLineParser parser = new CommandLineParser(args);

            Constants.SERVER_PORT = parser.hasArgument("port") ? parser.getInt("port") : 7777; // Default port 7777
            Constants.ENABLE_SSL  = parser.hasFlag("secure");

            preflight();

            try {
                app = new Application();

                readInput();

            } catch (Exception e) {
                if (e.getCause() != null && e.getCause().getClass() == IllegalStateException.class) {
                    logger.info("Add a valid keystore to launch the server in secure mode.");
                }

                logger.error("Error while launching server", e);
            }
        }
    }

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
        createInitialOrganizationIfOneDoesNotExist();
        createAdministratorAccountIfOneDoesNotExist();
    }

    /**
     * Create all the necessary directories.
     */
    private static void createDirectories() {
        try {
            Files.createDirectories(Path.of("projects"));
            Files.createDirectories(Path.of("slides"));
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
     * Copies the reference configuration file from the .jar file.
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
     * This must be run <b>before</b> Hibernate is instantiated due to how Flyway operates..
     */
    private static void migrateDatabase() {
        Flyway.configure()
              .dataSource("jdbc:h2:./database;DB_CLOSE_DELAY=-1", "sa", null) // Use the same as in hibernate.cfg.xml
              .load()
              .migrate();
    }

    /**
     * Validates that Hibernate has a valid database connection.
     */
    private static void checkDatabaseConnection() {
        try {
            Database.openSession().close();
        } catch (Exception e) {
            logger.error("Error while opening database connection -- cannot continue, exiting", e);
            System.exit(0);
        }
    }

    private static void createInitialOrganizationIfOneDoesNotExist() {
        Session session = Database.openSession();
        session.beginTransaction();

        long count = (long) session.createQuery("SELECT COUNT (*) FROM Organization").getSingleResult();

        if (count > 0) return;

        System.out.println("================================================================");
        System.out.println("No organizations exist, creating a new one ...");
        System.out.println("If you believe this is an error: stop the server and confirm");
        System.out.println("that the database file exists, is readable and is not corrupted.");
        System.out.println("================================================================");

        /* Organization name */

        Scanner scanner = new Scanner(System.in);

        System.out.print("Organization name: ");
        String name = scanner.nextLine();

        String confirm;

        do {
            System.out.println("Selected name: " + name);
            System.out.print("Type 'yes' to confirm and 'no' to re-enter: ");

            confirm = scanner.nextLine();

            if (confirm.equals("no")) {
                System.out.print("New organization name: ");
                name = scanner.nextLine();
            }
        } while (!confirm.equals("yes"));

        try {
            Organization organization = new Organization();
            organization.setId(UUID.randomUUID());
            organization.setName(name);

            session.save(organization);

            session.getTransaction().commit();
            session.close();

            System.out.println("================================================================");
            System.out.printf("Organization created [%s (%s)]%n", organization.getName(), organization.getId());
            System.out.println("================================================================");
        } catch (Exception e) {
            System.err.println("Error while creating organization -- cannot continue, exiting");
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void createAdministratorAccountIfOneDoesNotExist() {
        if (Files.exists(Path.of(Constants.ADMINISTRATORS_FILE))) {
            return;
        }

        System.out.println("================================================================");
        System.out.println("Missing administrators file, creating a new one ...");
        System.out.println("If you believe this is an error, then stop the server and");
        System.out.println("confirm that the administrators.json file exist and is readable.");
        System.out.println("================================================================");

        /* User details */

        Scanner scanner = new Scanner(System.in);

        System.out.print("Email: ");
        String email = scanner.nextLine();

        System.out.print("Name: ");
        String name = scanner.nextLine();

        String password, repeatPassword;

        do {
            System.out.print("Password: ");
            password = scanner.nextLine();

            System.out.print("Repeat password: ");
            repeatPassword = scanner.nextLine();

            if (!(password.equals(repeatPassword))) {
                System.err.print("Passwords do not match, try again ...");
            }
        } while (!(password.equals(repeatPassword)));

        try {
            Session session = Database.openSession();
            session.beginTransaction();

            // Get the first organization inside the Organizations table, which we just created.
            Organization organization = session.createQuery("from Organization", Organization.class)
                    .setMaxResults(1)
                    .getSingleResult();

            User user = new User();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setName(name);
            user.hashPassword(password);
            user.setOrganization(organization);
            user.setRoles(Set.of(Roles.ADMIN));

            session.save(user);

            session.getTransaction().commit();
            session.close();

            Files.writeString(
                Path.of(Constants.ADMINISTRATORS_FILE),
                new Gson().toJson(List.of(user))
            );

            System.out.println("================================================================");
            System.out.println("Administrator account created; assigned to " + organization.getName());
            System.out.println("================================================================");
        } catch (Exception e) {
            System.err.println("Error while creating administrator account -- cannot continue, exiting");
            e.printStackTrace();
            System.exit(0);
        }
    }
}
