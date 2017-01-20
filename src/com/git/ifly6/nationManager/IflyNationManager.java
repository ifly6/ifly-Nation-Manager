/* Copyright (c) 2017 Kevin Wong. All Rights Reserved. */
package com.git.ifly6.nationManager;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.git.ifly6.iflyLibrary.IflyDialogs;
import com.git.ifly6.iflyLibrary.IflyStrings;
import com.git.ifly6.iflyLibrary.IflySystem;
import com.git.ifly6.iflyLibrary.IflyVersion;
import com.git.ifly6.iflyLibrary.generics.IflyPair;
import com.git.ifly6.nsapi.NSConnection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/** @author ifly6 */
public class IflyNationManager {
	
	public static final IflyVersion VERSION = new IflyVersion(0, 2);
	private static Path PERSIST_PATH;
	private static Path NATIONS_STORE;
	private static Path HASH_STORE;
	private static Path SALT_LOCATION;
	
	private static char[] password;
	private static byte[] salt;
	
	private JFrame frame;
	private JList<IfnmNation> list;
	
	public static void main(String[] args) {
		
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
				password = cLine.getOptionValue(passwordOption.getOpt()).toCharArray();
				salt = getSalt();
				if (!verifyHash(createHash(password))) { return; }
				
				if (cLine.hasOption(addOption.getOpt())) {
					
					String[] addArgs = cLine.getOptionValues(addOption.getOpt());
					String nation = addArgs[0];
					String nationPass = addArgs[1];
					
					IfnmNation newNation = new IfnmNation(nation,
							IfnmConnectWindow.getNationPassword(new IfnmCipher(password, salt), nationPass));
					List<IfnmNation> nations = loadData(NATIONS_STORE).getNations();
					boolean match = nations.stream().map(IfnmNation::getName).anyMatch(s -> s.equals(newNation.getName()));
					if (!match) {
						nations.add(newNation);
					}
					saveData(new IfnmData(nations), NATIONS_STORE);
				}
				if (cLine.hasOption(removeOption.getOpt())) {
					String refRemove = cLine.getOptionValue(removeOption.getOpt()).trim().toLowerCase().replace(" ", "_");
					List<IfnmNation> nations = loadData(NATIONS_STORE).getNations();
					for (IfnmNation nation : nations) {
						if (nation.getName().equals(refRemove)) {
							nations.remove(nation);
						}
					}
					saveData(new IfnmData(nations), NATIONS_STORE);
				}
				if (cLine.hasOption(listOption.getOpt())) {
					IfnmData data = loadData(NATIONS_STORE);
					data.getNations().stream().forEach(System.out::println);
				}
				if (cLine.hasOption(connectOption.getOpt())) {
					IfnmData data = loadData(NATIONS_STORE);
					for (IfnmNation nation : data.getNations()) {
						try {
							NSConnection connection = new NSConnection(IfnmConnectWindow.createApiQuery(nation.getName()));
							Map<String, String> entries = new HashMap<>();
							entries.put("Password", new IfnmCipher(password, salt).decrypt(nation.getPassword()));
							connection.setHeaders(entries);
							connection.connect();
						} catch (GeneralSecurityException | IOException e) {
							e.printStackTrace();
							continue;
						}
					}
				}
				
			} catch (ParseException e) {
				
				String opts = Stream.of(args).collect(Collectors.joining(" "));
				System.err.println("Cannot parse options: " + opts);
				e.printStackTrace();
				
				System.out.println();
				formatter.printHelp("java -jar IflyNationManager.jar", options);
				
			}
			
		} else {	// not headless, launch GUI
			
			if (IflySystem.IS_OS_MAC) {	// Mac look-and-feel
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				System.setProperty("com.apple.mrj.application.apple.menu.about.name", "ifly Nation Manager");
			}
			
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
			
			if (IflySystem.IS_OS_MAC) {
				PERSIST_PATH =
						Paths.get(System.getProperty("user.home"), "Library", "Application Support", "ifly Nation Manager");
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				System.setProperty("com.apple.mrj.application.apple.menu.about.name",
						"Ifly Nation Manager " + IflyNationManager.VERSION.toString());
			} else {
				PERSIST_PATH = Paths.get(System.getProperty("user.dir"), "config");
			}
			
			NATIONS_STORE = PERSIST_PATH.resolve("nations-store.txt");
			HASH_STORE = PERSIST_PATH.resolve("hash-store");
			SALT_LOCATION = PERSIST_PATH.resolve("salt-data");
			
			try {
				Files.createDirectories(PERSIST_PATH);
			} catch (IOException e) { // nothing
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
	
	/** Create the application.
	 * @wbp.parser.entryPoint */
	public IflyNationManager() {
		initialise();
	}
	
	/** Initialise the contents of the frame. */
	private void initialise() {
		
		frame = new JFrame();
		frame.setTitle("ifly Nation Manager " + IflyNationManager.VERSION.toString());
		
		frame.setBounds(100, 100, 400, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		{	// Load cryptographic information
			salt = getSalt();
			password = passwordPrompt();
			String persistHash = createHash(password);
			
			if (!verifyHash(persistHash)) {
				JOptionPane.showMessageDialog(frame, "Incorrect password.", "Error",
						JOptionPane.PLAIN_MESSAGE, null);
				return;	// straight exit
			}
		}
		
		DefaultListModel<IfnmNation> listModel = new DefaultListModel<>();
		
		// If available, load data
		try {
			loadData(NATIONS_STORE).getNations().stream().forEach(n -> listModel.addElement(n));
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		
		list = new JList<>(listModel);
		list.setSelectionModel(new DefaultListSelectionModel() {
			private static final long serialVersionUID = 1L;
			boolean gestureStarted = false;
			
			@Override public void setSelectionInterval(int index0, int index1) {
				if (!gestureStarted) {
					if (isSelectedIndex(index0)) {
						super.removeSelectionInterval(index0, index1);
					} else {
						super.addSelectionInterval(index0, index1);
					}
				}
				gestureStarted = true;
			}
			
			@Override public void setValueIsAdjusting(boolean isAdjusting) {
				if (isAdjusting == false) {
					gestureStarted = false;
				}
			}
		});
		list.setFont(new Font(Font.MONOSPACED, 0, 11));
		
		JButton btnAdd = new JButton("+");
		btnAdd.setToolTipText("Add a new nation");
		btnAdd.addActionListener(e -> {
			IfnmPasswordDialog dialog = new IfnmPasswordDialog(password, salt);
			IflyPair<String, String> pair = dialog.showDialog();
			if (!IflyStrings.isEmpty(pair.getLeft()) && !IflyStrings.isEmpty(pair.getRight())) {
				
				boolean duplicate = false;
				for (int i = 0; i < listModel.getSize(); i++) {
					if (listModel.getElementAt(i).getName().equals(pair.getLeft())) {
						duplicate = true;
					}
				}
				
				IfnmNation nation = new IfnmNation(pair.getLeft(), pair.getRight());
				if (duplicate) {
					JOptionPane.showMessageDialog(frame, "Nation \"" + nation.getName() + "\" is already in the "
							+ "list.\nRemove it first.", "Error", JOptionPane.PLAIN_MESSAGE, null);
					return;
				}
				
				listModel.addElement(nation);
			}
		});
		
		JButton btnRemove = new JButton("—");	// em-dash
		btnRemove.setToolTipText("Remove selected nations");
		btnRemove.addActionListener(ae -> list.getSelectedValuesList().stream()
				.forEach(n -> listModel.removeElement(n)));
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.setToolTipText("Connect to selected nations");
		btnConnect.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ae) {
				EventQueue.invokeLater(() -> {
					IfnmConnectWindow progressDialog = new IfnmConnectWindow();
					progressDialog.showDialog(list.getSelectedValuesList(), password, salt);
				});
			}
		});
		
		JButton btnShow = new JButton("Show");
		btnShow.setToolTipText(
				"Show selected nations in your browser. Only opens nations which are not marked with an (*), and therefore, "
						+ "have a good chance of existing.");
		btnShow.addActionListener(ae -> {
			Desktop desktop = Desktop.getDesktop();
			list.getSelectedValuesList().stream()
					.filter(IfnmNation::exists)
					.map(IfnmNation::getLeft)
					.forEach(n -> {
						try {
							desktop.browse(new URI("https://www.nationstates.net/nation=" + n));
						} catch (IOException | URISyntaxException e) {
							e.printStackTrace();
						}
					});
		});
		
		JScrollPane scrollPane = new JScrollPane(list);
		
		JButton btnInspector = new JButton("i");
		btnInspector.setToolTipText("Open inspector for at-a-glance details about selected nations");
		btnInspector.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				List<IfnmNation> items = IntStream.of(list.getSelectedIndices())
						.mapToObj(listModel::getElementAt)
						.filter(n -> n.exists()).collect(Collectors.toList());
				if (!items.isEmpty()) {
					new IfnmInspector(items);
				} else {
					IflyDialogs.showMessageDialog(frame, "No living nations selected.", "Message");
				}
			}
		});
		
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
		
		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mntmSave.addActionListener((ae) -> {
			autosave();
		});
		mnFile.add(mntmSave);
		
		JMenuItem mntmLoad = new JMenuItem("Load");
		mntmLoad.setToolTipText("Imported files must have been generated with the same salt and password as this session");
		mntmLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mntmLoad.addActionListener((ae) -> {
			
			if (!listModel.isEmpty()) {
				int value = JOptionPane.showConfirmDialog(frame,
						"This will overwrite unsaved data. Continue?", "Warning",
						JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null);
				if (value == JOptionPane.NO_OPTION) { return; }
			}
			
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Choose salt-compatible JSON file...");
			fileChooser.setCurrentDirectory(PERSIST_PATH.toFile());
			fileChooser.showOpenDialog(frame);
			Path savedFile = fileChooser.getSelectedFile().toPath();
			
			listModel.clear();
			loadData(savedFile).getNations().stream().forEach(i -> listModel.addElement(i));
			
		});
		mnFile.add(mntmLoad);
		
		JSeparator separator = new JSeparator();
		mnFile.add(separator);
		
		JMenuItem mntmImport = new JMenuItem("Import From CSV");
		mntmImport.setToolTipText("CSV should be in form 'nation,password' on individual lines");
		mntmImport.addActionListener((ae) -> {
			
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Choose applicable CSV file...");
			fileChooser.setCurrentDirectory(PERSIST_PATH.toFile());
			fileChooser.showOpenDialog(frame);
			Path savedFile = fileChooser.getSelectedFile().toPath();
			
			try {
				List<String> lines = Files.readAllLines(savedFile);
				for (String element : lines) {
					String[] sides = element.split(",");
					try {
						IfnmNation nation = new IfnmNation(sides[0], IfnmPasswordDialog.encrypt(sides[1], password, salt));
						listModel.addElement(nation);
					} catch (IndexOutOfBoundsException e) {
						System.err.println("Error. Cannot process line: " + element);
						continue;
					} catch (GeneralSecurityException e) {
						throw new IOException();
					}
				}
				
			} catch (IOException e1) {
				IflyDialogs.showMessageDialog(frame, "Cannot import from CSV.", "Error");
			}
			
		});
		mnFile.add(mntmImport);
		
		JSeparator separator_1 = new JSeparator();
		mnFile.add(separator_1);
		
		JMenuItem mntmOpenFolder = new JMenuItem("Open Folder");
		mntmOpenFolder.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				InputEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mntmOpenFolder.addActionListener((ae) -> {
			try {
				Desktop.getDesktop().browse(PERSIST_PATH.toUri());
			} catch (IOException e1) {
				IflyDialogs.showMessageDialog(frame, "Cannot open folder.", "Error");
				e1.printStackTrace();
			}
		});
		mnFile.add(mntmOpenFolder);
		
		JMenu mnWindow = new JMenu("Window");
		menuBar.add(mnWindow);
		
		JMenuItem mntmMinimise = new JMenuItem("Minimise");
		mntmMinimise
				.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mntmMinimise.addActionListener((ae) -> {
			if (frame.getState() == Frame.NORMAL) {
				frame.setState(Frame.ICONIFIED);
			}
		});
		mnWindow.add(mntmMinimise);
		
		JMenuItem mntmClose = new JMenuItem("Close");
		mntmClose
				.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mntmClose.addActionListener((ae) -> {
			frame.setVisible(false);
			frame.dispose();
		});
		mnWindow.add(mntmClose);
		
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
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override public void run() {
				autosave();
			}
		}));
	}
	
	private char[] passwordPrompt() throws HeadlessException {
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Input your master password.");
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
			if (!IflyStrings.isEmpty(pass)) { return passwordField.getPassword(); }
		}
		return "ifnmDefaultPassword".toCharArray();	// return a default password
	}
	
	private void autosave() {
		List<IfnmNation> nations = new ArrayList<>();
		for (int i = 0; i < list.getModel().getSize(); i++) {
			nations.add(list.getModel().getElementAt(i));
		}
		saveData(new IfnmData(nations), NATIONS_STORE);
	}
	
	private static void saveData(IfnmData data, Path dataFile) {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String json = gson.toJson(data);
			Files.write(dataFile, Arrays.asList(json));
			
		} catch (JsonIOException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private static IfnmData loadData(Path dataFile) {
		try {
			Gson gson = new Gson();
			return gson.fromJson(Files.newBufferedReader(dataFile), IfnmData.class);
		} catch (JsonSyntaxException | JsonIOException | IOException e) {
			e.printStackTrace();
			return new IfnmData(new ArrayList<IfnmNation>());
		}
	}
	
	private static byte[] createSalt() {
		byte[] newSalt = new byte[8];
		new SecureRandom().nextBytes(newSalt);
		return newSalt;
	}
	
	private static byte[] getSalt() {
		try {
			InputStream is = Files.newInputStream(SALT_LOCATION);
			ObjectInputStream ois = new ObjectInputStream(is);
			return (byte[]) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			byte[] newSalt = createSalt();
			try {
				OutputStream os = Files.newOutputStream(SALT_LOCATION);
				ObjectOutputStream oos = new ObjectOutputStream(os);
				oos.writeObject(newSalt);
			} catch (IOException e1) {}	// pass
			return newSalt;
		}
	}
	
	/** @param persistHash
	 * @return */
	private static boolean verifyHash(String persistHash) {
		try {
			if (!Files.exists(HASH_STORE)) {	// if it doesn't exist, save hash
				Files.write(HASH_STORE, persistHash.getBytes());
				return true;
			}
			String storedHash = Files.readAllLines(HASH_STORE).get(0);
			if (!storedHash.equals(persistHash)) { return true; }
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/** @param passString
	 * @return hashed version of the key defined in {@link IfnmCipher.PERSIST} based on provided salt and password. */
	private static String createHash(char[] passString) {
		try {
			return new IfnmCipher(password, salt).encrypt(IfnmCipher.PERSIST);
		} catch (UnsupportedEncodingException | GeneralSecurityException e) {
			e.printStackTrace();
			return "";
		}
	}
}
