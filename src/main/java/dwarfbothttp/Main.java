package dwarfbothttp;

import java.io.InputStream;
import java.util.HashMap;
import javax.servlet.MultipartConfigElement;
import spark.ModelAndView;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

public class Main {
	public static void main(String[] args) {
		VelocityTemplateEngine velocityTemplateEngine = new VelocityTemplateEngine();

		Spark.get("/", (request, response) -> {
			HashMap<String, Object> model = new HashMap<String, Object>();
			return new ModelAndView(model, "index.vm");
		}, velocityTemplateEngine);
		Spark.post("/upload", (request, response) -> {
			request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement((String)null));
			try (InputStream inputStream = request.raw().getPart("to_convert").getInputStream()) {
				System.out.println(inputStream);
			}
			return "file uploaded";
		});
	}
}
