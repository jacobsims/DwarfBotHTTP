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
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;

import com.google.gson.Gson;
import spark.Request;

/**
 * @author Jacob Sims
 */
public class Session {
	private static ArrayList<Tileset> supportedTilesets; // Use one set of these for all sessions.
	private static Gson gson; // Says it is thread safe.

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

	public void startDecoding() {
		if (stage != null) {
			// We have already started decoding. No need to start it over.
			return;
		}
		stage = new AtomicInteger(0);
		fitter = new TilesetFitter(supportedTilesets, false);
		conversionMainThread = new Thread(() -> {
			//TODO: Allow `artistic` mode (checkbox in the first form)
			fitter.loadImageForConverting(toConvert);
			stage.incrementAndGet();
			tilesetDetected = fitter.extractTileset();
			stage.incrementAndGet();
			decodedImage = fitter.readTiles(tilesetDetected.getBasex(), tilesetDetected.getBasey(), tilesetDetected.getTileset());
			stage.incrementAndGet();
		});
		conversionMainThread.start();
	}

	public String statusJson() {
		HashMap<String, Integer> statusMap = new HashMap<>();
		statusMap.put("loadImageForConverting", ((stage != null && stage.get() > 0) ? 100 : 0));
		statusMap.put("extractTileset", 100 * fitter.getNumTilesetChecksComplete() / supportedTilesets.size());
		statusMap.put("readTiles", ((stage != null && stage.get() > 2) ? 100 : 0));
		return gson.toJson(statusMap);
	}

	public BufferedImage getToConvert() {
		return toConvert;
	}

	public boolean isDecodingFinished() {
		return stage.get() == 3;
	}

	public BufferedImage renderToTileset(Tileset tileset) {
		if (!isDecodingFinished()) {
			throw new IllegalStateException("Cannot render an image if it is not decoded yet!");
		}
		int tilesetId = -1;
		for (int i = 0; i < supportedTilesets.size(); i++) {
			if (tileset == supportedTilesets.get(i)) {
				tilesetId = i;
				break;
			}
		}
		if (tilesetId == -1) {
			throw new IllegalArgumentException("Tileset not on the list of supported tilesets");
		}
		return fitter.renderImage(decodedImage, tilesetId);
	}

	public static ArrayList<Tileset> getSupportedTilesets() {
		return supportedTilesets;
	}

	static {
		TilesetManager tilesetManager = new TilesetManager();
		supportedTilesets = tilesetManager.getTilesets();
		gson = new Gson();
	}
}
