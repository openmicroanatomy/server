package fi.ylihallila.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.ylihallila.server.models.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

    private final static Logger logger = LoggerFactory.getLogger(Util.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * ObjectMapper used everywhere in the application. Using this is advised, as
     * constructing new instances of ObjectMapper is resource consuming.
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Creates a backup of given filePath. A backup is created only when 10 minutes have passed
     * from the previous backup.
     * @param filePath Path of file to backup.
     */
    public synchronized static void backup(Path filePath) throws IOException {
        logger.debug("Creating backup of {}", filePath);

        String fileName = filePath.getFileName().toString();
        List<Backup> backups = getBackups(backup -> backup.getFilename().equalsIgnoreCase(fileName));

        if (backups.size() > 1) {
            Backup previousBackup = backups.get(backups.size() - 1);
            String previousBackupHash = DigestUtils.sha1Hex(Files.readAllBytes(previousBackup.getFilepath()));
            String newBackupHash = DigestUtils.sha1Hex(Files.readAllBytes(filePath));

            if (previousBackupHash.equals(newBackupHash)) {
                logger.debug("Abort creating backup. New version identical to previous.");
                return;
            } else {
                logger.debug("Different");
            }
        }

        Files.copy(
            filePath,
            Path.of(String.format(Constants.BACKUP_FILE_FORMAT, fileName, System.currentTimeMillis()))
        );

        logger.debug("Backup created.");
    }

    /**
     * Returns a list of all backups. An additional filter can be provided.
     *
     * @param filter Filter or null.
     * @return List of backups or an empty list.
     * @throws IOException if an I/O error occurs
     */
    public synchronized static List<Backup> getBackups(Predicate<? super Backup> filter) throws IOException {
        Stream<Path> files = Files.list(Path.of(Constants.BACKUP_FOLDER));

        List<Backup> backups = new ArrayList<>();

        files.forEach(file -> {
            try {
                String[] split = file.getFileName().toString().split("@");

                if (split.length == 2) {
                    backups.add(new Backup(split[0], Long.parseLong(split[1])));
                }
            } catch (Exception ignored) {}
        });

        if (filter == null) {
            return backups;
        }

        return backups.stream().filter(filter).sorted(Comparator.comparingLong(Backup::getTimestamp)).collect(Collectors.toList());
    }

    /**
     * Cache of human readable formats for IDs.
     *
     * @beta WeakHashMaps idea is to periodically refresh the cache (organization or users
     * name might occasionally change)
     */
    private static WeakHashMap<String, String> cache = new WeakHashMap<>();

    /**
     * TODO: Only supports projects currently
     *
     * Tries to get a human readable version of a ID. The ID can represent: Users, Slides,
     * Workspaces, Projects or Backups.
     *
     * @param id UUID of organization, project, user or workspace.
     * @return Human readable name or Optional.empty(); if unable to find.
     */
    public static Optional<String> getHumanReadableName(String id) {
        if (cache.containsKey(id)) {
            return Optional.of(cache.get(id));
        }

        Session session = Database.openSession();
        session.beginTransaction();

        Project project = session.find(Project.class, id);

        session.getTransaction().commit();
        session.close();

        if (project == null) {
            return Optional.empty();
        } else {
            cache.put(id, project.getName());
            return Optional.of(project.getName());
        }
    }

    /**
     * Gets an organization object for given ID. If can't find one, it will create one.
     *
     * @param id ID of the organization
     * @return Organization
     */
    public static Organization getOrganization(String id) {
        Session session = Database.openSession();
        session.beginTransaction();

        Organization organization = session.find(Organization.class, id);

        if (organization == null) {
            organization = new Organization();
            organization.setId(id);
            organization.setName("Unknown Organization");
            session.save(organization);
        }

        session.getTransaction().commit();
        session.close();

        return organization;
    }

    public static String getResourceFileAsString(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            if (is == null) return null;
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}
