package fi.ylihallila.server;

import com.google.gson.GsonBuilder;
import fi.ylihallila.server.archivers.TarTileArchive;
import fi.ylihallila.server.archivers.TileArchive;
import fi.ylihallila.server.storage.Allas;
import fi.ylihallila.server.storage.StorageProvider;
import fi.ylihallila.server.util.Constants;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class TileGenerator {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final ForkJoinPool executor = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

	private final OpenSlide openSlide;

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

		String id = getOrGenerateUUID(slide.getName());

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

		StorageProvider tileStorage = new Allas.Builder()
		   .setConfigDefaults()
		   .setContainer(id)
		   .build();

		for (int level = levels - 1; level >= 0; level--) {
			int levelHeight = (int) (readIntegerProperty("openslide.level[" + level + "].height") * boundsYMultiplier);
			int levelWidth  = (int) (readIntegerProperty("openslide.level[" + level + "].width")  * boundsXMultiplier);

			int cols = (int) Math.ceil(1.0 * levelHeight / tileHeight);
			int rows = (int) Math.ceil(1.0 * levelWidth  / tileWidth);

			int downsample = (int) readDoubleProperty("openslide.level[" + level + "].downsample");

			TileArchive tileArchive = new TarTileArchive(id, level);

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
				while (!executor.isQuiescent() && (System.currentTimeMillis() - start < 300000)) {
					System.out.print("\rProcessing tiles [L=" + level + "; generated ~" + (tiles - executor.getQueuedSubmissionCount()) +" / ~" + tiles + " tiles]");
					executor.wait(100);
				}
			}

			File archive = tileArchive.save();

			logger.debug("Starting archive upload to Allas");
			tileStorage.commitArchive(archive); // TODO: Commit async
			logger.debug("Archive upload finished");
		}

		generateProperties(id, tileStorage);

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
			color = null;
			logger.debug("Unable to find background color: {}", e.getLocalizedMessage());
		}

		return color;
	}

	/**
	 * Generates the .properties file for this slide and adds property
	 * `openslide.remoteserver.uri` based on {@link StorageProvider#getTilesURI()}
	 *
	 * @param id id of the slide.
	 * @param storageProvider StorageProvider used to upload this slide.
	 */
	private void generateProperties(String id, StorageProvider storageProvider) {
		Map<String, String> properties = new HashMap<>(openSlide.getProperties());
		properties.put("openslide.remoteserver.uri", storageProvider.getTilesURI().replace("{id}", id));

		String json = new GsonBuilder().setPrettyPrinting().create().toJson(properties);

		try {
			Files.write(Path.of(String.format(Constants.SLIDE_PROPERTIES_FILE, id)), json.getBytes());
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
}
