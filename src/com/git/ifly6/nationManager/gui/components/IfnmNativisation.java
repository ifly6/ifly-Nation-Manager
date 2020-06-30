package com.git.ifly6.nationManager.gui.components;

import com.git.ifly6.iflyLibrary.IflySystem;
import com.git.ifly6.nationManager.gui.IflyNationManager;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class IfnmNativisation {

    private static final Logger LOGGER = Logger.getLogger(IfnmNativisation.class.getName());

    public static Path PERSIST_DIR;
    public static Path NATIONS_STORE;
    public static Path PASS_HASH_STORE;

    private IfnmNativisation() {
    }

    public static void initialise() {
        // set nice-looking logging format
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");

        // if we are on Mac, go here
        if (IflySystem.IS_OS_MAC) {
            PERSIST_DIR = Paths.get(System.getProperty("user.home"),
                    "Library",
                    "Application Support",
                    "ifly Nation Manager");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                    "Ifly Nation Manager " + IflyNationManager.VERSION.toString());
        } else {
            // otherwise, go here
            PERSIST_DIR = Paths.get(System.getProperty("user.dir"), "config");
        }

        NATIONS_STORE = PERSIST_DIR.resolve("nations-store.txt");
        PASS_HASH_STORE = PERSIST_DIR.resolve("hash-store");

        // create directories if necessary
        try {
            Files.createDirectories(PERSIST_DIR);
        } catch (IOException e) {
            LOGGER.info("Could not create necessary directories for files");
        }
    }

    public static void nativise() {
        if (IflySystem.IS_OS_MAC) {    // Mac look-and-feel
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "ifly Nation Manager");
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }
}
