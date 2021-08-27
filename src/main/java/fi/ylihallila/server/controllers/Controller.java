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
import java.util.Set;

public class Controller {

	private Logger logger = LoggerFactory.getLogger(Controller.class);

	protected void saveAndBackup(Path path, Object object) throws IOException {
		save(path, object);
		backup(path);
	}

	protected void save(Path path, Object object) throws IOException {
		Files.write(path, new Gson().toJson(object).getBytes());
	}

	private final int BUFFER = 1024;

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

	protected void Allow(Context ctx, Roles... roles) {
		if (Arrays.stream(roles).allMatch(role -> role.equals(Roles.ANYONE))) {
			return;
		}

		if (!Authenticator.hasRoles(ctx, Set.of(roles))) {
			throw new UnauthorizedResponse();
		}
	}

	/**
	 * Checks if the given Context (User) has write permissions to given object by an ID. An ID can represent either a
	 * workspace or a (personal) project.
	 *
	 * @param ctx User context
	 * @param id Workspace / project ID
	 * @return True if access to write (=modify), otherwise false.
	 */
	public boolean hasPermission(Context ctx, String id) {
		User user = Authenticator.getUser(ctx);
		Set<Roles> roles = user.getRoles();

		if (roles.contains(Roles.ADMIN)) {
			return true;
		}

		var hasPermission = false;

		Session session = ctx.use(Session.class);
		Project project = session.find(Project.class, id);

		if (project != null) {
			Owner owner = project.getSubject().getWorkspace().getOwner();

			if (owner.getId().equals(user.getId())
					&& roles.contains(Roles.MANAGE_PERSONAL_PROJECTS)) {
				 hasPermission = true;
			} else if (owner.getId().equals(user.getOrganization().getId())
					&& roles.contains(Roles.MANAGE_PROJECTS)) {
				hasPermission = true;
			}
		} else {
			Workspace workspace = session.find(Workspace.class, id);

			if (workspace == null) {
				return false;
			}

			if (workspace.getOwner().getId().equals(user.getOrganization().getId())
					&& roles.contains(Roles.MANAGE_PROJECTS)) {
				hasPermission = true;
			} else if (workspace.getOwner().getId().equals(user.getId())
					&& roles.contains(Roles.MANAGE_PERSONAL_PROJECTS)) {
				hasPermission = true;
			}
		}

		return hasPermission;
	}

	public boolean isImage(@NotNull InputStream is ){
		try {
			ImageIO.read(is);

			return true;
		} catch (Exception ignored) {}

		return false;
	}

	/**
	 * Tries to test if provided InputStream is a valid image by running it through {@link ImageIO#read(InputStream)}
	 * and checking that the image is of width >= 400px and height >= 80px.
	 *
	 * @param is InputStream of Image
	 * @return true if an valid image
	 */
	public boolean isValidOrganizationLogo(@NotNull InputStream is) {
		try {
			BufferedImage image = ImageIO.read(is);

			if (image == null) {
				return false;
			}

			return image.getType() == BufferedImage.TYPE_INT_ARGB
					&& image.getWidth() >= 400 && image.getHeight() >= 80;
		} catch (Exception ignored) {}

		return false;
	}
}
