package fi.ylihallila.server.controllers;

import fi.ylihallila.server.Application;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.exceptions.UnprocessableEntityResponse;
import fi.ylihallila.server.generators.Tiler;
import fi.ylihallila.server.models.Slide;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.util.OpenSlideCache;
import fi.ylihallila.server.util.Util;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.*;
import io.javalin.plugin.openapi.annotations.*;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SlideController extends Controller implements CrudHandler {

	private final Logger logger = LoggerFactory.getLogger(SlideController.class);

	@OpenApi(
		tags = { "slide" },
		summary = "Upload a chunk of a new slide. Slide will be created when all chunks are uploaded.",
		queryParams = {
			@OpenApiParam(name = "filename", required = true),
			@OpenApiParam(name = "fileSize", type = Long.class, required = true),
			@OpenApiParam(name = "chunkSize", type = Integer.class, required = true),
			@OpenApiParam(name = "chunk", type = Integer.class, required = true),
		},
		formParams = {
			@OpenApiFormParam(name = "file", required = true, type = File.class)
		},
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "422")
		}
	)
	@Override public void create(@NotNull Context ctx) {
		Allow(ctx, Roles.MANAGE_SLIDES);

		String fileName = ctx.queryParam("filename");
		long totalSize  = ctx.queryParam("fileSize", Long.class).get();
		long buffer     = ctx.queryParam("chunkSize", Integer.class).get();
		long index      = ctx.queryParam("chunk", Integer.class).get();

		UploadedFile file = ctx.uploadedFile("file");

		if (file == null) {
			throw new UnprocessableEntityResponse("Slide not provided");
		}

		try {
			logger.trace("Uploading slide chunk: {} [Size: {}, Buffer: {}, Index: {}]", fileName, totalSize, buffer, index);

			byte[] data = file.getContent().readAllBytes();

			RandomAccessFile writer = new RandomAccessFile(String.format(Constants.TEMP_FILE, fileName), "rw");
			writer.seek(index * buffer);
			writer.write(data, 0, data.length);
			writer.close();

			// The slide is fully uploaded we can start processing it.
			if (Files.size(Path.of(String.format(Constants.TEMP_FILE, fileName))) == totalSize) {
				processUploadedSlide(ctx, fileName);
			}

			ctx.status(200);
		} catch (IOException e) {
			logger.error("Error while generating tiles for slide", e);
			throw new InternalServerErrorResponse(e.getMessage());
		}
	}

	@OpenApi(
		tags = { "slide" },
		summary = "Delete given slide",
		pathParams = @OpenApiParam(
			name = "id",
			description = "UUID of slide to be deleted",
			required = true
		),
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404"),
		}
	)
	@Override public void delete(@NotNull Context ctx, @NotNull String id) {
		Allow(ctx, Roles.MANAGE_SLIDES);

		User user = Authenticator.getUser(ctx);
		Session session = ctx.use(Session.class);

		Slide slide = session.find(Slide.class, id);

		if (slide == null) {
			throw new NotFoundResponse();
		}

		if (!slide.hasPermission(user)) {
			throw new ForbiddenResponse();
		}

		session.delete(slide);

		Path propertiesPath = Path.of(String.format(Constants.SLIDE_PROPERTIES_FILE, id));
		backup(propertiesPath);

		try {
			Files.delete(propertiesPath);
		} catch (IOException e) {
			logger.warn("Could not delete properties file for {} [{}]", id, e);
		}

		ctx.status(200);

		logger.info("Slide {} deleted by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
	}

	@OpenApi(
		tags = { "slide" },
		queryParams = @OpenApiParam(
			name = "properties",
			description = "When true, includes properties for slides.",
			type = boolean.class
		),
		summary = "Get all slides with or without properties; to get individual slide properties use the <code>/slides/:id</code> endpoint.",
		responses = {
			@OpenApiResponse(status = "200", content = @OpenApiContent(from = Slide.class, isArray = true))
		}
	)
	@Override public void getAll(@NotNull Context ctx) {
		Session session = ctx.use(Session.class);
		User user = Authenticator.getUser(ctx);
		boolean withProperties = validate(ctx, "properties", boolean.class, false);

		if (!(user.hasRole(Roles.MODERATOR) || user.hasRole(Roles.MANAGE_SLIDES) || user.hasWriteAccessSomewhere())) {
			throw new UnauthorizedResponse("Listing slides requires write access to a workspace.");
		}

		List<Slide> slides = session.createQuery("from Slide", Slide.class).list();

		if (withProperties) {
			ctx.status(200).json(addPropertiesToSlides(slides));
		} else {
			ctx.status(200).json(slides);
		}
	}

	@OpenApi(
		tags = { "slide" },
		summary = "Get properties for specified slide",
		pathParams = {
			@OpenApiParam(name = "id", description = "UUID of slide to be fetched", required = true)
		},
		responses = {
			@OpenApiResponse(status = "200", content = @OpenApiContent(from = Slide.class))
		}
	)
	@Override public void getOne(@NotNull Context ctx, @NotNull String id) {
		if (ctx.queryParamMap().containsKey("openslide")) {
			getSlidePropertiesFromOpenslide(ctx, id);
		} else {
			getSlidePropertiesFromFile(ctx, id);
		}
	}

	@OpenApi(
		tags = { "slide" },
		summary = "Update given slide",
		formParams = {
			@OpenApiFormParam(name = "slide-name")
		},
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404"),
		}
	)
	@Override public void update(@NotNull Context ctx, @NotNull String id) {
		Allow(ctx, Roles.MANAGE_SLIDES);

		User user = Authenticator.getUser(ctx);
		Session session = ctx.use(Session.class);

		Slide slide = session.find(Slide.class, id);

		if (slide == null) {
			throw new NotFoundResponse();
		}

		if (!(slide.hasPermission(user))) {
			throw new ForbiddenResponse();
		}

		slide.setName(ctx.formParam("slide-name", slide.getName()));
		session.update(slide);

		ctx.status(200);

		logger.info("Slide {} ({}) edited by {} ({})", slide.getName(), id, user.getName(), user.getId());
	}

	@OpenApi(
		tags = { "slide" },
		summary = "Add slide to tiling queue",
		pathParams = {
			@OpenApiParam(name = "id", required = true)
		},
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404"),
		}
	)
	public void tile(@NotNull Context ctx) {
		String id = ctx.pathParam("id", String.class).get();
		Path path = Path.of(String.format(Constants.PENDING_SLIDES, id));

		User user = Authenticator.getUser(ctx);
		Session session = ctx.use(Session.class);

		Slide slide = session.find(Slide.class, id);

		if (slide == null) {
			throw new NotFoundResponse();
		}

		if (!(slide.hasPermission(user))) {
			throw new ForbiddenResponse();
		}

		if (!(path.toFile().exists())) {
			throw new UnprocessableEntityResponse("Original slide not found; try retrying later or re-uploading slide.");
		}

		Tiler tiler = Application.getInstance().getTiler();

		if (tiler.isAlreadyQueued(path)) {
			throw new UnprocessableEntityResponse("Slide already queued for processing.");
		}

		tiler.addSlideToTilerQueue(path);

		ctx.status(200).json(Map.of("Message", "Successfully added to tiling queue."));
	}

	/* Private API */

	private List<HashMap<String, Object>> addPropertiesToSlides(Collection<Slide> slides) {
		return slides.stream().map(slide -> {
			HashMap<String, Object> data = new HashMap<>();
			data.put("name", slide.getName());
			data.put("id", slide.getId());
			data.put("owner", slide.getOwner());

			File propertiesFile = new File(String.format(Constants.SLIDE_PROPERTIES_FILE, slide.getId()));

			if (propertiesFile.exists()) {
				try {
					data.put("properties", Util.getMapper().readValue(propertiesFile, Map.class));
				} catch (IOException e) {
					logger.error("Error while trying to get properties for slide {}", slide.getId(), e);
				}
			}

			return data;
		}).collect(Collectors.toList());
	}

	private void getSlidePropertiesFromFile(Context ctx, String id) {
		try {
			File propertiesFile = new File(String.format(Constants.SLIDE_PROPERTIES_FILE, id));

			if (propertiesFile.exists()) {
				ctx.status(200).json(Util.getMapper().readValue(propertiesFile, Map.class));
			} else {
				throw new NotFoundResponse();
			}
		} catch (IOException e) {
			logger.error("Error while reading slide properties from file", e);
			throw new InternalServerErrorResponse(e.getMessage());
		}
	}

	private void getSlidePropertiesFromOpenslide(Context ctx, String id) {
		try {
			Map<String, String> properties = OpenSlideCache.get(id).get().getProperties();

			ctx.status(200).json(properties); // TODO: Test
		} catch (Exception e) {
			logger.error("Error while reading slide properties using OpenSlide", e);
			throw new InternalServerErrorResponse(e.getMessage());
		}
	}

	private void processUploadedSlide(Context ctx, String slideName) throws IOException {
		Optional<OpenSlide> openSlide = OpenSlideCache.get(String.format(Constants.TEMP_FILE, slideName));

		String id = UUID.randomUUID().toString();
		User user = Authenticator.getUser(ctx);

		if (openSlide.isEmpty()) {
			logger.error("Error when processing uploaded file: Couldn't create OpenSlide instance."
				+ "\n" + "Possible solutions: file was corrupted during upload or the file isn't supported by OpenSlide");
			return;
		} else {
			logger.info("Processing slide {} ({}); uploaded by {} ({})", slideName, id, user.getName(), user.getId());
		}

		// Add slide to database
		Session session = ctx.use(Session.class);

		Slide slide = new Slide();
		slide.setName(slideName);
		slide.setId(id);
		slide.setOwner(user.getOrganization());
		slide.setTiled(false);

		session.save(slide);

		// Mark slide as pending tiling. See Tiler for further processing.
		Path pendingSlide = Files.move(
			Path.of(String.format(Constants.TEMP_FILE, slideName)),
			Path.of(String.format(Constants.PENDING_SLIDES, id))
		);

		Application.getInstance().getTiler().addSlideToTilerQueue(pendingSlide);
	}
}
