package dwarfbothttp;

import spark.Request;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Jacob Sims
 */
public class Session {
	private BufferedImage toConvert;

	public void setImageFromUpload(Request request) throws UploadFailedException {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement((String)null));
		try (InputStream inputStream = request.raw().getPart("to_convert").getInputStream()) {
			toConvert = ImageIO.read(inputStream);
			toConvert.getData();
		} catch (IOException|ServletException|NullPointerException e) {
			throw new UploadFailedException("Could not set image for the session from your upload.", e);
		}
	}

	public BufferedImage getToConvert() {
		return toConvert;
	}
}
