package com.ikalagaming.crypto;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Allows for hashing of data.
 *
 * @author Ches Burks
 *
 */
public class Hasher {
	/**
	 * Returns a salted PBKDF2 hash of the password using a random salt.
	 *
	 * @param password the password to hash
	 * @return a salted PBKDF2 hash of the password
	 * @throws NoSuchAlgorithmException if a nonexistent algorithm was used
	 * @throws InvalidKeySpecException if the keyspec is wrong
	 */
	public static String createHash(char[] password)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Generate a random salt
		byte[] salt = Hasher.getRandomSaltBytes();
		return Hasher.createHash(password, salt);
	}

	// The following constants may be changed without breaking existing hashes.

	/**
	 * Returns a salted PBKDF2 hash of the password using the provided salt.
	 *
	 * @param password the password to hash
	 * @param salt the salt to use
	 * @return a salted PBKDF2 hash of the password
	 * @throws NoSuchAlgorithmException if a nonexistent algorithm was used
	 * @throws InvalidKeySpecException if the keyspec is wrong
	 */
	public static String createHash(char[] password, byte[] salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Hash the password
		byte[] hash =
				Hasher.pbkdf2(password, salt, Hasher.PBKDF2_ITERATIONS,
						Hasher.HASH_BYTE_SIZE);
		// format iterations:salt:hash
		return Hasher.PBKDF2_ITERATIONS + ":" + Hasher.toHex(salt) + ":"
				+ Hasher.toHex(hash);
	}

	/**
	 * Returns a salted PBKDF2 hash of the password using a random salt.
	 *
	 * @param password the password to hash
	 * @return a salted PBKDF2 hash of the password
	 * @throws NoSuchAlgorithmException if a nonexistent algorithm was used
	 * @throws InvalidKeySpecException if the keyspec is wrong
	 */
	public static String createHash(String password)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		return Hasher.createHash(password.toCharArray());
	}

	/**
	 * Converts a string of hexadecimal characters into a byte array.
	 *
	 * @param hex the hex string
	 * @return the hex string decoded into a byte array
	 */
	public static byte[] fromHex(String hex) {
		byte[] binary = new byte[hex.length() / 2];
		for (int i = 0; i < binary.length; i++) {
			binary[i] =
					(byte) Integer
							.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return binary;
	}

	/**
	 * Returns a random salt of length {@link #SALT_BYTE_SIZE} as a byte array.
	 *
	 * @return the array of bytes
	 */
	public static byte[] getRandomSaltBytes() {
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[Hasher.SALT_BYTE_SIZE];
		random.nextBytes(salt);
		return salt;
	}

	/**
	 * Returns a random salt of length {@link #SALT_BYTE_SIZE} as a byte array,
	 * formatted as a hex string.
	 *
	 * @return the random salt in hexadecimal format
	 */
	public static String getRandomSaltHex() {
		return Hasher.toHex(Hasher.getRandomSaltBytes());
	}

	/**
	 * Computes the PBKDF2 hash of a password.
	 *
	 * @param password the password to hash.
	 * @param salt the salt
	 * @param iterations the iteration count (slowness factor)
	 * @param bytes the length of the hash to compute in bytes
	 * @return the PBDKF2 hash of the password
	 * @throws NoSuchAlgorithmException if no Provider supports a
	 *             SecretKeyFactorySpi implementation for the specified
	 *             algorithm.
	 * 
	 * @throws InvalidKeySpecException - if the given key specification is
	 *             inappropriate for this secret-key factory to produce a secret
	 *             key.
	 */
	private static byte[] pbkdf2(char[] password, byte[] salt, int iterations,
			int bytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
		SecretKeyFactory skf =
				SecretKeyFactory.getInstance(Hasher.PBKDF2_ALGORITHM);
		return skf.generateSecret(spec).getEncoded();
	}

	/**
	 * Compares two byte arrays in length-constant time. This comparison method
	 * is used so that password hashes cannot be extracted from an on-line
	 * system using a timing attack and then attacked off-line.
	 *
	 * @param a the first byte array
	 * @param b the second byte array
	 * @return true if both byte arrays are the same, false if not
	 */
	private static boolean slowEquals(byte[] a, byte[] b) {
		int diff = a.length ^ b.length;
		for (int i = 0; i < a.length && i < b.length; i++) {
			diff |= a[i] ^ b[i];
		}
		return diff == 0;
	}

	/**
	 * Converts a byte array into a hexadecimal string.
	 *
	 * @param array the byte array to convert
	 * @return a length*2 character string encoding the byte array
	 */
	public static String toHex(byte[] array) {
		BigInteger bi = new BigInteger(1, array);
		String hex = bi.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if (paddingLength > 0) {
			return String.format("%0" + paddingLength + "d", 0) + hex;
		}
		return hex;
	}

	/**
	 * Validates a password using a hash.
	 *
	 * @param password the password to check
	 * @param correctHash the hash of the valid password
	 * @return true if the password is correct, false if not
	 * @throws NoSuchAlgorithmException if a nonexistent algorithm was used
	 * @throws InvalidKeySpecException if the keyspec is wrong
	 */
	public static boolean validatePassword(char[] password, String correctHash)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Decode the hash into its parameters
		String[] params = correctHash.split(":");
		int iterations = Integer.parseInt(params[Hasher.ITERATION_INDEX]);
		byte[] salt = Hasher.fromHex(params[Hasher.SALT_INDEX]);
		byte[] hash = Hasher.fromHex(params[Hasher.PBKDF2_INDEX]);
		// Compute the hash of the provided password, using the same salt,
		// iteration count, and hash length
		byte[] testHash =
				Hasher.pbkdf2(password, salt, iterations, hash.length);
		// Compare the hashes in constant time. The password is correct if
		// both hashes match.
		return Hasher.slowEquals(hash, testHash);
	}

	/**
	 * Validates a password using a hash.
	 *
	 * @param password the password to check
	 * @param correctHash the hash of the valid password
	 * @return true if the password is correct, false if not
	 * @throws NoSuchAlgorithmException if a nonexistent algorithm was used
	 * @throws InvalidKeySpecException if the keyspec is wrong
	 */
	public static boolean validatePassword(String password, String correctHash)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		return Hasher.validatePassword(password.toCharArray(), correctHash);
	}

	/**
	 * The string name of the algorithm to use (PBKDF2WithHmacSHA1).
	 */
	public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";

	/**
	 * The size of the salt, measured in bytes.
	 */
	public static final int SALT_BYTE_SIZE = 32;

	/**
	 * The size of the hash, measured in bytes.
	 */
	public static final int HASH_BYTE_SIZE = 32;

	/**
	 * Iteration count is the number of times that the password is hashed during
	 * the derivation of the symmetric key. The higher number, the more
	 * difficult it is to brute force the key.
	 */
	public static final int PBKDF2_ITERATIONS = 1000;

	/**
	 * The index of the iteration count in the hash string.
	 */
	public static final int ITERATION_INDEX = 0;

	/**
	 * The index of the salt in the hash string.
	 */
	public static final int SALT_INDEX = 1;

	/**
	 * The index of the hash in the hash string.
	 */
	public static final int PBKDF2_INDEX = 2;

}
