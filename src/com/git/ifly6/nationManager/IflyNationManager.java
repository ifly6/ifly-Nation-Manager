/* Copyright (c) 2017 Kevin Wong. All Rights Reserved. */
package com.git.ifly6.nationManager;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

import com.git.ifly6.iflyLibrary.IflyDialogs;
import com.git.ifly6.iflyLibrary.IflyStrings;
import com.git.ifly6.iflyLibrary.IflySystem;
import com.git.ifly6.iflyLibrary.IflyVersion;
import com.git.ifly6.iflyLibrary.generics.IflyPair;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/** @author ifly6 */
public class IflyNationManager {
	
	public static final IflyVersion VERSION = new IflyVersion(1, 0, 0);
	private static Path PERSIST_PATH;
	private static Path NATIONS_STORE;
	
	private JFrame frame;
	private char[] password;
	private byte[] salt;
	private JList<IfnmNation> list;
	
	/** Launch the application. */
	public static void main(String[] args) {
		
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
		
		EventQueue.invokeLater(() -> {
			try {
				IflyNationManager window = new IflyNationManager();
				window.frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	/** Create the application.
	 * @wbp.parser.entryPoint */
	public IflyNationManager() {
		
		try {
			Files.createDirectories(PERSIST_PATH);
		} catch (IOException e) { // nothing
		}
		
		initialise();
	}
	
	/** Initialise the contents of the frame. */
	private void initialise() {
		
		frame = new JFrame();
		frame.setTitle("ifly Nation Manager " + IflyNationManager.VERSION.toString());
		
		frame.setBounds(100, 100, 400, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		{	// Load cryptographic information
			password = passwordPrompt();
			if (salt == null) {
				try {
					salt = loadSalt();
					
				} catch (Exception e) {
					salt = new byte[8];
					new SecureRandom().nextBytes(salt);
					saveSalt();
				}
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
		btnAdd.addActionListener(e -> {
			IfnmPasswordDialog dialog = new IfnmPasswordDialog(password, salt);
			IflyPair<String, String> pair = dialog.showDialog();
			if (!IflyStrings.isEmpty(pair.getLeft()) && !IflyStrings.isEmpty(pair.getRight())) {
				
				List<String> nationNames = new ArrayList<>();
				for (int i = 0; i < listModel.getSize(); i++) {
					nationNames.add(listModel.getElementAt(i).getName());
				}
				
				IfnmNation nation = new IfnmNation(pair.getLeft(), pair.getRight());
				if (nationNames.contains(nation.getName())) {
					JOptionPane.showMessageDialog(frame,
							String.format("Nation \"%s\" is already in the list.%nRemove it first.", nation.getName()),
							"Error", JOptionPane.PLAIN_MESSAGE, null);
					return;
				}
				
				listModel.addElement(nation);
			}
		});
		
		JButton btnRemove = new JButton("—");
		btnRemove.addActionListener(ae -> IntStream.of(list.getSelectedIndices())
				.mapToObj(listModel::getElementAt)
				.forEach(n -> listModel.removeElement(n)));
		
		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ae) {
				List<IfnmNation> nations = IntStream.of(list.getSelectedIndices())
						.mapToObj(listModel::getElementAt).collect(Collectors.toList());
				EventQueue.invokeLater(() -> {
					IfnmConnectWindow progressDialog = new IfnmConnectWindow();
					progressDialog.showDialog(nations, password, salt);
				});
			}
		});
		
		JButton btnShow = new JButton("Show");
		btnShow.addActionListener(ae -> {
			Desktop desktop = Desktop.getDesktop();
			IntStream.of(list.getSelectedIndices())
					.mapToObj(listModel::getElementAt)
					.filter(n -> n.exists())
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
		btnInspector.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				List<IfnmNation> items = IntStream.of(list.getSelectedIndices())
						.mapToObj(listModel::getElementAt)
						.filter(n -> n.exists()).collect(Collectors.toList());
				new IfnmInspector(items);
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
		mntmLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mntmLoad.addActionListener((ae) -> {
			
			if (!listModel.isEmpty()) {
				int value = JOptionPane.showConfirmDialog(frame,
						"This will overwrite unsaved data. Continue?", "Warning",
						JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null);
				if (value == JOptionPane.NO_OPTION) { return; }
			}
			
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Choose applicable JSON file...");
			fileChooser.setCurrentDirectory(PERSIST_PATH.toFile());
			fileChooser.showOpenDialog(frame);
			Path savedFile = fileChooser.getSelectedFile().toPath();
			
			listModel.clear();
			loadData(savedFile).getNations().stream().forEach(i -> listModel.addElement(i));
			
		});
		mnFile.add(mntmLoad);
		
		JSeparator separator = new JSeparator();
		mnFile.add(separator);
		
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
		
		if (option == JOptionPane.OK_OPTION) { return passwordField.getPassword(); }
		return new char[0];
	}
	
	private void autosave() {
		List<IfnmNation> nations = new ArrayList<>();
		for (int i = 0; i < list.getModel().getSize(); i++) {
			nations.add(list.getModel().getElementAt(i));
		}
		saveData(new IfnmData(nations), NATIONS_STORE);
	}
	
	private void saveData(IfnmData data, Path dataFile) {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String json = gson.toJson(data);
			Files.write(dataFile, Arrays.asList(json));
			
		} catch (JsonIOException | IOException e) {
			IflyDialogs.showMessageDialog(this.frame, "Cannot save data.", "Error");
			e.printStackTrace();
		}
	}
	
	private IfnmData loadData(Path dataFile) {
		Gson gson = new Gson();
		try {
			return gson.fromJson(Files.newBufferedReader(dataFile), IfnmData.class);
		} catch (JsonSyntaxException | JsonIOException | IOException e) {
			IflyDialogs.showMessageDialog(this.frame, "Cannot load data.", "Error");
			e.printStackTrace();
			return new IfnmData(new ArrayList<IfnmNation>());
		}
	}
	
	private void saveSalt() {
		try {
			OutputStream os = Files.newOutputStream(PERSIST_PATH.resolve("salt-data"));
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(salt);
		} catch (IOException e) {
			IflyDialogs.showMessageDialog(frame, "Cannot save cryptographic information.", "Error");
			e.printStackTrace();
		}
	}
	
	private byte[] loadSalt() throws IOException, ClassNotFoundException {
		InputStream is = Files.newInputStream(PERSIST_PATH.resolve("salt-data"));
		ObjectInputStream ois = new ObjectInputStream(is);
		return (byte[]) ois.readObject();
	}
	
}
