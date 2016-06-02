package dwarfbothttp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;

import javax.naming.ConfigurationException;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jacob Sims
 */
public class SlackPosterBot {
	private static final String SLACK_TOKEN_CONFIG_KEY = "slackToken";
	private static final String SLACK_CHANNEL_CONFIG_KEY = "slackChannel";
	private String slackToken;
	private String slackChannelId;
	private HttpClient httpClient;
	private static Gson gson;

	public SlackPosterBot() throws ConfigurationException {
		File configFile = new File(Main.getConfigDir(), Main.CONFIGFILE_FILENAME);
		httpClient = HttpClients.createDefault();
		try (FileReader fileReader = new FileReader(configFile)) {
			Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
			Map<String, Object> configMap = gson.fromJson(fileReader, mapType);
			slackToken = (String)configMap.get(SLACK_TOKEN_CONFIG_KEY);
			String channelName = (String)configMap.get(SLACK_CHANNEL_CONFIG_KEY);
			if (channelName == null) {
				throw new ConfigurationException("Channel name not configured.");
			}
			slackChannelId = slackChannelIdFromChannelName(channelName);
		} catch (IOException|ClassCastException e) {
			throw new ConfigurationException("Could not initialize SlackPosterBot with a token");
		}
	}

	private String slackChannelIdFromChannelName(String channelName) throws IOException, ConfigurationException {
		try {
			Map<String, String> getParams = new HashMap<>();
			getParams.put("exclude_archived", "1");
			HttpGet httpGet = new HttpGet(slackApiUri("channels.list", getParams));
			HttpResponse response = httpClient.execute(httpGet);
			try (
					InputStream contentStream = response.getEntity().getContent();
					Reader streamReader = new InputStreamReader(contentStream);
			) {
				Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
				Map<String, Object> responseMap = gson.fromJson(streamReader, mapType);
				List<Map<String, Object>> channelObjects = (List<Map<String, Object>>)responseMap.get("channels");
				for (Map<String, Object> channelObject : channelObjects) {
					if (channelName.equals(channelObject.get("name"))) {
						String channelId = (String)channelObject.get("id");
						if (channelId != null) {
							return channelId;
						}
					}
				}
				throw new ConfigurationException("Could not find the channel named " + channelName);
			}
		} catch (ClassCastException e) {
			throw new IOException("Could not read response from Slack", e);
		}
	}

	private void postMessage(String message) throws IOException {
		Map<String, String> getParams = new HashMap<>();
		getParams.put("text", message);
		getParams.put("channel", slackChannelId);
		getParams.put("as_user", "true");
		HttpGet httpGet = new HttpGet(slackApiUri("chat.postMessage", getParams));
		HttpResponse response = httpClient.execute(httpGet);
		try (
				InputStream contentStream = response.getEntity().getContent();
				Reader streamReader = new InputStreamReader(contentStream);
				BufferedReader bufferedReader = new BufferedReader(streamReader);
		) {
			System.out.println(bufferedReader.readLine());
		}
	}

	private URI slackApiUri(String endpoint, Map<String, String> params) {
		try {
			URIBuilder builder = new URIBuilder("https://slack.com/api/" + endpoint).addParameter("token", slackToken);
			for (String param : params.keySet()) {
				builder.addParameter(param, params.get(param));
			}
			return builder.build();
		} catch (URISyntaxException e) {
			throw new Error("This shouldn't happen: URL syntax of API request is bad. Check surrounding code.");
		}
	}

	static {
		gson = new Gson();
	}
}
