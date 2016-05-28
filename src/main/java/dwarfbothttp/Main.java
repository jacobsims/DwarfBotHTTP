package dwarfbothttp;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.MultipartConfigElement;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

public class Main {
	private static final String SESSION_COOKIE_NAME = "sessionID";

	private static SessionManager sessionManager;

	public static void main(String[] args) {
		sessionManager = new SessionManager();

		Spark.staticFiles.location("/static");
		VelocityTemplateEngine velocityTemplateEngine = new VelocityTemplateEngine();

		Spark.get("/", (request, response) -> {
			response.cookie(SESSION_COOKIE_NAME, sessionManager.addNewSession());
			HashMap<String, Object> model = new HashMap<String, Object>();
			return new ModelAndView(model, "index.vm");
		}, velocityTemplateEngine);
		Spark.post("/upload", (request, response) -> {
			String sessionId = getSessionIdForRequest(request, response);
			if (sessionId == null) {
				return response;
			}
			Session s = sessionManager.get(sessionId);

			try {
				s.setImageFromUpload(request);
			} catch (UploadFailedException e) {
				return errorOutResponse(response, 400, "Uploading the image failed.");
			}

			response.redirect("/convertpage");
			return response;
		});
		Spark.get("/uploadedimage.png", (request, response) -> {
			String sessionId = getSessionIdForRequest(request, response);
			if (sessionId == null) {
				return response;
			}
			Session s = sessionManager.get(sessionId);

			response.type("image/png");
			OutputStream outputStream = response.raw().getOutputStream();
			ImageIO.write(s.getToConvert(), "png", outputStream);

			return response;
		});
		Spark.get("/convertpage", (request, response) -> {
			String sessionId = getSessionIdForRequest(request, response);
			if (sessionId == null) {
				return response;
			}
			Session s = sessionManager.get(sessionId);

			HashMap<String, Object> model = new HashMap<String, Object>();
			return velocityTemplateEngine.render(new ModelAndView(model, "convertpage.vm"));
		});
	}

	private static Response errorOutResponse(Response response, int status, String message) {
		response.status(status);
		//TODO: Make this more friendly.
		response.body(message);
		return response;
	}

	private static String getSessionIdForRequest(Request request, Response response) {
		String id = request.cookie(SESSION_COOKIE_NAME);
		if (id == null || sessionManager.get(id) == null) {
			response.removeCookie(SESSION_COOKIE_NAME);
			errorOutResponse(response, 404, "Session not found.");
			return null;
		}
		return id;
	}
}
