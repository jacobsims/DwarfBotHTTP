package dwarfbothttp;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
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

		//TODO: Fix this once Alex's changes are merged
		Code.Main.logger = Logger.getGlobal();

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
			s.startConversion();

			HashMap<String, Object> model = new HashMap<String, Object>();
			return velocityTemplateEngine.render(new ModelAndView(model, "convertpage.vm"));
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
