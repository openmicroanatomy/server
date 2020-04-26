package fi.ylihallila.server.controllers;

import com.google.gson.Gson;
import fi.ylihallila.server.Configuration;
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
import java.util.List;
import java.util.UUID;

public class BasicController {

	private Logger logger = LoggerFactory.getLogger(BasicController.class);

	private Path workspaceFile = Path.of(Configuration.WORKSPACE_FILE);

	protected void saveWorkspace(Object object) throws IOException {
		Files.write(workspaceFile, new Gson().toJson(object).getBytes());
	}

	protected Path getWorkspaceFile() {
		return workspaceFile;
	}

	protected String generateUUID() {
		return UUID.randomUUID().toString();
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

	/**
	 *
	 * @param projectName
	 * @return
	 */
	protected String getProjectFile(String projectName) {
		return String.format(Configuration.PROJECT_FILE_FORMAT, projectName);
	}

	protected String getBackupFile(String fileName) {
		return String.format(Configuration.BACKUP_FILE_FORMAT, fileName, System.currentTimeMillis());
	}

	/**
	 * Creates a mutable ArrayList from array
	 * @return ArrayList of workspace
	 * @throws IOException When unable to read workspace file
	 */
	protected List<Workspace> getWorkspaces() throws IOException {
		return new ArrayList<>(
			Arrays.asList(new Gson().fromJson(Files.readString(workspaceFile), Workspace[].class)
		));
	}
}
