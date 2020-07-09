package fi.ylihallila.server;

import fi.ylihallila.server.archivers.TarTileArchive;
import fi.ylihallila.server.archivers.TileArchive;
import fi.ylihallila.server.storage.Allas;
import fi.ylihallila.server.storage.StorageProvider;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.*;

public class TileGenerator {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final ForkJoinPool executor = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

	private static OpenSlide openSlide;

	public static OpenSlide getOpenSlide() {
		return openSlide;
	}

	static {
		ImageIO.setUseCache(false);
	}

	// TODO: Catch exceptions so generation can try and continue.

	public TileGenerator(String slideName) throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();

		openSlide = new OpenSlide(new File(slideName));

		int slideHeight = readIntegerProperty("openslide.level[0].height");
		int slideWidth  = readIntegerProperty("openslide.level[0].width");
		int levels      = readIntegerProperty("openslide.level-count");

		int tileHeight = 1024; //readIntegerPropertyOrDefault("openslide.level[0].tile-height", 256);
		int tileWidth  = 1024; //readIntegerPropertyOrDefault("openslide.level[0].tile-width",  256);

		int boundsX = 0; //readIntegerPropertyOrDefault(OpenSlide.PROPERTY_NAME_BOUNDS_X, 0);
		int boundsY = 0; //readIntegerPropertyOrDefault(OpenSlide.PROPERTY_NAME_BOUNDS_Y, 0);

		int boundsHeight = 0; //readIntegerPropertyOrDefault(OpenSlide.PROPERTY_NAME_BOUNDS_WIDTH, slideHeight);
		int boundsWidth  = 0; //readIntegerPropertyOrDefault(OpenSlide.PROPERTY_NAME_BOUNDS_HEIGHT, slideWidth);

		double boundsYMultiplier = 1;
		double boundsXMultiplier = 1;

		if (boundsHeight > 0 || boundsWidth > 0) { // todo: whats the point of boundsXYmultiplier?
			boundsYMultiplier = 1.0 * boundsHeight / slideHeight;
			boundsXMultiplier = 1.0 * boundsWidth  / slideWidth;
		}

		Color backgroundColor = getBackgroundColor();

		String id = getOrGenerateUUID(slideName);

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

			TileArchive tileArchive = new TarTileArchive(slideName, level);

			for (int row = 0; row <= rows; row++) {
				for (int col = 0; col <= cols; col++) {
					executor.execute(new TileWorker(
						downsample, level, row, col,
						boundsX, boundsY,
						tileWidth, tileHeight,
						slideWidth, slideHeight,
						slideName,
						backgroundColor,
						tileArchive
					));
				}
			}

			float start = System.currentTimeMillis();
			int tiles = (int) Math.ceil(1.0 * levelHeight / tileHeight) * (int) Math.ceil(1.0 * levelWidth / tileWidth);

			synchronized (executor) {
				while (!executor.isQuiescent() && (System.currentTimeMillis() - start < 300000)) {
					System.out.print("\rProcessing tiles [L=" + level + "; generated ~" + (tiles - executor.getQueuedSubmissionCount()) +" / ~" + tiles + " tiles]");
					executor.wait(100);
				}
			}

			File archive = tileArchive.save();

			logger.debug("Starting archive upload to Allas");
			tileStorage.commitArchive(archive);
			logger.debug("Archive upload finished");
		}

		long endTime = System.currentTimeMillis();
		System.out.print("\rTook " + (endTime - startTime) / 1000.000 + " seconds to generate tiles.");
	}

	/**
	 * This method checks if the slide name is a valid UUID. If Slide name is a UUID the method
	 * returns that, otherwise it generates a new UUID.
	 */
	private String getOrGenerateUUID(String slideName) {
		try {
			return UUID.fromString(slideName).toString();
		} catch (IllegalArgumentException e) {
			return UUID.randomUUID().toString();
		}
	}

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

	private String readStringProperty(String property) {
		return openSlide.getProperties().get(property);
	}

	private double readDoubleProperty(String property) {
		return Double.parseDouble(openSlide.getProperties().get(property));
	}

	private Integer readIntegerProperty(String property) {
		return Integer.parseInt(openSlide.getProperties().get(property));
	}

	private Integer readIntegerPropertyOrDefault(String property, Integer defaultValue) {
		try {
			return Integer.parseInt(openSlide.getProperties().get(property));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
