package cz.martlin.hg5;

import java.io.File;
import java.io.Serializable;

import javax.naming.InitialContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HomeGuardApp implements Serializable {
	private static final long serialVersionUID = -5651193670762995887L;

	private static final Logger LOG = LogManager.getLogger(HomeGuardApp.class);

	private static final String APP_NAME = "Homeguard";
	private static final String VERSION = "5.3";
	private static final String AUTHOR = "m@rtlin";
	private static final File CONFIG_FILE = createConfigFile();

	private static File createConfigFile() {
		try {
			InitialContext initialContext = new InitialContext();
			String name = (String) initialContext.lookup("java:comp/env/config.file");
			return new File(name);
		} catch (Exception e) {
			LOG.error("Cannot find value of config.file env-entry. Using null value.");
			return null;
		}
	}

	public static String getAppName() {
		return APP_NAME;
	}

	public static String getVersion() {
		return VERSION;
	}

	public static String getAuthor() {
		return AUTHOR;
	}

	public static File getConfigFile() {
		return CONFIG_FILE;
	}

}
