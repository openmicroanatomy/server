package fi.ylihallila.server.controllers;

import fi.ylihallila.server.Configuration;
import fi.ylihallila.server.OpenSlideCache;
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
import java.util.Optional;

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

	public void uploadSlide(Context ctx) {
		/*
		 * 1. Upload file
		 * 2. Get tiles & upload / store on server
		 * 3. Get Openslide properties
		 * 4. Add property "openslide.remoteserver.uri"
		 *
		 */
	}

	public void getSlideProperties(Context ctx) throws IOException {
		Path slidePath = Path.of("slides", ctx.pathParam("slide-name", String.class).get());

		if (slidePath.toFile().exists()) {
			ctx.status(200)
			   .contentType("application/json")
			   .result(Files.readString(slidePath));
		} else {
			ctx.status(404);
		}
	}

	public void renderTile(Context ctx) throws Exception {
		String slide   = ctx.pathParam("slide-name");
		int tileX      = ctx.pathParam("tileX", Integer.class).get();
		int tileY      = ctx.pathParam("tileY", Integer.class).get();
		int level      = ctx.pathParam("level", Integer.class).get();
		int tileWidth  = ctx.pathParam("tileWidth", Integer.class).get();
		int tileHeight = ctx.pathParam("tileHeight", Integer.class).get();

		String fileName = String.format(Configuration.FILE_FORMAT, slide, tileX, tileY, level, tileWidth, tileHeight);
		InputStream is;

		if (Files.exists(Path.of("tiles", fileName), LinkOption.NOFOLLOW_LINKS)) {
			logger.info("Retrieving from disk [{}, {},{} / {} / {},{}]", fileName, tileX, tileY, level, tileWidth, tileHeight);

			FileInputStream fis = new FileInputStream(Path.of("tiles", fileName).toString());
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
