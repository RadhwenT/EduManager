package io.github.radhwent.edumanager.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Connexion unique à la base Oracle.
 *
 * Les identifiants ne sont plus écrits dans le code. Ils sont lus, par ordre de priorité :
 *   1. propriétés système  : -Ddb.url=... -Ddb.user=... -Ddb.password=...
 *   2. variables d'environnement : DB_URL, DB_USER, DB_PASSWORD
 *   3. fichier db.properties à la racine du projet (voir db.properties.example)
 */
public class DBConnection {

	private static final String CONFIG_FILE = "db.properties";
	private static final String DEFAULT_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1";

	private static Connection connection;
	private static String lastError;

	private DBConnection() {}

	public static Connection getConnection() {
		try {
			if (connection == null || connection.isClosed()) {
				Properties cfg = loadFileConfig();
				String url = setting("db.url", "DB_URL", cfg, DEFAULT_URL);
				String user = setting("db.user", "DB_USER", cfg, null);
				String password = setting("db.password", "DB_PASSWORD", cfg, null);

				if (user == null || password == null) {
					lastError = "Identifiants de la base manquants. Copiez db.properties.example en db.properties "
							+ "et renseignez db.user et db.password.";
					System.err.println("❌ " + lastError);
					return null;
				}

				Class.forName("oracle.jdbc.OracleDriver");
				connection = DriverManager.getConnection(url, user, password);
				connection.setAutoCommit(true);
				lastError = null;
				System.out.println("✅ Connexion Oracle établie avec succès (" + url + ")");
			} else if (!connection.getAutoCommit()) {
				connection.setAutoCommit(true);
			}
		} catch (Exception e) {
			lastError = e.getMessage();
			System.err.println("❌ Erreur de connexion à la base Oracle : " + e.getMessage());
		}
		return connection;
	}

	/** Dernière erreur de connexion, affichée à l'utilisateur au démarrage. */
	public static String getLastError() {
		return lastError;
	}

	private static String setting(String sysProp, String envVar, Properties cfg, String defaultValue) {
		String v = System.getProperty(sysProp);
		if (isBlank(v)) v = System.getenv(envVar);
		if (isBlank(v)) v = cfg.getProperty(sysProp);
		return isBlank(v) ? defaultValue : v.trim();
	}

	private static Properties loadFileConfig() {
		Properties p = new Properties();
		Path file = Path.of(CONFIG_FILE);
		if (Files.exists(file)) {
			try (InputStream in = Files.newInputStream(file)) {
				p.load(in);
			} catch (IOException e) {
				System.err.println("⚠️ Impossible de lire " + CONFIG_FILE + " : " + e.getMessage());
			}
		}
		return p;
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}
}
