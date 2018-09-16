package com.git.ifly6.nationManager;

import org.jasypt.util.text.BasicTextEncryptor;

public class IfnmCoder {

    private BasicTextEncryptor textEncryptor;

    /**
     * Creates an encoder which has a password set in the constructor
     * @param password to use to encrypt
     */
    public IfnmCoder(String password) {
        textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(password);
    }

    /**
     * Encodes plaintext with the password set in the constructor
     * @param plaintext to encrypt
     * @return encrypted text
     */
    public String encrypt(String plaintext) {
        return textEncryptor.encrypt(plaintext);
    }

    /**
     * Decodes text
     * @param hash to encrypt
     * @return decrypted text
     */
    public String decrypt(String hash) {
        return textEncryptor.decrypt(hash);
    }

}
