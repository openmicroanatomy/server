package fi.ylihallila.server.controllers;

import com.google.gson.Gson;
import fi.ylihallila.server.models.*;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.util.Util;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.commons.Roles;
import io.javalin.core.validation.Validator;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public class Controller {

	private final Logger logger = LoggerFactory.getLogger(Controller.class);

	private final int BUFFER = 1024;

	protected void saveAndBackup(Path path, Object object) throws IOException {
		save(path, object);
		backup(path);
	}

	protected void save(Path path, Object object) throws IOException {
		Files.write(path, new Gson().toJson(object).getBytes());
	}

	protected void copyInputStreamToFile(InputStream is, File file) throws IOException {
		try (FileOutputStream os = new FileOutputStream(file)) {
			int read;
			byte[] bytes = new byte[BUFFER];

			while ((read = is.read(bytes)) != -1) {
				os.write(bytes, 0, read);
			}
		}
	}

	protected void backupAndDelete(String pathToFile) {
		backupAndDelete(Path.of(pathToFile));
	}

	protected void backupAndDelete(Path pathToFile) {
		try {
			Util.backup(pathToFile);
			Files.delete(pathToFile);
		} catch (IOException e) {
			logger.error("Error while deleting / backing up file {}", pathToFile, e);
		}
	}

	protected void backup(String pathToFile) {
		backup(Path.of(pathToFile));
	}

	protected void backup(Path pathToFile) {
		try {
			Util.backup(pathToFile);
		} catch (IOException e) {
			logger.error("Error while creating a backup of {}", pathToFile, e);
		}
	}

	protected String getProjectFile(String projectId) {
		return String.format(Constants.PROJECT_FILE_FORMAT, projectId);
	}

	protected String getBackupFile(String filename) {
		return String.format(Constants.BACKUP_FILE_FORMAT, filename, System.currentTimeMillis());
	}

	protected String getBackupFile(String filename, String timestamp) {
		return String.format(Constants.BACKUP_FILE_FORMAT, filename, timestamp);
	}

	/**
	 * Wrapper for Javalin validator, as it only supports defaultValue of strings. This implementation supports
	 * any data type for defaultValue.
	 */
	protected <T> T validate(Context ctx, String key, Class<T> clazz, T defaultValue) {
		if (ctx.formParamMap().containsKey(key)) {
			Validator<T> validator = ctx.formParam(key, clazz);

			if (validator.isValid()) {
				return validator.get();
			}
		}

		return defaultValue;
	}

	/**
	 * Check whether current user has any of the specified roles. If the user has Roles.ADMIN the user will be
	 * allowed regardless if the user actually has the given role.
	 * @param ctx request context.
	 * @param roles allowed roles.
	 * @throws UnauthorizedResponse if user does not have any of the specified roles.
	 */
	protected void Allow(Context ctx, Roles... roles) {
		if (Arrays.stream(roles).allMatch(role -> role.equals(Roles.ANYONE))) {
			return;
		}

		if (!Authenticator.hasRoles(ctx, Set.of(roles))) {
			throw new UnauthorizedResponse();
		}
	}

	protected boolean isAdmin(User user) {
		return user.hasRole(Roles.ADMIN);
	}

	/**
	 * Check whether user has permissions to make changes (=write) to a given workspace.
	 * If the user has Roles.ADMIN this will always return true.
	 * @param ctx request context.
	 * @param id workspace id.
	 * @return true if user has permission to make changes to given workspace.
	 */
	public boolean hasWritePermission(Context ctx, String id) {
		User user = Authenticator.getUser(ctx);

		if (isAdmin(user)) {
			return true;
		}

		Session session = ctx.use(Session.class);

		Optional<Workspace> workspace = getWorkspaceById(session, id);
		return workspace.map(w -> w.hasWritePermission(user)).orElse(false);
	}

	/**
	 * Check whether user has permissions to access (=read) a given workspace.
	 * If the user has Roles.ADMIN this will always return true.
	 * @param ctx request context.
	 * @param id workspace id.
	 * @return true if user has permission to read given workspace.
	 */
	public boolean hasReadPermission(Context ctx, String id) {
		User user = Authenticator.getUser(ctx);

		if (isAdmin(user)) {
			return true;
		}

		Session session = ctx.use(Session.class);

		Optional<Workspace> workspace = getWorkspaceById(session, id);
		return workspace.map(w -> w.hasReadPermission(user)).orElse(false);
	}

	/**
	 * Tries to fetch a workspace associated to an ID. The ID can represent a workspace, subject or a project.
	 */
	private Optional<Workspace> getWorkspaceById(Session session, String id) {
		Workspace workspace = session.find(Workspace.class, id);
		if (workspace != null) {
			return Optional.of(workspace);
		}

		Subject subject = session.find(Subject.class, id);
		if (subject != null) {
			return Optional.of(subject.getWorkspace());
		}

		Project project = session.find(Project.class, id);
		if (project != null) {
			return Optional.of(project.getSubject().getWorkspace());
		}

		return Optional.empty();
	}

	/**
	 * Check whether given InputStream is an image by passing it to ImageIO.read().
	 * @param is InputStream to read from.
	 * @return true if InputStream represents an image.
	 */
	public boolean isImage(@NotNull InputStream is ){
		try {
			ImageIO.read(is);

			return true;
		} catch (Exception ignored) {}

		return false;
	}

	/**
	 * Checks that provided InputStream is an image by running it through {@link ImageIO#read(InputStream)}
	 * and validates that the image width is >= 400px, height is >= 80px and aspect ratio is 5:1.
	 *
	 * @param is InputStream to read from.
	 * @return true if a valid organization logo.
	 */
	public boolean isValidOrganizationLogo(@NotNull InputStream is) {
		try {
			BufferedImage image = ImageIO.read(is);

			if (image == null) {
				return false;
			}

			return image.getColorModel().hasAlpha() &&
					image.getWidth() >= 400 && image.getHeight() >= 80 && getImageAspectRatio(image) == 5.0;
		} catch (Exception ignored) {}

		return false;
	}

	private double getImageAspectRatio(BufferedImage image) {
		return (double) image.getWidth() / image.getHeight();
	}
}
