package fi.ylihallila.server.controllers;

import com.google.gson.Gson;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.OpenSlideCache;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.models.Slide;
import fi.ylihallila.server.models.User;
import io.javalin.http.Context;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SlideController extends BasicController {

	private Logger logger = LoggerFactory.getLogger(SlideController.class);

	public void getAllSlides(Context ctx) throws IOException {
		File directory = new File("slides");

		if (directory.isDirectory()) {
			String[] files = directory.list();
			ctx.status(200).json(files);
		} else {
			ctx.status(404);
		}
	}

	public void GetAllSlidesV2(Context ctx) throws IOException {
		File directory = new File("slides");

		if (!directory.isDirectory()) {
			ctx.status(404);
			return;
		}

		List<Slide> slides = List.of(new Gson().fromJson(
			Files.readString(Path.of(Constants.SLIDES_FILE)),
			Slide[].class
		));

		List<HashMap<String, Object>> slidesWithProperties = slides.stream().map(slide -> {
			HashMap<String, Object> data = new HashMap<>();
			data.put("name", slide.getName());
			data.put("id", slide.getId());
			data.put("owner", slide.getOwner());
			data.put("ownerReadable", slide.getOwnerReadable()); // todo: rename "owner" and "ownerId"

			File slideProperties = new File(String.format(Constants.SLIDE_PROPERTIES_FILE, slide.getId()));

			if (slideProperties.exists()) {
				try {
					data.put("parameters", new Gson().fromJson(
						Files.readString(slideProperties.toPath()),
						Map.class
					));
				} catch (IOException e) {
					logger.error("Error while trying to get slide properties", e);
					logger.error(slide.toString());
				}
			}

			return data;
		}).collect(Collectors.toList());

		ctx.json(slidesWithProperties).status(200);
	}

	public void upload(Context ctx) throws IOException {
		String fileName = ctx.queryParam("filename");
		int totalSize   = ctx.queryParam("fileSize", Integer.class).get();
		int buffer      = ctx.queryParam("chunkSize", Integer.class).get();
		int index       = ctx.queryParam("chunk", Integer.class).get();

		if (ctx.method().equals("GET")) { // todo: cleanup GET
			if (buffer > totalSize) {
				buffer = totalSize;
			}

			if (!Files.exists(Path.of(fileName))) {
				ctx.status(400);
				return;
			}

			RandomAccessFile reader = new RandomAccessFile(fileName, "r");
			reader.seek(index * buffer);
			int read = reader.skipBytes(buffer);

			if (read == buffer) {
				ctx.status(200);
			} else {
				ctx.status(400);
			}

			reader.close();
		} else if (ctx.method().equals("POST") && ctx.uploadedFile("file") != null) {
			byte[] data = ctx.uploadedFile("file").getContent().readAllBytes();

			RandomAccessFile writer = new RandomAccessFile(String.format(Constants.UPLOADED_FILE, fileName), "rw");

			writer.seek(index * buffer);
			writer.write(data, 0, data.length);
			writer.close();

			// The file is fully uploaded we can start processing it.
			if (Files.size(Path.of(String.format(Constants.UPLOADED_FILE, fileName))) == totalSize) {
				processUploadedSlide(ctx, fileName);
			}
		}
	}

	public void updateSlide(Context ctx) throws IOException {
		String slideId = ctx.pathParam("slide-id", String.class).get();

		List<Slide> slides = getSlides();
		var success = new AtomicBoolean(false);

		slides.forEach(slide -> {
			if (slide.getId().equalsIgnoreCase(slideId)) {
				slide.setName(ctx.formParam("slide-name", slide.getName()));
				success.set(true);
			}
		});

		if (success.get()) {
			saveAndBackup(Path.of(Constants.SLIDES_FILE), slides);
			ctx.status(200);

			logger.info("Slide {} edited by {}", slideId, Authenticator.getUsername(ctx).orElse("Unknown"));
		} else {
			ctx.status(404);
		}
	}

	public void deleteSlide(Context ctx) throws IOException {
		String slideId = ctx.pathParam("slide-id", String.class).get();

		List<Slide> slides = getSlides();
		var deleted = slides.removeIf(slide -> slide.getId().equalsIgnoreCase(slideId));

		if (deleted) {
			Path propertiesPath = Path.of(String.format(Constants.SLIDE_PROPERTIES_FILE, slideId));
			backup(propertiesPath);
			Files.delete(propertiesPath);

			saveAndBackup(Path.of(Constants.SLIDES_FILE), slides);
			ctx.status(200);

			logger.info("Slide {} deleted by {}", slideId, Authenticator.getUsername(ctx).orElse("Unknown"));
		} else {
			ctx.status(404);
		}
	}

	public void getSlideProperties(Context ctx) throws IOException {
		if (ctx.queryParam("openslide") != null) {
			getSlidePropertiesFromOpenslide(ctx);
		} else {
			getSlidePropertiesFromFile(ctx);
		}
	}

	private void getSlidePropertiesFromFile(Context ctx) {
		String slideId = ctx.pathParam("slide-id", String.class).get();

		Path slideProperties = Path.of(String.format(Constants.SLIDE_PROPERTIES_FILE, slideId));

		if (slideProperties.toFile().exists()) {
			try {
				ctx.status(200)
				   .contentType("application/json")
				   .result(Files.readString(slideProperties));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			ctx.status(404);
		}
	}

	private void getSlidePropertiesFromOpenslide(Context ctx) throws IOException {
		Map<String, String> properties = OpenSlideCache.get(ctx.pathParam("slide-name")).get().getProperties();

		String json = new Gson().toJson(properties);
		ctx.result(json);
	}

	public void renderTile(Context ctx) throws Exception {
		String slide   = ctx.pathParam("slide-id");
		int tileX      = ctx.pathParam("tileX", Integer.class).get();
		int tileY      = ctx.pathParam("tileY", Integer.class).get();
		int level      = ctx.pathParam("level", Integer.class).get();
		int tileWidth  = ctx.pathParam("tileWidth", Integer.class).get();
		int tileHeight = ctx.pathParam("tileHeight", Integer.class).get();

		String fileName = String.format(Constants.TILE_FILE_FORMAT, slide, tileX, tileY, level, tileWidth, tileHeight);

		if (Files.exists(Path.of(fileName), LinkOption.NOFOLLOW_LINKS)) {
			logger.info("Retrieving from disk [{}, {},{} / {} / {},{}]", fileName, tileX, tileY, level, tileWidth, tileHeight);

			FileInputStream fis = new FileInputStream(Path.of(fileName).toString());
			InputStream is = new ByteArrayInputStream(fis.readAllBytes());

			ctx.status(200).contentType("image/jpg");
			ctx.result(is);

			is.close();
			fis.close();
		} else {
			logger.info("Couldn't find tile [{}, {},{} / {} / {},{}]", fileName, tileX, tileY, level, tileWidth, tileHeight);
			ctx.status(404);
		}
	}

	private void processUploadedSlide(Context ctx, String slideName) throws IOException {
		Optional<OpenSlide> openSlide = OpenSlideCache.get(String.format(Constants.UPLOADED_FILE, slideName));
		User user = Authenticator.getUser(ctx);

		if (openSlide.isEmpty()) {
			logger.error("Error when processing uploaded file. Couldn't create OpenSlide instance. Perhaps the file was corrupted during upload or the file isn't supported by OpenSlide");
			return;
		}

		// Add slide to slides.json

		String id = UUID.randomUUID().toString();
		ArrayList<Slide> slides = getSlides();

		Slide slide = new Slide();
		slide.setName(slideName);
		slide.setId(id);
		slide.setOwner(user.getOrganizationId());

		slides.add(slide);

		saveAndBackup(Path.of(Constants.SLIDES_FILE), slides); // TODO: SlideRepository

		// Move slide to slides pending tiling. See Tiler for further processing.

		Files.move(
			Path.of(String.format(Constants.UPLOADED_FILE, slideName)),
			Path.of(String.format(Constants.PENDING_SLIDES, id))
		);
	}
}
