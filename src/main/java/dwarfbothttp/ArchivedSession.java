package dwarfbothttp;

import Code.DecodedImage;
import Code.TilesetDetected;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jacob Sims
 */
public class ArchivedSession {
	public static final String TOCONVERT_FILENAME = "toConvert.png";
	public static final String INFO_FILENAME = "info.json";
	public static final String DECODED_IMAGE_FILENAME = "decodedImage.ser";
	public static final String ARCHIVE_DIR_NAME = "sessions";


	private static Gson gson; // Says it is thread safe.

	private File directory;
	private String id;

	public ArchivedSession(File _directory) throws UnarchiveFailedException {
		directory = _directory;
		id = retrieveId();

		// Unarchive and discard the value. That way, any issues with the directory can be noticed as soon as possible.
		convertToLive();
	}

	public LiveSession convertToLive() throws UnarchiveFailedException {
		File jsonFile = new File(directory, INFO_FILENAME);
		try {
			Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
			Map<String, Object> map = gson.fromJson(new FileReader(jsonFile), mapType);
			int seedx = (Integer)map.get("seedx");
			int seedy = (Integer)map.get("seedy");
			Map<String, Object> tilesetDetectedMap = (Map<String, Object>)map.get("tilesetDetected");
			TilesetDetected tilesetDetected = new TilesetDetected(
					(Integer)tilesetDetectedMap.get("basex"),
					(Integer)tilesetDetectedMap.get("basey"),
					Session.tilesetWithPath((String)tilesetDetectedMap.get("tilesetpath")),
					(Integer)tilesetDetectedMap.get("matchCount"));
			BufferedImage toConvert;
			toConvert = ImageIO.read(new File(directory, TOCONVERT_FILENAME));
			DecodedImage decodedImage;
			try (
					FileInputStream fileInputStream = new FileInputStream(new File(directory, DECODED_IMAGE_FILENAME));
					ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)
			) {
				decodedImage = (DecodedImage)objectInputStream.readObject();
			}
			LiveSession liveSession = new LiveSession(toConvert, tilesetDetected, decodedImage, seedx, seedy);
			liveSession.setId((String)map.get("id"));

			return liveSession;
		} catch (IOException|ClassCastException|ClassNotFoundException|NullPointerException e) {
			throw new UnarchiveFailedException(e);
		}
	}

	public String statusJson() {
		HashMap<String, Integer> statusMap = new HashMap<>();
		statusMap.put("loadImageForConverting", 100);
		statusMap.put("extractTileset", 100);
		statusMap.put("readTiles", 100);
		return gson.toJson(statusMap);
	}

	public static ArrayList<ArchivedSession> retrieveAllFromConfigDir() {
		ArrayList<ArchivedSession> archivedSessions = new ArrayList<>();
		File sessionsDir = new File(Main.getConfigDir(), ARCHIVE_DIR_NAME);
		if (!sessionsDir.isDirectory()) {
			return archivedSessions;
		}
		File[] subdirectories = sessionsDir.listFiles();
		for (File subdirectory : subdirectories) {
			try {
				archivedSessions.add(new ArchivedSession(subdirectory));
			} catch (UnarchiveFailedException e) {
				e.printStackTrace();
				System.out.println("A broken archive directory was found here: " + subdirectory);
				// Just skip this one.
			}
		}
		return archivedSessions;
	}

	private String retrieveId() throws UnarchiveFailedException {
		File infoFile = new File(directory, INFO_FILENAME);
		Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
		try (FileReader fileReader = new FileReader(infoFile)) {
			Map<String, Object> map = gson.fromJson(fileReader, mapType);
			return (String)map.get("id");
		} catch (ClassCastException|IOException e) {
			throw new UnarchiveFailedException(e);
		}
	}

	public static ArchivedSession convertFromLive(LiveSession liveSession) throws UnarchiveFailedException {
		File archiveContainer = new File(Main.getConfigDir(), ARCHIVE_DIR_NAME);
		archiveContainer.mkdirs();
		File archiveDir = null;
		try {
			// This sounds like the directory will be deleted afterward, but it does not do that.
			// We use this to create a random filename without working hard to generate it.
			archiveDir = Files.createTempDirectory(archiveContainer.toPath(), "session_").toFile();
		} catch (IOException e) {
			throw new Error("Could not create a directory for the ArchivedSession", e);
		}
		HashMap<String, Object> tilesetDetectedJson = new HashMap<>();
		tilesetDetectedJson.put("basex", liveSession.getTilesetDetected().getBasex());
		tilesetDetectedJson.put("basey", liveSession.getTilesetDetected().getBasey());
		tilesetDetectedJson.put("tilesetpath", liveSession.getTilesetDetected().getTileset().getImagePath());
		tilesetDetectedJson.put("matchCount", liveSession.getTilesetDetected().getMatchCount());

		HashMap<String, Object> infoJson = new HashMap<>();
		infoJson.put("tilesetDetected", tilesetDetectedJson);
		infoJson.put("id", liveSession.getId());
		infoJson.put("seedx", liveSession.getSeedx());
		infoJson.put("seedy", liveSession.getSeedy());

		try (
				FileWriter jsonWriter = new FileWriter(new File(archiveDir, INFO_FILENAME));
				FileOutputStream fileOutputStream = new FileOutputStream(new File(archiveDir, DECODED_IMAGE_FILENAME));
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
		) {
			gson.toJson(infoJson, jsonWriter);
			ImageIO.write(liveSession.getToConvert(), "png", new File(archiveDir, TOCONVERT_FILENAME));
			objectOutputStream.writeObject(liveSession.getDecodedImage());
		} catch (IOException e) {
			throw new Error("Could not write a file in the ArchivedSession", e);
		}

		return new ArchivedSession(archiveDir);
	}

	public String getId() {
		return id;
	}

	static {
		gson = new Gson();
	}
}
