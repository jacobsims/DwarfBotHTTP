package dwarfbothttp;

import Code.DecodedImage;
import Code.Tileset;
import Code.TilesetDetected;
import Code.TilesetFitter;
import Code.TilesetManager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import spark.Request;

/**
 * @author Jacob Sims
 */
public class Session {
	private static ArrayList<Tileset> supportedTilesets; // Use one set of these for all sessions.

	private BufferedImage toConvert;
	private Thread conversionMainThread;
	private TilesetFitter fitter;
	private AtomicInteger stage;
	private TilesetDetected tilesetDetected;
	private DecodedImage decodedImage;

	public void setImageFromUpload(Request request) throws UploadFailedException {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement((String)null));
		try (InputStream inputStream = request.raw().getPart("to_convert").getInputStream()) {
			toConvert = ImageIO.read(inputStream);
			toConvert.getData();
		} catch (IOException|ServletException|NullPointerException e) {
			throw new UploadFailedException("Could not set image for the session from your upload.", e);
		}
	}

	public void startConversion() {
		stage = new AtomicInteger(0);
		fitter = new TilesetFitter(supportedTilesets, false);
		conversionMainThread = new Thread(() -> {
			//TODO: Allow `artistic` mode (checkbox in the first form)
			fitter.loadImageForConverting(toConvert);
			stage.incrementAndGet();
			System.out.println(stage);
			tilesetDetected = fitter.extractTileset();
			stage.incrementAndGet();
			System.out.println(stage);
			decodedImage = fitter.decodeImage();
			stage.incrementAndGet();
			System.out.println(stage);
		});
		conversionMainThread.start();
	}

	public BufferedImage getToConvert() {
		return toConvert;
	}

	static {
		TilesetManager tilesetManager = new TilesetManager();
		supportedTilesets = tilesetManager.getTilesets();
	}
}
