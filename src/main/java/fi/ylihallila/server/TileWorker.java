package fi.ylihallila.server;

import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
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

	private Color backgroundColor;

	public TileWorker(OpenSlide openSlide, int level, int row, int col, int tileWidth, int tileHeight, int slideWidth, int slideHeight, String slideName, Color backgroundColor) {
		this.openSlide = openSlide;
		this.level = level;
		this.row = row;
		this.col = col;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.slideWidth = slideWidth;
		this.slideHeight = slideHeight;
		this.slideName = slideName;
		this.backgroundColor = backgroundColor;
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

			BufferedImage temp = new BufferedImage(tileWidth - adjustX, tileHeight - adjustY, BufferedImage.TYPE_INT_ARGB_PRE);
			int[] data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

			openSlide.paintRegionARGB(data, tileX, tileY, level, tileWidth - adjustX, tileHeight - adjustY);

			BufferedImage img = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = img.createGraphics();

			if (backgroundColor != null) {
				g2d.setColor(backgroundColor);
				g2d.fillRect(0, 0, tileWidth, tileHeight);
			}

			g2d.drawImage(temp, 0, 0, tileWidth, tileHeight, null);
			g2d.dispose();

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(img, "jpg", os);
			String fileName = String.format(Configuration.TILE_FILE_FORMAT, slideName, tileX, tileY, level, tileWidth - adjustX, tileHeight - adjustY);

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
