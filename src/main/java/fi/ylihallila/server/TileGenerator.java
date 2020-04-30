package fi.ylihallila.server;

import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TileGenerator {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public TileGenerator(String slideName) throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		System.loadLibrary("openslide-jni");
		OpenSlide openSlide = new OpenSlide(new File(slideName));

		int slideWidth = Integer.parseInt(openSlide.getProperties().get("openslide.level[0].width"));
		int slideHeight = Integer.parseInt(openSlide.getProperties().get("openslide.level[0].height"));
		int levels = Integer.parseInt(openSlide.getProperties().get("openslide.level-count"));

		for (int level = 0; level < levels; level++) {
			int tileHeight = Integer.parseInt(openSlide.getProperties().get("openslide.level[" + level + "].tile-height"));
			int tileWidth = Integer.parseInt(openSlide.getProperties().get("openslide.level[" + level + "].tile-width"));

			int rows = (int) Math.ceil(Double.parseDouble(openSlide.getProperties().get("openslide.level[" + level + "].width")) / tileWidth);
			int cols = (int) Math.ceil(Double.parseDouble(openSlide.getProperties().get("openslide.level[" + level + "].height")) / tileHeight);

			for (int row = 0; row <= rows; row++) {
				for (int col = 0; col <= cols; col++) {
					executor.submit(new TileWorker(
							openSlide,
							level, row, col,
							tileWidth, tileHeight,
							slideWidth, slideHeight,
							slideName
					));
				}
			}
		}

		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.MINUTES);

		long endTime = System.currentTimeMillis();
		logger.info("Took " + (endTime - startTime) + "ms to generate tiles.");
	}
}
