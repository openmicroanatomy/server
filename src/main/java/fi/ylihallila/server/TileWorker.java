package fi.ylihallila.server;

import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

public class TileWorker implements Callable<Boolean> {

	private Logger logger = LoggerFactory.getLogger(TileWorker.class);

	private OpenSlide openSlide;

	private int level;
	private int row;
	private int col;

	private int tileWidth;
	private int tileHeight;

	private int slideWidth;
	private int slideHeight;

	private String slideName;

	public TileWorker(OpenSlide openSlide, int level, int row, int col, int tileWidth, int tileHeight, int slideWidth, int slideHeight, String slideName) {
		this.openSlide = openSlide;
		this.level = level;
		this.row = row;
		this.col = col;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.slideWidth = slideWidth;
		this.slideHeight = slideHeight;
		this.slideName = slideName;
	}

	@Override
	public Boolean call() {
		try {
			int downsample = (int) Double.parseDouble(openSlide.getProperties().get("openslide.level[" + level + "].downsample"));

			int tileX = row * tileWidth * downsample;
			int tileY = col * tileHeight * downsample;
			int adjustX = 0;
			int adjustY = 0;

			if ((tileX + downsample * tileWidth) > slideWidth) {
				adjustX = tileWidth - Math.abs((tileX - slideWidth) / downsample);
			}

			if ((tileY + downsample * tileHeight) > slideHeight) {
				adjustY = tileHeight - Math.abs((tileY - slideHeight) / downsample);
			}

			BufferedImage img = new BufferedImage(tileWidth - adjustX, tileHeight - adjustY, BufferedImage.TYPE_INT_RGB);
			int[] data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

			openSlide.paintRegionARGB(data, tileX, tileY, level, tileWidth - adjustX, tileHeight - adjustY);

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(img, "jpg", os);
			String fileName = String.format(Configuration.FILE_FORMAT, slideName, tileX, tileY, level, tileWidth - adjustX, tileHeight - adjustY);

			Files.write(Path.of("tiles", fileName),
					os.toByteArray(),
					StandardOpenOption.WRITE,
					StandardOpenOption.CREATE);

			os.flush();

			logger.info("Generating: " + slideName + "_" + tileX + "_" + tileY + "_" + level + "_" + (tileWidth - adjustX) + "_" + (tileHeight - adjustY) + ".png");
		} catch (IOException e) {
			logger.error("Error when generating tile: ", e);
			return false;
		}

		return true;
	}
}
