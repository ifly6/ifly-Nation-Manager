/* Copyright (c) 2017 Kevin Wong. All Rights Reserved. */
package com.git.ifly6.nationManager.gui;

import com.git.ifly6.iflyLibrary.IflyDialogs;
import com.git.ifly6.iflyLibrary.IflyStrings;
import com.git.ifly6.iflyLibrary.IflySystem;
import com.git.ifly6.iflyLibrary.IflyVersion;
import com.git.ifly6.iflyLibrary.generics.IflyPair;
import com.git.ifly6.nationManager.IfnmAuthenticator;
import com.git.ifly6.nationManager.IfnmCoder;
import com.git.ifly6.nationManager.IfnmReader;
import com.git.ifly6.nationManager.IfnmWriter;
import com.git.ifly6.nsapi.NSConnection;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author ifly6
 */
public class IflyNationManager {

    private static final Logger LOGGER = Logger.getLogger(IflyNationManager.class.getName());

    @SuppressWarnings("WeakerAccess")
    public static final IflyVersion VERSION = new IflyVersion(2, 0);

    private static Path PERSIST_DIR;
    private static Path NATIONS_STORE;
    private static Path PASS_HASH_STORE;

    private IfnmCoder coder;

    private JFrame frame;
    private JList<IfnmNation> list;

    public static void main(String[] args) {

        // Get us a reasonable-looking log format
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

        // resolve the paths we use
        NATIONS_STORE = PERSIST_DIR.resolve("nations-store.txt");
        PASS_HASH_STORE = PERSIST_DIR.resolve("hash-store");

        // create directories if necessary
        try {
            Files.createDirectories(PERSIST_DIR);
        } catch (IOException e) {
            LOGGER.info("Could not create necessary directories for files. Terminating");
            return;
        }

        if (GraphicsEnvironment.isHeadless()) {

            Options options = new Options();

            Option passwordOption = new Option("p", "password", true, "Specifies password");
            passwordOption.setRequired(true);
            options.addOption(passwordOption);

            Option addOption = new Option("a", "Adds new nation");
            addOption.setLongOpt("add");
            addOption.setArgs(2);
            options.addOption(addOption);

            Option removeOption = new Option("r", "remove", false, "remove nation");
            options.addOption(removeOption);

            Option listOption = new Option("l", "list", false, "list loaded nations");
            options.addOption(listOption);

            Option connectOption = new Option("c", "connect", false, "connect to NationStates");
            options.addOption(connectOption);

            CommandLineParser parser = new DefaultParser();
            HelpFormatter formatter = new HelpFormatter();

            try {

                CommandLine cLine = parser.parse(options, args);

                // require password, then verify
                String password = cLine.getOptionValue(passwordOption.getOpt());

                /* 1. Is there a preexisting hash?
                 *  2(a) If not, then create.
                 *  2(b) If so, then verify. */
                if (Files.notExists(PASS_HASH_STORE)) {
                    String hash = IfnmAuthenticator.generateHash(password);
                    try {
                        IfnmAuthenticator.saveHash(PASS_HASH_STORE, hash);
                    } catch (IOException e) {
                        System.out.println("Could not create hash for secure storage. You're SOL.");
                    }

                } else {
                    if (!IfnmAuthenticator.verify(PASS_HASH_STORE, password)) {
                        System.out.println("Provided password's hash does not match that on file. Stopped.");
                        return;
                    }
                }

                /* All others, let's go down this path. */
                if (cLine.hasOption(addOption.getOpt())) {

                    String[] addArgs = cLine.getOptionValues(addOption.getOpt());
                    String nation = addArgs[0];
                    String nationPass = addArgs[1];

                    IfnmNation newNation = new IfnmNation(nation, new IfnmCoder(password).encrypt(nationPass));

                    // Load the current nations
                    List<IfnmNation> nations;
                    try {
                        nations = IfnmReader.read(NATIONS_STORE);
                    } catch (IOException e) {
                        LOGGER.info("Could not retrieve the current nation list to verify non-duplication");
                        return;
                    }

                    // Make sure that it's not already in the list
                    boolean match = nations.stream()
                            .map(IfnmNation::getName)
                            .anyMatch(s -> s.equals(newNation.getName()));
                    if (!match) {
                        nations.add(newNation);
                    } else {
                        LOGGER.info("Nation already exists in nation list");
                        return;
                    }

                    // save data
                    try {
                        IfnmWriter.write(NATIONS_STORE, nations);
                    } catch (IOException e) {
                        LOGGER.info("Cannot save new nation to the store");
                    }

                }

                if (cLine.hasOption(removeOption.getOpt())) {

                    // get name of the thing to remove
                    String refRemove = cLine.getOptionValue(removeOption.getOpt())
                            .trim()
                            .toLowerCase()
                            .replace(" ", "_");

                    // get the list of nations already there
                    List<IfnmNation> nations;
                    try {
                        nations = IfnmReader.read(NATIONS_STORE);
                    } catch (IOException e) {
                        LOGGER.info("Could not load nations list. Cannot remove it from something unloaded.");
                        return;
                    }

                    // find the things to remove, remove them
                    for (IfnmNation nation : nations) {
                        if (nation.getName().equals(refRemove)) {
                            nations.remove(nation);
                        }
                    }

                    // save new version
                    try {
                        IfnmWriter.write(NATIONS_STORE, nations);
                    } catch (IOException e) {
                        LOGGER.info("Could not save the modified nations list to file.");
                    }

                }

                if (cLine.hasOption(listOption.getOpt())) {
                    try {
                        // load lines for display
                        List<String> lines = IfnmReader.list(IfnmReader.read(NATIONS_STORE));
                        lines.forEach(System.out::println); // print
                    } catch (IOException e) {
                        LOGGER.info("Could not load data for display");
                        return;
                    }

                }

                if (cLine.hasOption(connectOption.getOpt())) {

                    // load data
                    List<IfnmNation> nations;
                    try {
                        nations = IfnmReader.read(NATIONS_STORE);
                    } catch (IOException e) {
                        LOGGER.info("Could not load data for connecting");
                        return;
                    }

                    // create coder
                    IfnmCoder coder = new IfnmCoder(password);

                    // do connection
                    for (IfnmNation nation : nations) {
                        try { // try connection

                            NSConnection connection = new NSConnection(IfnmConnectWindow.createApiQuery(nation.getName()));
                            Map<String, String> entries = new HashMap<>();
                            entries.put("Password", coder.decrypt(nation.getPassword()));
                            connection.setHeaders(entries);
                            connection.connect();

                        } catch (IOException e) {
                            // report errors
                            System.err.printf("IOException on connection to %s%n", nation.getName());
                        }
                    }
                }

            } catch (ParseException e) {

                String opts = String.join(" ", args);
                LOGGER.info("Cannot parse options: " + opts);
                e.printStackTrace();

                System.out.println();
                formatter.printHelp("java -jar IflyNationManager.jar", options);

            }

        } else { // not headless, launch GUI

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

            EventQueue.invokeLater(() -> {
                try {
                    IflyNationManager window = new IflyNationManager();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Create the application.
     */
    private IflyNationManager() {

        // Load cryptographic information
        String password = passwordPrompt(Files.exists(PASS_HASH_STORE));

        try { // verification
            if (!IfnmAuthenticator.verify(PASS_HASH_STORE, password)) {

                boolean toDelete = IflyDialogs.showConfirmDialog(null,
                        "Incorrect password. Delete existing hash and store to restart?",
                        "Authentication error", true);

                if (!toDelete) {
                    System.exit(0); // exit
                }

                toDelete = IflyDialogs.showConfirmDialog(null,
                        "Are you sure you want to delete the existing hash and nations store?",
                        "Confirm", true);

                if (toDelete) {
                    Files.delete(PASS_HASH_STORE);
                    Files.delete(NATIONS_STORE);
                    IflyDialogs.showMessageDialog(null,
                            "Deleted existing hash and nations file.",
                            "Done");

                } else {
                    System.exit(0); // exit
                }
            }

        } catch (IOException e) {

            if (Files.notExists(PASS_HASH_STORE)) {
                String hash = IfnmAuthenticator.generateHash(password);
                try {
                    IfnmAuthenticator.saveHash(PASS_HASH_STORE, hash);
                } catch (IOException e1) {
                    IflyDialogs.showMessageDialog(null,
                            "Could not save password to file for future authentication.",
                            "Error");
                }

            } else {
                IflyDialogs.showMessageDialog(null, "Could not check password. Terminating.", "Error");
                System.exit(0); // exit
            }

        }

        // initialise the coder
        coder = new IfnmCoder(password);

        // delete the password
        //noinspection UnusedAssignment
        password = null;
        System.gc();

        // get the stored data
        List<IfnmNation> nationList = new ArrayList<>();
        if (Files.exists(NATIONS_STORE)) {
            try {
                nationList = IfnmReader.read(NATIONS_STORE);
            } catch (IOException e) {
                IflyDialogs.showMessageDialog(null, "Could not load stored nation data", "Error");

                // automatically move old file
                try {
                    Files.move(NATIONS_STORE, NATIONS_STORE.getParent()
                            .resolve(String.format("nation-store-old-%s.txt",
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(Instant.now()))));
                } catch (IOException e1) {
                    IflyDialogs.showMessageDialog(null, "Unable to back up old nation data", "Error");
                    System.exit(0);
                }
            }
        }

        initialise(nationList);
    }

    /**
     * Initialise the contents of the frame.
     */
    private void initialise(List<IfnmNation> nationList) {

        frame = new JFrame();
        frame.setTitle("ifly Nation Manager " + IflyNationManager.VERSION.toString());

        frame.setBounds(100, 100, 400, 600);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        DefaultListModel<IfnmNation> listModel = new DefaultListModel<>();

        // If available, load data
        nationList.forEach(listModel::addElement);

        list = new JList<>(listModel);
        list.setSelectionModel(new DefaultListSelectionModel() {
            private static final long serialVersionUID = 1L;
            boolean gestureStarted = false;

            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (!gestureStarted) {
                    if (isSelectedIndex(index0)) {
                        super.removeSelectionInterval(index0, index1);
                    } else {
                        super.addSelectionInterval(index0, index1);
                    }
                }
                gestureStarted = true;
            }

            @Override
            public void setValueIsAdjusting(boolean isAdjusting) {
                if (!isAdjusting) {
                    gestureStarted = false;
                }
            }
        });
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
//        list.setCellRenderer(new DefaultListCellRenderer() {
//            @Override
//            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
//                                                          boolean cellHasFocus) {
//                IfnmNation nation = (IfnmNation) value;
//                if (!nation.exists()) setBackground(Color.RED);
//                return this;
//            }
//        });

        JButton btnAdd = new JButton("+");
        btnAdd.setToolTipText("Add a new nation");
        btnAdd.addActionListener(e -> addNation(listModel));

        JButton btnRemove = new JButton("—");    // em-dash
        btnRemove.setToolTipText("Remove selected nations");
        btnRemove.addActionListener(ae -> list.getSelectedValuesList().forEach(listModel::removeElement));

        JButton btnConnect = new JButton("Connect");
        btnConnect.setToolTipText("Connect to selected nations");
        btnConnect.addActionListener(ae -> connectNations());

        JButton btnShow = new JButton("Show");
        btnShow.setToolTipText(
                "Show selected nations in your browser. Only opens nations which are not marked with an (*), and therefore, "
                        + "have a good chance of existing.");
        btnShow.addActionListener(ae -> showInBrowser());

        JScrollPane scrollPane = new JScrollPane(list);

        JButton btnInspector = new JButton("i");
        btnInspector.setToolTipText("Open inspector for at-a-glance details about selected nations");
        btnInspector.addActionListener(e -> openInspector());

        GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
        groupLayout.setHorizontalGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 438, Short.MAX_VALUE)
                                        .addGroup(groupLayout.createSequentialGroup()
                                                .addComponent(btnAdd, GroupLayout.PREFERRED_SIZE, 25,
                                                        GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(ComponentPlacement.RELATED)
                                                .addComponent(btnRemove, GroupLayout.PREFERRED_SIZE, 25,
                                                        GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(ComponentPlacement.RELATED)
                                                .addComponent(btnConnect)
                                                .addPreferredGap(ComponentPlacement.RELATED)
                                                .addComponent(btnShow)
                                                .addPreferredGap(ComponentPlacement.RELATED, 177, Short.MAX_VALUE)
                                                .addComponent(btnInspector, GroupLayout.PREFERRED_SIZE, 20,
                                                        GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap()));
        groupLayout.setVerticalGroup(
                groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addGroup(groupLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(btnAdd, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnRemove, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnConnect, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnInspector, GroupLayout.PREFERRED_SIZE, 20,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnShow, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap()));
        frame.getContentPane().setLayout(groupLayout);

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        JMenuItem mntmClose = new JMenuItem("Close");
        mntmClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        mntmClose.addActionListener((ae) -> {
            frame.setVisible(false);
            frame.dispose();
        });
        mnFile.add(mntmClose);

        mnFile.addSeparator();

        JMenuItem mntmSave = new JMenuItem("Save");
        mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        mntmSave.addActionListener((ae) -> {
            try {
                save();
            } catch (IOException e) {
                IflyDialogs.showMessageDialog(frame, "Could not save nation data to file.", "Error");
            }
        });
        mnFile.add(mntmSave);

        JMenuItem mntmLoad = new JMenuItem("Load");
        mntmLoad.setToolTipText("Imported files must have been generated with the same salt and password as this session");
        mntmLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        mntmLoad.addActionListener((ae) -> loadFile(listModel));
        mnFile.add(mntmLoad);

        JSeparator separator = new JSeparator();
        mnFile.add(separator);

        JMenuItem mntmImport = new JMenuItem("Import From CSV");
        mntmImport.setToolTipText("CSV should be in form 'nation,password' on individual lines");
        mntmImport.addActionListener((ae) -> importCSV(listModel));
        mnFile.add(mntmImport);

        JSeparator separator_1 = new JSeparator();
        mnFile.add(separator_1);

        JMenuItem mntmOpenFolder = new JMenuItem("Open Data Folder");
        mntmOpenFolder.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                InputEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        mntmOpenFolder.addActionListener((ae) -> openDataDirectory());
        mnFile.add(mntmOpenFolder);

        JMenu mnWindow = new JMenu("Window");
        menuBar.add(mnWindow);

        JMenuItem mntmMinimise = new JMenuItem("Minimise");
        mntmMinimise.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        mntmMinimise.addActionListener((ae) -> {
            if (frame.getState() == Frame.NORMAL) {
                frame.setState(Frame.ICONIFIED);
            }
        });
        mnWindow.add(mntmMinimise);

        JMenu mnHelp = new JMenu("Help");
        menuBar.add(mnHelp);

        JMenuItem mntmAbout = new JMenuItem("About");
        mntmAbout.addActionListener(ae -> {

        });
        mnHelp.add(mntmAbout);

        JMenuItem mntmOpenHelp = new JMenuItem("Open Help...");
        mntmOpenHelp.addActionListener((ae) -> {

        });
        mnHelp.add(mntmOpenHelp);

        frame.setLocationRelativeTo(null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                save();
            } catch (IOException e) {
                LOGGER.info("Saved");
                e.printStackTrace();
                // otherwise, do nothing
            }
        }));
    }

    /**
     * Adds nation to list provided
     * @param listModel to which to add nation
     */
    private void addNation(DefaultListModel<IfnmNation> listModel) {
        IfnmNationDialog dialog = new IfnmNationDialog(coder);
        IflyPair<String, String> pair = dialog.showDialog();
        if (!IflyStrings.isEmpty(pair.getLeft()) && !IflyStrings.isEmpty(pair.getRight())) {

            // check for duplicates
            boolean duplicate = false;
            for (int i = 0; i < listModel.getSize(); i++) {
                if (listModel.getElementAt(i).getName().equals(pair.getLeft())) {
                    duplicate = true;
                }
            }

            // throw error if duplicate
            IfnmNation nation = new IfnmNation(pair.getLeft(), pair.getRight());
            if (duplicate) {
                IflyDialogs.showMessageDialog(frame, "Nation \"" + nation.getName() + "\" is already in the "
                        + "list.\nRemove it first.", "Error");
                return;
            }

            listModel.addElement(nation); // otherwise add
        }
    }

    /**
     * Sets up the {@link IfnmConnectWindow} to the selected nations in the list, then invokes.
     */
    private void connectNations() {
        EventQueue.invokeLater(() -> {
            // get the selected values
            List<IfnmNation> nations = list.getSelectedValuesList();
            if (nations.isEmpty()) {
                // stop if no nations are selected
                IflyDialogs.showMessageDialog(frame, "No nations selected", "Error");
                return;
            }

            // do connection
            IfnmConnectWindow progressDialog = new IfnmConnectWindow();
            progressDialog.showDialog(nations, coder);

        });
    }

    /**
     * Shows selected nations in system browser.
     */
    private void showInBrowser() {
        Desktop desktop = Desktop.getDesktop();
        list.getSelectedValuesList().stream()
                .map(IfnmNation::getLeft)
                .forEach(n -> {
                    try {
                        desktop.browse(new URI("https://www.nationstates.net/nation=" + n));
                    } catch (IOException | URISyntaxException e) {

                        e.printStackTrace();
                    }
                });
    }

    /**
     * Opens nations in inspector
     */
    private void openInspector() {
        List<IfnmNation> items = list.getSelectedValuesList().stream()
                .filter(IfnmNation::exists)
                .collect(Collectors.toList());
        if (!items.isEmpty()) {
            new IfnmInspector(items);
        } else {
            IflyDialogs.showMessageDialog(frame, "No living nations selected.", "Message");
        }
    }

    /**
     * Loads a preexisting file. Should be configured to be hash-compatible. Otherwise, will be unable to decrypt
     * passwords.
     * @param listModel in which to deposit information
     */
    private void loadFile(DefaultListModel<IfnmNation> listModel) {
        if (!listModel.isEmpty()) {
            boolean value = IflyDialogs.showConfirmDialog(frame,
                    "This will overwrite data. Continue?", "Warning", true);
            if (!value) {
                return;
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose hash-compatible storage file...");
        fileChooser.setCurrentDirectory(PERSIST_DIR.toFile());
        fileChooser.showOpenDialog(frame);
        Path savedFile = fileChooser.getSelectedFile().toPath();

        listModel.clear();
        try {
            IfnmReader.read(savedFile).forEach(listModel::addElement);
        } catch (IOException e) {
            IflyDialogs.showMessageDialog(frame,
                    String.format("Could not load data from %s", savedFile.toString()),
                    "Error");
        }
    }

    private void openDataDirectory() {
        try {
            Desktop.getDesktop().browse(PERSIST_DIR.toUri());
        } catch (IOException e1) {
            IflyDialogs.showMessageDialog(frame, "Cannot open folder.", "Error");
            e1.printStackTrace();
        }
    }

    /**
     * Imports data from CSV and puts it into the provided list model
     * @param listModel in which to deposit information
     */
    private void importCSV(DefaultListModel<IfnmNation> listModel) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose applicable CSV file...");
        fileChooser.setCurrentDirectory(PERSIST_DIR.toFile());
        fileChooser.showOpenDialog(frame);
        Path savedFile = fileChooser.getSelectedFile().toPath();

        try {
            List<String> lines = Files.readAllLines(savedFile);
            List<String> failures = new ArrayList<>();
            for (String element : lines) {
                String[] sides = element.split(",");
                try {
                    IfnmNation nation = new IfnmNation(sides[0], coder.encrypt(sides[1]));
                    listModel.addElement(nation);

                } catch (IndexOutOfBoundsException e) {
                    LOGGER.info("Error. Cannot process line: " + element);
                    failures.add(sides[0].trim());
                }
            }

            if (failures.size() > 0) {
                IflyDialogs.showMessageDialog(frame,
                        failures.size() > 1
                                ? "Errors"
                                : "Error"
                                + "in processing " + String.join(",", failures),
                        "Error");
            }

        } catch (IOException e1) {
            IflyDialogs.showMessageDialog(frame, "Cannot import from CSV.", "Error");
        }
    }

    /**
     * Prompts user for password
     * @param hashExists if true, then asks for existing password, otherwise, asks for new master password
     * @return user input to prompt
     * @throws HeadlessException if headless, from {@link JOptionPane#showOptionDialog(Component, Object, String, int,
     *                           int, Icon, Object[], Object)}
     */
    private String passwordPrompt(boolean hashExists) throws HeadlessException {
        JPanel panel = new JPanel();

        // change text if hash exists or not
        JLabel label = new JLabel(hashExists
                ? "Input your master password."
                : "Create a new master password.");

        JPasswordField passwordField = new JPasswordField();
        panel.setLayout(new GridLayout(2, 1, 5, 5));
        panel.add(label);
        panel.add(passwordField);

        passwordField.requestFocusInWindow();
        int option = JOptionPane.showOptionDialog(null, panel, "Authentication",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, null, "");

        if (option == JOptionPane.OK_OPTION) {
            String pass = new String(passwordField.getPassword());
            if (!IflyStrings.isEmpty(pass))
                return new String(passwordField.getPassword());
        }

        return "ifnmDefaultPassword"; // return a default password
    }

    /**
     * Saves information to file using {@link IfnmWriter}, after converting to <code>List&lt;IfnmNation&gt;</code>.
     * @throws IOException from {@link IfnmWriter#write(Path, List)}
     */
    private void save() throws IOException {
        List<IfnmNation> nations = new ArrayList<>();
        for (int i = 0; i < list.getModel().getSize(); i++)
            nations.add(list.getModel().getElementAt(i));

        // only save if not empty
        if (!nations.isEmpty()) IfnmWriter.write(NATIONS_STORE, nations);
    }

}
