package fi.ylihallila.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.ylihallila.server.models.Backup;
import fi.ylihallila.server.repositories.Repos;
import fi.ylihallila.server.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

//        Backup previousBackup = backups.get(backups.size() - 1);
//        String previousBackupHash = DigestUtils.sha1Hex(Files.readAllBytes(previousBackup.getFilepath()));
//        String newBackupHash = DigestUtils.sha1Hex(Files.readAllBytes(filePath));
//
//        if (previousBackupHash.equals(newBackupHash)) { // TODO: Doesn't work: 1) ZipUtil broken. 2) Gives IndexOutOfBounds when no previous backups
//            logger.debug("Abort creating backup. New version identical to previous.");
//            return;
//        }

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
     * List of known Microsoft Azure AD GUIDs mapped to their respective names.
     */
    private static final Map<String, String> knownTenants = Map.of(
    "9f9ce49a-5101-4aa3-8c75-0d5935ad6525", "University of Oulu",
    "3f66cfe2-34d7-4783-b684-b6ff0a66b9b9", "University of Jogador",
    "a1d90ab7-88d9-49cd-bb61-7661d3371ccb", "University of Putsnik"
    );

    public static Map<String, String> getKnownTenants() {
        return knownTenants;
    }

    /**
     * Cache of human readable formats for IDs.
     *
     * @beta WeakHashMaps idea is to periodically refresh the cache (organization or users
     * name might occasionally change)
     */
    private static WeakHashMap<String, String> cache = new WeakHashMap<>();

    /**
     * Tries to get a human readable version of a ID. The ID can represent: Users, Slides,
     * Workspaces, Projects or Backups.
     * @param id UUID of organization, project, user or workspace.
     * @return Human readable name or Optional.empty(); if unable to find.
     */
    public static Optional<String> getHumanReadableName(String id) {
        if (knownTenants.containsKey(id)) {
            return Optional.of(knownTenants.get(id));
        }

        if (cache.containsKey(id)) {
            return Optional.of(cache.get(id));
        }

        if (Repos.getProjectRepo().contains(id)) {
            return cacheAndReturn(id, Repos.getProjectRepo().getById(id).get().getName());
        }

        if (Repos.getUserRepo().contains(id)) {
            return cacheAndReturn(id, Repos.getUserRepo().getById(id).get().getName());
        }

        if (Repos.getWorkspaceRepo().contains(id)) {
            return cacheAndReturn(id, Repos.getWorkspaceRepo().getById(id).get().getName());
        }

        return Optional.empty();
    }


    private static Optional<String> cacheAndReturn(String id, String name) {
        cache.put(id, name);

        return Optional.of(name);
    }
}
