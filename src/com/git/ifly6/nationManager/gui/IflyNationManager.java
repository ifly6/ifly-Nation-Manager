package com.git.ifly6.nationManager.gui;

import com.git.ifly6.communique.CommuniqueUtilities;
import com.git.ifly6.iflyLibrary.IflyDialogs;
import com.git.ifly6.iflyLibrary.IflyStrings;
import com.git.ifly6.iflyLibrary.IflyVersion;
import com.git.ifly6.iflyLibrary.generics.IflyPair;
import com.git.ifly6.nationManager.IfnmAuthenticator;
import com.git.ifly6.nationManager.IfnmCoder;
import com.git.ifly6.nationManager.IfnmReader;
import com.git.ifly6.nationManager.IfnmWriter;
import com.git.ifly6.nationManager.gui.components.IfnmList;
import com.git.ifly6.nationManager.gui.components.IfnmNativisation;
import com.git.ifly6.nsapi.ApiUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.git.ifly6.nationManager.gui.components.IfnmNativisation.NATIONS_STORE;
import static com.git.ifly6.nationManager.gui.components.IfnmNativisation.PASS_HASH_STORE;
import static com.git.ifly6.nationManager.gui.components.IfnmNativisation.PERSIST_DIR;

public class IflyNationManager {

    private static final Logger LOGGER = Logger.getLogger(IflyNationManager.class.getName());

    @SuppressWarnings("WeakerAccess")
    public static final IflyVersion VERSION = new IflyVersion(3, 0);

    private IfnmCoder coder;

    private JFrame frame;
    private IfnmList list;
    private DefaultListModel<IfnmNation> listModel;

    public static void main(String[] args) {
        IfnmNativisation.initialise(); // initialise needed data
        IfnmNativisation.nativise(); // nativise for macOS

        EventQueue.invokeLater(() -> {
            try {
                IflyNationManager window = new IflyNationManager();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Create the application.
     */
    private IflyNationManager() {

        // Load cryptographic information
        String password = passwordPrompt(Files.exists(PASS_HASH_STORE));

        try { // verification
            if (!IfnmAuthenticator.verify(PASS_HASH_STORE, password)) {
                boolean deleting = IflyDialogs.showConfirmDialog(null,
                        "Incorrect password. Delete existing hash and store to restart?",
                        "Authentication error", true);

                if (!deleting)
                    System.exit(0); // exit

                deleting = IflyDialogs.showConfirmDialog(null,
                        "Are you sure you want to delete the existing hash and nations store?",
                        "Confirm", true);

                if (deleting) {
                    Files.delete(PASS_HASH_STORE);
                    Files.delete(NATIONS_STORE);
                    IflyDialogs.showMessageDialog(null,
                            "Deleted existing hash and nations file.",
                            "Done");

                } else
                    System.exit(0); // exit
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

        // initialise the coder and get stored data
        coder = new IfnmCoder(password);
        List<IfnmNation> nationList = new ArrayList<>();
        if (Files.exists(NATIONS_STORE)) {
            try {
                nationList = IfnmReader.read(NATIONS_STORE);
            } catch (IOException e) {
                IflyDialogs.showMessageDialog(null, "Could not load stored nation data", "Error");

                // automatically move old file
                try {
                    Files.move(NATIONS_STORE, NATIONS_STORE.getParent().resolve(
                            String.format("nation-store-old-%s.txt", CommuniqueUtilities.dateTimeFormat())
                    ));
                } catch (IOException e1) {
                    IflyDialogs.showMessageDialog(null, "Unable to back up old nation data", "Error");
                    System.exit(1);
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

        frame.setMinimumSize(new Dimension(400, 500));
        frame.setBounds(100, 100, 400, 600);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        list = IfnmList.createList(nationList);
        listModel = list.getListModel();

        JButton btnAdd = new JButton("+");
        btnAdd.setToolTipText("Add a new nation");
        btnAdd.addActionListener(e -> addNation(listModel));

        JButton btnRemove = new JButton("—");    // em-dash
        btnRemove.setToolTipText("Remove selected nations");
        btnRemove.addActionListener(ae -> list.getSelected().forEach(listModel::removeElement));

        JButton btnConnect = new JButton("Connect");
        btnConnect.setToolTipText("Connect to selected nations");
        btnConnect.addActionListener(ae -> connectNations());

        JButton btnShow = new JButton("Show");
        btnShow.setToolTipText(
                "Show selected nations in your browser");
        btnShow.addActionListener(ae -> showInBrowser());

        {  // main content panel
            JPanel content = new JPanel();
            content.setLayout(new BorderLayout());
            content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            content.add(new JScrollPane(list), BorderLayout.CENTER);

            {  // bottom content with the two panels on bottom left and bottom right
                JPanel bottomContent = new JPanel();
                bottomContent.setLayout(new BorderLayout());
                bottomContent.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

                JPanel bottomLeft = new JPanel();
                bottomLeft.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
                bottomLeft.add(btnAdd);
                bottomLeft.add(btnRemove);

                JPanel bottomRight = new JPanel();
                bottomRight.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                bottomRight.add(btnShow);
                bottomRight.add(btnConnect);

                bottomContent.add(bottomLeft, BorderLayout.WEST);
                bottomContent.add(bottomRight, BorderLayout.EAST);

                content.add(bottomContent, BorderLayout.SOUTH);
            }

            frame.add(content);
        }

        initialiseMenubar();
    }

    private void initialiseMenubar() {

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

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

        mnFile.addSeparator();

        JMenuItem mntmImport = new JMenuItem("Import From CSV");
        mntmImport.setToolTipText("CSV should be in form 'nation,password' on individual lines");
        mntmImport.addActionListener((ae) -> importCSV(listModel));
        mnFile.add(mntmImport);

        mnFile.addSeparator();

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
            if (frame.getState() == Frame.NORMAL)
                frame.setState(Frame.ICONIFIED);
        });
        mnWindow.add(mntmMinimise);

        frame.setLocationRelativeTo(null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                save();
            } catch (IOException e) {
                LOGGER.severe("Failed to save");
                e.printStackTrace();
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
        if (ApiUtils.isNotEmpty(pair.getLeft()) && ApiUtils.isNotEmpty(pair.getRight())) {
            if (list.contains(pair.getLeft())) { // check for duplicates
                // throw error if duplicate
                IflyDialogs.showMessageDialog(frame,
                        String.format("Nation \"%s\" is already in the list.\nRemove it first.", pair.getLeft()),
                        "Error");
                return; // cancel
            }

            // otherwise add
            IfnmNation nation = new IfnmNation(pair.getLeft(), pair.getRight());
            listModel.addElement(nation);
        }
    }

    /**
     * Sets up the {@link IfnmConnectWindow} to the selected nations in the list, then invokes.
     */
    private void connectNations() {
        EventQueue.invokeLater(() -> {
            // get selected
            List<IfnmNation> nations = list.getSelected();
            if (nations.isEmpty())
                nations = list.getAll();

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
        list.getSelected().stream()
                .filter(IfnmNation::exists)
                .map(IfnmNation::getName)
                .forEach(n -> {
                    try {
                        desktop.browse(new URI("https://www.nationstates.net/nation=" + n));
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    }
                });
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
                String[] sides = element.split(",\\s*?");
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
                : "Create a master password.");

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
        List<IfnmNation> nations = list.getAll();
        if (!nations.isEmpty()) // only save if not empty
            IfnmWriter.write(NATIONS_STORE, nations);
    }
}
