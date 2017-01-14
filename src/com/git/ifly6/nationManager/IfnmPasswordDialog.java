/* Copyright (c) 2017 Kevin Wong. All Rights Reserved. */
package com.git.ifly6.nationManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;

import com.git.ifly6.iflyLibrary.generics.IflyPair;

/** @author ifly6 */
public class IfnmPasswordDialog extends JDialog {
	
	private static final long serialVersionUID = IflyNationManager.VERSION.major;
	
	private final JPanel contentPanel = new JPanel();
	private JTextField txtUsername;
	private JPasswordField passwordField;
	
	private char[] password;
	private byte[] salt;
	
	/** Create the dialog. */
	public IfnmPasswordDialog(char[] password, byte[] salt) {
		
		this.password = password;
		this.salt = salt;
		
		Dimension screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
		double sWidth = screenDimensions.getWidth();
		double sHeight = screenDimensions.getHeight();
		int windowWidth = 300;
		int windowHeight = 150;
		setBounds((int) (sWidth / 2 - windowWidth / 2), (int) (sHeight / 2 - windowHeight / 2), windowWidth,
				windowHeight);
		setMaximumSize(new Dimension(windowHeight, windowHeight));
		setMinimumSize(new Dimension(windowWidth, windowHeight));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		txtUsername = new JTextField();
		
		JLabel lblUsername = new JLabel("Nation name");
		
		passwordField = new JPasswordField();
		
		JLabel lblPassword = new JLabel("Password");
		
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
				gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPanel.createSequentialGroup()
								.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
										.addComponent(lblUsername)
										.addComponent(lblPassword))
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
										.addComponent(passwordField, GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
										.addComponent(txtUsername, GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE))));
		gl_contentPanel.setVerticalGroup(
				gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPanel.createSequentialGroup()
								.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
										.addComponent(txtUsername, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addComponent(lblUsername))
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
										.addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addComponent(lblPassword))
								.addContainerGap(171, Short.MAX_VALUE)));
		contentPanel.setLayout(gl_contentPanel);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(ae -> {
					setVisible(false);
					dispose();
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}
	
	public IflyPair<String, String> showDialog() {
		setVisible(true);
		return new IflyPair<>(txtUsername.getText(), encrypt(new String(passwordField.getPassword())));
	}
	
	private String encrypt(String data) {
		try {
			return encrypt(data, password, salt);
		} catch (UnsupportedEncodingException | GeneralSecurityException e) {
			JOptionPane.showMessageDialog(this, "Cannot encrypt password", "Error", JOptionPane.PLAIN_MESSAGE, null);
			e.printStackTrace();
			return "";	// return nothing
		}
	}
	
	public static String encrypt(String data, char[] password, byte[] salt)
			throws UnsupportedEncodingException, GeneralSecurityException {
		IfnmCipher cipher = new IfnmCipher(password, salt);
		return cipher.encrypt(data);
	}
}
