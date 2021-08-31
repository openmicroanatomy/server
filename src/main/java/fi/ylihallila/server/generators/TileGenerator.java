package fi.ylihallila.server.generators;

import com.google.gson.GsonBuilder;
import fi.ylihallila.server.archivers.TileArchive;
import fi.ylihallila.server.archivers.ZipTileArchive;
import fi.ylihallila.server.storage.Allas;
import fi.ylihallila.server.storage.FlatFile;
import fi.ylihallila.server.storage.S3;
import fi.ylihallila.server.storage.StorageProvider;
import fi.ylihallila.server.util.Config;
import fi.ylihallila.server.util.Constants;
import org.apache.commons.compress.utils.FileNameUtils;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class TileGenerator {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final ForkJoinPool executor = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

	private final OpenSlide openSlide;

	/**
	 * How long should the tile generation process take for one slide.
	 *
	 * After the duration specified any remaining tiles are ignored and the generated tiles are uploaded.
	 */
	private final long TIMEOUT = Duration.ofMinutes(30).toMillis();

	static {
		ImageIO.setUseCache(false);
	}

	// TODO: Catch exceptions so generation can try and continue.

	public TileGenerator(String slideName) throws IOException, InterruptedException {
		this(new File(slideName));
	}

	public TileGenerator(Path path ) throws IOException, InterruptedException {
		this(path.toFile());
	}

	public TileGenerator(File slide) throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();

		openSlide = new OpenSlide(slide);

		String id = getOrGenerateUUID(FileNameUtils.getBaseName(slide.getName()));

		Color backgroundColor = getBackgroundColor();

		int slideHeight = readIntegerProperty("openslide.level[0].height");
		int slideWidth  = readIntegerProperty("openslide.level[0].width");
		int levels      = readIntegerProperty("openslide.level-count");

		int tileHeight = 1024; //readIntegerPropertyOrDefault("openslide.level[0].tile-height", 256);
		int tileWidth  = 1024; //readIntegerPropertyOrDefault("openslide.level[0].tile-width",  256);

		int boundsX = 0; //readIntegerPropertyOrDefault(OpenSlide.PROPERTY_NAME_BOUNDS_X, 0);
		int boundsY = 0; //readIntegerPropertyOrDefault(OpenSlide.PROPERTY_NAME_BOUNDS_Y, 0);

		int boundsHeight = 0; //readIntegerPropertyOrDefault(OpenSlide.PROPERTY_NAME_BOUNDS_WIDTH, slideHeight);
		int boundsWidth  = 0; //readIntegerPropertyOrDefault(OpenSlide.PROPERTY_NAME_BOUNDS_HEIGHT, slideWidth);

		// These multipliers are used to calculate the levelHeight and levelWidth
		// as only the level 0 boundHeight and boundWidth is known.
		double boundsYMultiplier = 1;
		double boundsXMultiplier = 1;

		if (boundsHeight > 0 && boundsWidth > 0) {
			boundsYMultiplier = 1.0 * boundsHeight / slideHeight;
			boundsXMultiplier = 1.0 * boundsWidth  / slideWidth;
		}

		StorageProvider storage = switch (Config.Config.getString("storage.provider").toLowerCase()) {
			case "aws" -> new S3.Builder()
					.setConfigDefaults()
					.setBucket(id)
					.build();
			case "allas" -> new Allas.Builder()
					.setConfigDefaults()
					.setContainer(id)
					.build();
			default -> new FlatFile();
		};

		logger.info("Using {} as storage provider", storage);

		for (int level = levels - 1; level >= 0; level--) {
			int levelHeight = (int) (readIntegerProperty("openslide.level[" + level + "].height") * boundsYMultiplier);
			int levelWidth  = (int) (readIntegerProperty("openslide.level[" + level + "].width")  * boundsXMultiplier);

			int cols = (int) Math.ceil(1.0 * levelHeight / tileHeight);
			int rows = (int) Math.ceil(1.0 * levelWidth  / tileWidth);

			int downsample = (int) readDoubleProperty("openslide.level[" + level + "].downsample");

			// TODO: Let StorageProvider choose which TileArchive to use
			TileArchive tileArchive = new ZipTileArchive(id, level);

			for (int row = 0; row <= rows; row++) {
				for (int col = 0; col <= cols; col++) {
					executor.execute(new TileWorker(
						downsample, level, row, col,
						boundsX, boundsY,
						tileWidth, tileHeight,
						slideWidth, slideHeight,
						id,
						backgroundColor,
						openSlide,
						tileArchive
					));
				}
			}

			float start = System.currentTimeMillis();
			int tiles = rows * cols;

			synchronized (executor) {
				while (!executor.isQuiescent() && !hasTimedOut(start)) {
					System.out.print("\rProcessing tiles [L=" + level + "; generated ~" + (tiles - executor.getQueuedSubmissionCount()) +" / ~" + tiles + " tiles]");
					executor.wait(100);
				}
			}

			/* TODO: This might be called while there is still tiles being processed and result in a IOException:
			    "Stream has already been finished" in TarTileArchive#addTile, caused by TarArchiveOutputStream:342
			    Probably has to do when processing takes longer than the timeout period. */
			File archive = tileArchive.save();

			logger.debug("Committing archive to storage");
			storage.commitArchive(archive); // TODO: Commit async

			logger.debug("Deleting archive file");
			Files.delete(archive.toPath());
		}

		generateThumbnail(id, storage);
		generateProperties(id, storage);

		logger.debug("Deleting original slide");
		Files.delete(slide.toPath());

		long endTime = System.currentTimeMillis();
		System.out.print("\rTook " + (endTime - startTime) / 1000.0 + " seconds to generate & upload tiles.");
	}

	/**
	 * This method checks if the slide name is a valid UUID. If Slide name is a UUID the method
	 * returns that, otherwise it generates a new UUID. This is to ensure that all slides are
	 * saved as a UUID.
	 */
	private String getOrGenerateUUID(String slideName) {
		try {
			return UUID.fromString(slideName).toString();
		} catch (IllegalArgumentException e) {
			return UUID.randomUUID().toString();
		}
	}

	/**
	 * Parses the OpenSlide configuration for background color.
	 * @return background color or null
	 */
	private Color getBackgroundColor() {
		Color color = null;

		try {
			String bg = readStringProperty(OpenSlide.PROPERTY_NAME_BACKGROUND_COLOR);

			if (bg != null) {
				if (!bg.startsWith("#")) {
					bg = "#" + bg;
				}

				color = Color.decode(bg);
			}
		} catch (Exception e) {
			logger.debug("Unable to find background color: {}", e.getLocalizedMessage());
		}

		return color;
	}

	/**
	 * Generates the thumbnail for the slide and saves it using the provided StorageProvider.
	 */
	private void generateThumbnail(String id, StorageProvider storageProvider) {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			BufferedImage thumbnail = openSlide.createThumbnailImage(500);

			Path path = Path.of(String.format(Constants.TEMP_FILE, id + "_thumbnail.jpg"));

			ImageIO.write(thumbnail, "jpg", os);

			Files.write(path, os.toByteArray());

			storageProvider.commitFile(path.toFile());

			Files.delete(path);
		} catch (IOException e) {
			logger.error("Error while generating thumbnail", e);
		}
	}

	/**
	 * Generates the .properties file for this slide and adds custom properties such as <b>openslide.remoteserver.uri</b>
	 * and <b>openslide.thumbnail.uri</b>.
	 *
	 * @param id id of the slide.
	 * @param storageProvider StorageProvider used to upload this slide.
	 */
	private void generateProperties(String id, StorageProvider storageProvider) {
		Map<String, String> properties = new HashMap<>(openSlide.getProperties());
		properties.put("openslide.remoteserver.uri", storageProvider.getTilesURI().replace("{id}", id));
		properties.put("openslide.thumbnail.uri",    storageProvider.getThumbnailURI().replace("{id}", id));
		properties.put("openslide.level[0].tile-width", "1024");
		properties.put("openslide.level[0].tile-height", "1024");

		Path propertiesFilePath = Path.of(String.format(Constants.SLIDE_PROPERTIES_FILE, id));
		String JSON = new GsonBuilder().setPrettyPrinting().create().toJson(properties);

		try {
			Files.writeString(propertiesFilePath, JSON);
		} catch (IOException e) {
			logger.error("Error while saving {} properties file", id, e);
		}
	}

	private String readStringProperty(String property) {
		return openSlide.getProperties().get(property);
	}

	private double readDoubleProperty(String property) {
		return Double.parseDouble(openSlide.getProperties().get(property));
	}

	private int readIntegerProperty(String property) {
		return Integer.parseInt(openSlide.getProperties().get(property));
	}

	private boolean hasTimedOut(float startTime) {
		return (System.currentTimeMillis() - startTime) < TIMEOUT;
	}
}
