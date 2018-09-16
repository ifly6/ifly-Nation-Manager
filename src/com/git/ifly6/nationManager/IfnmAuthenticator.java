package com.git.ifly6.nationManager;

import org.jasypt.util.password.BasicPasswordEncryptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class IfnmAuthenticator {

    private static BasicPasswordEncryptor passwordEncryptor = new BasicPasswordEncryptor();

    private IfnmAuthenticator() {
    }

    public static String generateHash(String password) {
        return passwordEncryptor.encryptPassword(password);
    }

    public static void saveHash(Path path, String hash) throws IOException {
        Files.write(path, hash.getBytes());
    }

    private static String getHash(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    public static boolean verify(Path persistentFile, String challenge) throws IOException {
        return passwordEncryptor.checkPassword(challenge, getHash(persistentFile));
    }

}
