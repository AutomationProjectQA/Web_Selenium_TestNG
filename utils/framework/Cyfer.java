package framework;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Cyfer {

	/**
	 * Encrypt string using AES Encryption
	 * 
	 * @param strToEncrypt
	 *            String to Encrypt
	 * @param secret
	 *            Key to encrypt string
	 * @return
	 */
	public static String encrypt(String strToEncrypt, String secret) {
		try {
			byte[] key = secret.getBytes();
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			final SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		} catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return null;
	}

	/**
	 * Decrypt string using AES Encryption
	 * 
	 * @param strToDecrypt
	 *            String to Decrypt
	 * @param secret
	 *            Key to Decrypt
	 * @return
	 */
	public static String decrypt(String strToDecrypt, String secret) {

		String encrypted = "";
		try {
			byte[] key = secret.getBytes();
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			final SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			encrypted = new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
		} catch (Exception e) {
			System.out.println("Error while decrypting: " + e.toString());
		}

		return encrypted;
	}

}
