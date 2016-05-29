package dwarfbothttp;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;

/**
 * @author Jacob Sims
 */
public class SessionManager {
	private HashMap<String, Session> sessionMap;

	public SessionManager() {
		this(new HashMap<String, Session>());
	}

	public SessionManager(HashMap<String, Session> _sessionMap) {
		sessionMap = _sessionMap;
	}

	public HashMap<String, Session> getSessionMap() {
		return sessionMap;
	}

	public String addNewSession() {
		String id = createSessionId();
		sessionMap.put(id, new Session());
		return id;
	}

	public Session get(String k) {
		return sessionMap.get(k);
	}

	private static String createSessionId() {
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			//This should never happen! http://docs.oracle.com/javase/7/docs/api/java/security/MessageDigest.html
			throw new Error("Java is messed up", e);
		}
		String beforeHash = new Long(System.nanoTime()).toString() + ':' + new Random().nextInt();
		messageDigest.update(beforeHash.getBytes());
		return DatatypeConverter.printHexBinary(messageDigest.digest()).substring(0, 16).toLowerCase();
	}
}
