package fi.ylihallila.server;

import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TileGenerator {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private OpenSlide openSlide;

	public TileGenerator(String slideName) throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

		openSlide = new OpenSlide(new File(slideName));
		System.loadLibrary("openslide-jni");

		Files.createDirectories(Path.of("tiles", slideName));

		int slideWidth = readIntegerProperty("openslide.level[0].width");
		int slideHeight = readIntegerProperty("openslide.level[0].height");
		int levels = readIntegerProperty("openslide.level-count");
		Color backgroundColor = null;

		try {
			String bg = readStringProperty(OpenSlide.PROPERTY_NAME_BACKGROUND_COLOR);
			
			if (bg != null) {
				if (!bg.startsWith("#")) {
					bg = "#" + bg;
				}

				backgroundColor = Color.decode(bg);
			}
		} catch (Exception e) {
			backgroundColor = null;
			logger.debug("Unable to find background color: {}", e.getLocalizedMessage());
		}

		for (int level = 0; level < levels; level++) {
			int tileHeight = readIntegerPropertyOrDefault("openslide.level[" + level + "].tile-height", 256);
			int tileWidth = readIntegerPropertyOrDefault("openslide.level[" + level + "].tile-width", 256);

			int rows = (int) Math.ceil(readDoubleProperty("openslide.level[" + level + "].width")  / tileWidth);
			int cols = (int) Math.ceil(readDoubleProperty("openslide.level[" + level + "].height") / tileHeight);

			for (int row = 0; row <= rows; row++) {
				for (int col = 0; col <= cols; col++) {
					executor.submit(new TileWorker(
							openSlide,
							level, row, col,
							tileWidth, tileHeight,
							slideWidth, slideHeight,
							slideName,
							backgroundColor
					));
				}
			}
		}

		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.MINUTES);

		long endTime = System.currentTimeMillis();
		logger.info("Took " + (endTime - startTime) + "ms to generate tiles.");
	}

	private String readStringProperty(String property) {
		return openSlide.getProperties().get(property);
	}

	private Double readDoubleProperty(String property) {
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
