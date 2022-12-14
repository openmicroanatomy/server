package fi.ylihallila.server.generators;

import fi.ylihallila.server.archivers.TileArchive;
import fi.ylihallila.server.storage.StorageProvider;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TileWorker implements Runnable {

	private final Logger logger = LoggerFactory.getLogger(TileWorker.class);

	private final String slideName;
	private final Color bgColor;

	private final int compression;

	private final int level;
	private final int tileX;
	private final int tileY;

	private final int tileWidth;
	private final int tileHeight;

	private final OpenSlide openSlide;
	private final TileArchive archive;
	private final StorageProvider storageProvider;

	public TileWorker(int compression, int downsample, int level, int row, int col, int offsetX, int offsetY, int tileWidth, int tileHeight, int slideWidth, int slideHeight, String slideName, Color bgColor, OpenSlide openSlide, TileArchive archive, StorageProvider storageProvider) {
		this.compression = compression;
		this.slideName = slideName;
		this.bgColor = bgColor;
		this.level = level;

		this.tileY = col * tileHeight * downsample + offsetY;
		this.tileX = row * tileWidth  * downsample + offsetX;

		int adjustX = 0;
		int adjustY = 0;

		if ((tileX + downsample * tileWidth) > slideWidth) {
			adjustX = tileWidth - Math.abs((tileX - slideWidth) / downsample);
		}

		if ((tileY + downsample * tileHeight) > slideHeight) {
			adjustY = tileHeight - Math.abs((tileY - slideHeight) / downsample);
		}

		this.tileHeight = tileHeight - adjustY;
		this.tileWidth  = tileWidth  - adjustX;

		this.openSlide = openSlide;
		this.archive = archive;
		this.storageProvider = storageProvider;
	}

	@Override
	public void run() {
		try {
			if (tileWidth == 0 || tileHeight == 0) {
				return;
			}

			BufferedImage temp = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB_PRE);
			int[] data = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();

			openSlide.paintRegionARGB(data, tileX, tileY, level, tileWidth, tileHeight);

			BufferedImage img = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = img.createGraphics();

			if (bgColor != null) {
				g2d.setColor(bgColor);
				g2d.fillRect(0, 0, tileWidth, tileHeight);
			}

			g2d.drawImage(temp, 0, 0, tileWidth, tileHeight, null);
			g2d.dispose();

			if (bgColor != null && isBackgroundTile(img)) {
				return;
			}

			String fileName = storageProvider.getTileNamingFormat()
					.replace("{id}", slideName)
					.replace("{level}", String.valueOf(level))
					.replace("{tileX}", String.valueOf(tileX))
					.replace("{tileY}", String.valueOf(tileY))
					.replace("{tileHeight}", String.valueOf(tileHeight))
					.replace("{tileWidth}", String.valueOf(tileWidth));

			archive.addTile(fileName, compressImage(img));
		} catch (Exception e) {
			logger.error("Error when generating tile: {}, row: {}, col: {}, x/y: {}/{}", slideName, tileX, tileY, tileWidth, tileHeight);
			e.printStackTrace();
		}
	}

	/**
	 * Compress a tile using lossy JPEG compression.
	 */
	private byte[] compressImage(BufferedImage img) throws IOException {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			ImageWriter     writer = ImageIO.getImageWritersByFormatName("jpg").next();
			ImageWriteParam params = writer.getDefaultWriteParam();
			params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			params.setCompressionQuality(compression / 100.0f);

			writer.setOutput(new MemoryCacheImageOutputStream(os));

			IIOImage output = new IIOImage(img, null, null);
			writer.write(null, output, params);
			writer.dispose();

			os.flush();

			return os.toByteArray();
		}
	}

	/**
	 * Checks if the tile can be considered background. Similar tones to background color are
	 * ignored and a total of 1% of the pixels can be entirely different for the tile to be
	 * considered background.
	 * @param img BufferedImage of tile to check
	 * @return true if to be considered background
	 */
	private boolean isBackgroundTile(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();

		int pixelsNotBackground = 0;
		int maxPixelsDifferent = (int) Math.ceil(0.01f * w * h);

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				if (img.getRGB(x, y) > scaleColor(bgColor, 1.05).getRGB() ||
					img.getRGB(x, y) < scaleColor(bgColor, 0.95).getRGB()) {

					pixelsNotBackground++;

					if (pixelsNotBackground > maxPixelsDifferent) {
						return false;
					}
				}
			}
		}

		return true;
	}

	private Color scaleColor(Color color, double factor) {
		return new Color(do8BitRangeCheck(color.getRed() * factor),
				do8BitRangeCheck(color.getGreen() * factor),
				do8BitRangeCheck(color.getBlue() * factor),
				color.getAlpha());
	}

	private int do8BitRangeCheck(double v) {
		return v < 0 ? 0 : (v > 255 ? 255 : (int)v);
	}
}
