package application;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hachage des mots de passe avec PBKDF2-HMAC-SHA256 (salt aléatoire par utilisateur).
 * Format stocké : pbkdf2$&lt;itérations&gt;$&lt;salt base64&gt;$&lt;hash base64&gt;
 *
 * Les anciens mots de passe stockés en clair restent acceptés une fois,
 * puis sont automatiquement re-hachés à la connexion (voir UserDAO).
 */
public final class PasswordUtil {

	private static final String PREFIX = "pbkdf2";
	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 120_000;
	private static final int SALT_BYTES = 16;
	private static final int HASH_BITS = 256;
	private static final SecureRandom RANDOM = new SecureRandom();

	private PasswordUtil() {}

	public static String hash(String plain) {
		byte[] salt = new byte[SALT_BYTES];
		RANDOM.nextBytes(salt);
		byte[] hash = pbkdf2(plain.toCharArray(), salt, ITERATIONS);
		Base64.Encoder b64 = Base64.getEncoder();
		return PREFIX + "$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
	}

	public static boolean verify(String plain, String stored) {
		if (plain == null || stored == null) return false;
		if (!isHashed(stored)) {
			// Ancien format (texte clair) : comparaison à temps constant
			return MessageDigest.isEqual(plain.getBytes(StandardCharsets.UTF_8),
					stored.getBytes(StandardCharsets.UTF_8));
		}
		String[] parts = stored.split("\\$");
		if (parts.length != 4) return false;
		try {
			int iterations = Integer.parseInt(parts[1]);
			byte[] salt = Base64.getDecoder().decode(parts[2]);
			byte[] expected = Base64.getDecoder().decode(parts[3]);
			byte[] actual = pbkdf2(plain.toCharArray(), salt, iterations);
			return MessageDigest.isEqual(expected, actual);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public static boolean isHashed(String stored) {
		return stored != null && stored.startsWith(PREFIX + "$");
	}

	private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
			return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("PBKDF2 indisponible", e);
		}
	}
}
