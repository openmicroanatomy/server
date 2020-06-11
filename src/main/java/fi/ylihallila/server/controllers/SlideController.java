package fi.ylihallila.server.controllers;

import com.google.gson.Gson;
import fi.ylihallila.server.Configuration;
import fi.ylihallila.server.OpenSlideCache;
import fi.ylihallila.server.gson.Slide;
import io.javalin.http.Context;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
			Files.readString(Path.of(Configuration.SLIDES_FILE)),
			Slide[].class
		));

		List<HashMap<String, Object>> slidesWithProperties = slides.stream().map(slide -> {
			HashMap<String, Object> data = new HashMap<>();
			data.put("name", slide.getName());
			data.put("id", slide.getId());
			data.put("organization", slide.getOwner());

			File slideProperties = new File(String.format(Configuration.SLIDE_PROPERTIES_FILE, slide.getId()));

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
		String fileName = ctx.queryParam("resumableFilename");
		String identifier = ctx.queryParam("resumableIdentifier");
		int totalSize = ctx.queryParam("resumableTotalSize", Integer.class).get();
		int index = ctx.queryParam("resumableChunkNumber", Integer.class).get() - 1;
		int buffer = ctx.queryParam("resumableChunkSize", Integer.class).get();

		logger.info(String.format("%s %s : %s / %s / %s", fileName, identifier, totalSize, index, buffer));

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
		} else if (ctx.method().equals("POST")) {
			byte[] data = ctx.uploadedFile("file").getContent().readAllBytes();
			logger.info("byte[] size: " + data.length);

			RandomAccessFile writer = new RandomAccessFile(fileName, "rw");
			writer.seek(index * buffer);
			writer.write(data);
			writer.close();
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
			saveAndBackup(Path.of(Configuration.SLIDES_FILE), slides);
			ctx.status(200);
		} else {
			ctx.status(404);
		}
	}

	public void deleteSlide(Context ctx) throws IOException {
		String slideId = ctx.pathParam("slide-id", String.class).get();

		List<Slide> slides = getSlides();
		var deleted = slides.removeIf(slide -> slide.getId().equalsIgnoreCase(slideId));

		if (deleted) {
			saveAndBackup(Path.of(Configuration.SLIDES_FILE), slides);
			ctx.status(200);
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

		logger.info(slideId);

		Path slideProperties = Path.of(String.format(Configuration.SLIDE_PROPERTIES_FILE, slideId));

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

		String fileName = String.format(Configuration.TILE_FILE_FORMAT, slide, tileX, tileY, level, tileWidth, tileHeight);
		InputStream is;

		if (Files.exists(Path.of(fileName), LinkOption.NOFOLLOW_LINKS)) {
			logger.info("Retrieving from disk [{}, {},{} / {} / {},{}]", fileName, tileX, tileY, level, tileWidth, tileHeight);

			FileInputStream fis = new FileInputStream(Path.of(fileName).toString());
			is = new ByteArrayInputStream(fis.readAllBytes());
			fis.close();

			ctx.status(200).contentType("image/jpg");
			ctx.result(is);
			is.close();
		} else {
			logger.info("Couldn't find tile [{}, {},{} / {} / {},{}]", fileName, tileX, tileY, level, tileWidth, tileHeight);
			ctx.status(404);
		}
	}

	private InputStream generateImage(String slide, int tileX, int tileY, int level, int tileWidth, int tileHeight, String fileName) throws IOException {
		Optional<OpenSlide> openSlide = OpenSlideCache.get(slide);
		if (openSlide.isEmpty()) {
			return InputStream.nullInputStream();
		}

		BufferedImage img = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB);
		int[] data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

		openSlide.get().paintRegionARGB(data, tileX, tileY, level, tileWidth, tileHeight);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(img, "jpg", os);
		InputStream is = new ByteArrayInputStream(os.toByteArray());

		Files.write(Path.of("tiles", fileName),
				os.toByteArray(),
				StandardOpenOption.WRITE);

		os.flush();

		return is;
	}
}
