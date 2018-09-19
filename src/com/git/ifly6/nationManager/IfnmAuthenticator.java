package com.git.ifly6.nationManager;

import org.jasypt.util.password.BasicPasswordEncryptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.logging.Logger;

public class IfnmAuthenticator {

    private static final Logger LOGGER = Logger.getLogger(IfnmAuthenticator.class.getName());

    private static BasicPasswordEncryptor passwordEncryptor = new BasicPasswordEncryptor();

    // prevent instantiation
    private IfnmAuthenticator() {
    }

    /**
     * Generates hash for this password
     * @param password in plaintext for which to generate hash
     * @return password hash as string
     */
    public static String generateHash(String password) {
        return passwordEncryptor.encryptPassword(password);
    }

    /**
     * Saves the hash to file
     * @param path to save the hash to
     * @param hash to save
     * @throws IOException if thrown by {@link Files#write(Path, byte[], OpenOption...)}
     */
    public static void saveHash(Path path, String hash) throws IOException {
        Files.write(path, hash.getBytes());
    }

    /**
     * Returns the hash, based on the location provided
     * @param path to look at
     * @return the hash
     * @throws IOException if thrown by {@link Files#readAllBytes(Path)}
     */
    private static String getHash(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * Verifies whether the input challenge string is the same as the hash saved to file at the location
     * <code>persistentFile</code>
     * @param persistentFile to acquire hash
     * @param challenge      string with plaintext with which to challenge
     * @return true if verified, otherwise false
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean verify(Path persistentFile, String challenge) {
        if (Files.notExists(persistentFile)) {
            LOGGER.severe("Persistent file does not exist or is empty, cannot get hash");
            return false;
        }

        try {
            if (Files.size(persistentFile) == 0) {
                LOGGER.severe("Cannot get hash, persistent file has size 0");
                return false;
            }
            return passwordEncryptor.checkPassword(challenge, getHash(persistentFile));

        } catch (IOException e) {
            LOGGER.severe("Cannot get hash for checking from persistent file");
            return false;
        }
    }

}
