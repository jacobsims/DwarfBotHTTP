package dwarfbothttp;

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
		String id = ((Integer)(new Random().nextInt())).toString();
		return id;
	}
}
