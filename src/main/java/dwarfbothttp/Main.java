package dwarfbothttp;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Level;
import javax.imageio.ImageIO;

import Code.Tileset;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

public class Main {
	//TODO: Switch to a GET param instead of cookies so you can use multiple tabs
	private static final String SESSION_COOKIE_NAME = "sessionID";

	private static SessionManager sessionManager;

	public static void main(String[] args) {
		sessionManager = new SessionManager();

		Code.Main.setupLogger();
		Code.Main.logger.getHandlers()[0].setLevel(Level.WARNING);

		Spark.staticFiles.location("/static");
		VelocityTemplateEngine velocityTemplateEngine = new VelocityTemplateEngine();

		Spark.get("/", (request, response) -> {
			response.cookie(SESSION_COOKIE_NAME, sessionManager.addNewSession());
			HashMap<String, Object> model = new HashMap<String, Object>();
			return new ModelAndView(model, "index.vm");
		}, velocityTemplateEngine);
		Spark.post("/upload", (request, response) -> {
			String sessionId = getSessionIdForRequest(request, response);
			Session s = sessionManager.get(sessionId);

			try {
				s.setImageFromUpload(request);
			} catch (UploadFailedException e) {
				errorOutResponse(400, "Uploading the image failed.");
			}

			response.redirect("/convertpage");
			return response;
		});
		Spark.get("/uploadedimage.png", (request, response) -> {
			String sessionId = getSessionIdForRequest(request, response);
			Session s = sessionManager.get(sessionId);

			response.type("image/png");
			OutputStream outputStream = response.raw().getOutputStream();
			ImageIO.write(s.getToConvert(), "png", outputStream);

			return response;
		});
		Spark.get("/convertpage", (request, response) -> {
			String sessionId = getSessionIdForRequest(request, response);
			Session s = sessionManager.get(sessionId);
			s.startDecoding();

			HashMap<String, Object> model = new HashMap<String, Object>();
			return velocityTemplateEngine.render(new ModelAndView(model, "convertpage.vm"));
		});
		Spark.get("/encodeimage", (request, response) -> {
			String sessionId = getSessionIdForRequest(request, response);
			Session s = sessionManager.get(sessionId);
			if (!s.isDecodingFinished()) {
				errorOutResponse(400, "Your image is not decoded yet! Be patient.");
			}

			HashMap<String, Object> model = new HashMap<String, Object>();
			model.put("tilesets", Session.getSupportedTilesets());
			return velocityTemplateEngine.render(new ModelAndView(model, "encodeimage.vm"));
		});
		Spark.get("/encodedimage.png", (request, response) -> {
			String sessionId = getSessionIdForRequest(request, response);
			Session s = sessionManager.get(sessionId);
			Tileset tileset = null;
			for (Tileset t : Session.getSupportedTilesets()) {
				if (t.getImagePath().equals(request.queryParams("tileset"))) {
					tileset = t;
					break;
				}
			}

			try {
				BufferedImage renderedImage = null;
				try {
					renderedImage = s.renderToTileset(tileset);
				} catch (IllegalArgumentException e) {
					errorOutResponse(404, "Tileset not found!");
				} catch (IllegalStateException e) {
					errorOutResponse(400, "Your image is not decoded yet! Be patient.");
				}

				if (renderedImage == null) {
					errorOutResponse(500, "Internal server error");
				}

				response.type("image/png");
				OutputStream outputStream = response.raw().getOutputStream();
				ImageIO.write(renderedImage, "png", outputStream);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return response;
		});
	}

	private static void errorOutResponse(int status, String message) {
		//TODO: Make this more friendly.
		String responseBody = message;

		// Throws an exception that stops execution of the current route.
		Spark.halt(status, responseBody);
	}

	private static String getSessionIdForRequest(Request request, Response response) {
		String id = request.cookie(SESSION_COOKIE_NAME);
		if (id == null || sessionManager.get(id) == null) {
			response.removeCookie(SESSION_COOKIE_NAME);
			errorOutResponse(404, "Session not found.");
		}
		return id;
	}
}
