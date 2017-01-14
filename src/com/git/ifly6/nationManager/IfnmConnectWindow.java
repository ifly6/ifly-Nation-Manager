/* Copyright (c) 2017 Kevin Wong. All Rights Reserved. */
package com.git.ifly6.nationManager;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;

import com.git.ifly6.nsapi.NSConnection;

/** @author ifly6 */
public class IfnmConnectWindow extends JFrame {
	
	private static final long serialVersionUID = IflyNationManager.VERSION.major;
	
	private final JPanel contentPanel = new JPanel();
	private JTextArea textArea;
	private JProgressBar progressBar;
	private JButton btnOk;
	
	/** Create the dialog. */
	public IfnmConnectWindow() {
		
		setTitle("Connecting...");
		setBounds(100, 100, 400, 300);
		
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		
		JLabel lblProgressInConnections = new JLabel("Detailed connection progress:");
		
		textArea = new JTextArea();
		textArea.setBorder(new EmptyBorder(5, 5, 5, 5));
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		textArea.setEditable(false);
		
		JScrollPane scrollPane = new JScrollPane(textArea);
		
		btnOk = new JButton("Ok");
		btnOk.addActionListener((ae) -> {
			setVisible(false);
			dispose();
		});
		
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
				gl_contentPanel.createParallelGroup(Alignment.TRAILING)
						.addGroup(gl_contentPanel.createSequentialGroup()
								.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 381, Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(btnOk, GroupLayout.PREFERRED_SIZE, 81, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_contentPanel.createSequentialGroup()
								.addComponent(lblProgressInConnections)
								.addContainerGap(279, Short.MAX_VALUE))
						.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE));
		gl_contentPanel.setVerticalGroup(
				gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPanel.createSequentialGroup()
								.addComponent(lblProgressInConnections)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING, false)
										.addComponent(btnOk, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE)
										.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 24,
												GroupLayout.PREFERRED_SIZE))));
		contentPanel.setLayout(gl_contentPanel);
		
		pack();
		setLocationRelativeTo(null);	// centre
	}
	
	public void showDialog(List<IfnmNation> nations, char[] password, byte[] salt) {
		
		new Thread(new Runnable() {
			@Override public void run() {
				setVisible(true);
				
				btnOk.setEnabled(false);
				for (int i = 0; i < nations.size(); i++) {
					
					IfnmNation nation = nations.get(i);
					String name = nation.getLeft();
					String encryptedPass = nation.getRight();
					try {
						NSConnection connection =
								new NSConnection(NSConnection.API_PREFIX + "nation=" + name + "&q=unread");
						Map<String, String> entries = new HashMap<>();
						entries.put("Password", new IfnmCipher(password, salt).decrypt(encryptedPass));
						connection.setHeaders(entries);
						connection.connect();
						
						appendText("Connected to \"" + nation.getName() + "\".");
						nation.setExists(true);
						
					} catch (FileNotFoundException e) {
						appendText("ERROR: \"" + nation.getName() + "\" does not exist.");
						nation.setExists(false);
						
					} catch (GeneralSecurityException | IOException e) {
						appendText("ERROR: Cannot connect to NationStates.");
						e.printStackTrace();
					}
					
					setProgress(i + 1, nations.size());
				}
				btnOk.setEnabled(true);
				
			}
		}).start();
	}
	
	public IfnmConnectWindow appendText(String input) {
		if (textArea.getText() == null || textArea.getText().trim().length() == 0) {
			textArea.setText(input);
		} else {
			textArea.append("\n" + input);
			textArea.setCaretPosition(textArea.getText().length());
		}
		return this;
	}
	
	public IfnmConnectWindow setProgress(int n, int max) {
		progressBar.setMaximum(max);
		progressBar.setValue(n);
		return this;
	}
	
	public IfnmConnectWindow setProgress(int n) {
		setProgress(n, progressBar.getMaximum());
		return this;
	}
}
