package fi.ylihallila.server.controllers;

import com.google.gson.Gson;
import fi.ylihallila.server.Config;
import fi.ylihallila.server.gson.Slide;
import fi.ylihallila.server.gson.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class BasicController {

	private Logger logger = LoggerFactory.getLogger(BasicController.class);

	private Path workspaceFile = Path.of(Config.WORKSPACE_FILE);

	protected void saveAndBackup(Path path, Object object) throws IOException {
		save(path, object);
		backup(path);
	}

	protected void save(Path path, Object object) throws IOException {
		Files.write(path, new Gson().toJson(object).getBytes());
	}

	protected Path getWorkspaceFile() {
		return workspaceFile;
	}

	private int BUFFER = 1024;

	protected void copyInputStreamToFile(InputStream is, File file) throws IOException {
		try (FileOutputStream os = new FileOutputStream(file)) {
			int read;
			byte[] bytes = new byte[BUFFER];

			while ((read = is.read(bytes)) != -1) {
				os.write(bytes, 0, read);
			}
		}
	}

	protected void delete(String pathToFile) {
		delete(Path.of(pathToFile));
	}

	protected void delete(Path pathToFile) {
		try {
			backup(pathToFile);
			Files.delete(pathToFile);
		} catch (IOException e) {
			logger.error("Error while deleting file", e);
		}
	}

	protected void backup(String pathToFile) {
		backup(Path.of(pathToFile));
	}

	protected void backup(Path pathToFile) {
		try {
			String fileName = pathToFile.toFile().getName();
			Files.copy(pathToFile, Path.of(getBackupFile(fileName)));
		} catch (IOException e) {
			logger.error("Error while backing up file", e);
		}
	}

	protected String getProjectFile(String projectName) {
		return String.format(Config.PROJECT_FILE_FORMAT, projectName);
	}

	protected String getBackupFile(String fileName) {
		return String.format(Config.BACKUP_FILE_FORMAT, fileName, System.currentTimeMillis());
	}

	/**
	 * Creates a mutable ArrayList from array
	 * @return ArrayList of workspaces
	 * @throws IOException When unable to read workspace file
	 */
	protected ArrayList<Workspace> getWorkspaces() throws IOException {
		return new ArrayList<>(
			Arrays.asList(new Gson().fromJson(Files.readString(Path.of(Config.WORKSPACE_FILE)), Workspace[].class)
		));
	}

	/**
	 * Creates a mutable ArrayList from array
	 * @return ArrayList of slides
	 * @throws IOException When unable to read slide file
	 */
	protected ArrayList<Slide> getSlides() throws IOException {
		return new ArrayList<>(
			Arrays.asList(new Gson().fromJson(Files.readString(Path.of(Config.SLIDES_FILE)), Slide[].class)
		));
	}
}
