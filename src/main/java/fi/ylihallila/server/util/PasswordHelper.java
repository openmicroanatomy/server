package fi.ylihallila.server.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Code based of https://howtodoinjava.com/java/java-security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
 */
public class PasswordHelper {

    private final static String ALGORITHM = "PBKDF2WithHmacSHA1";

    public static SecurePassword generateStrongPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        int iterations = 1000;
        char[] chars = password.toCharArray();
        byte[] salt = generateSalt();

        KeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] hash = skf.generateSecret(spec).getEncoded();

        return new SecurePassword(iterations, toHex(salt), toHex(hash));
    }

    private static byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }

    public static boolean validatePassword(String originalPassword, String storedPassword) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecurePassword securePassword = new SecurePassword(storedPassword);

        int iterations = securePassword.getIterations();
        byte[] salt = fromHex(securePassword.getSalt());
        byte[] hash = fromHex(securePassword.getHash());

        KeySpec spec = new PBEKeySpec(originalPassword.toCharArray(), salt, iterations, hash.length * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] testHash = skf.generateSecret(spec).getEncoded();

        int diff = hash.length ^ testHash.length;
        for (int i = 0; i < hash.length && i < testHash.length; i++) {
            diff |= hash[i] ^ testHash[i];
        }

        return diff == 0;
    }

    private static String toHex(byte[] array) {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();

        if (paddingLength > 0) {
            return String.format("%0" + paddingLength + "d", 0) + hex;
        } else {
            return hex;
        }
    }

    private static byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }

        return bytes;
    }

    public static class SecurePassword {

        private final int iterations;
        private final String salt;
        private final String hash;

        public SecurePassword(String password) {
            String[] parts = password.split(":");

            if (parts.length == 3) {
                iterations = Integer.parseInt(parts[0]);
                salt = parts[1];
                hash = parts[2];
            } else {
                throw new IllegalArgumentException("Stored password not of following format: <iterations>:<salt>:<hash>");
            }
        }

        public SecurePassword(int iterations, String salt, String hash) {
            this.iterations = iterations;
            this.salt = salt;
            this.hash = hash;
        }

        public int getIterations() {
            return iterations;
        }

        public String getSalt() {
            return salt;
        }

        public String getHash() {
            return hash;
        }

        @Override
        public String toString() {
            return iterations + ":" + salt + ":" + hash;
        }
    }
}
