/* Copyright (c) 2017 Kevin Wong. All Rights Reserved. */
package com.git.ifly6.nationManager.gui;

import com.git.ifly6.javatelegram.JTelegramException;
import com.git.ifly6.nationManager.IfnmCoder;
import com.git.ifly6.nsapi.NSConnection;
import com.git.ifly6.nsapi.NSException;

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
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A window which handles the process of connecting to the provided nations and putting the calls together to decode the
 * passwords necessary for the nation.
 * @author ifly6
 */
@SuppressWarnings("UnusedReturnValue")
class IfnmConnectWindow extends JFrame {

    private static final long serialVersionUID = IflyNationManager.VERSION.major;

    private JTextArea textArea;
    private JProgressBar progressBar;
    private JButton btnOk;

    /**
     * Create the dialog.
     */
    IfnmConnectWindow() {

        setTitle("Connecting...");
        setBounds(100, 100, 400, 300);

        getContentPane().setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
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
        setLocationRelativeTo(null);    // centre
    }

    /**
     * Shows the dialog and, in another thread, connects the nation to NationStates.
     * @param nations is a list of nations with which to connect
     * @param coder   , set up for decoding the hashes back to passwords
     */
    void showDialog(List<IfnmNation> nations, IfnmCoder coder) {

        new Thread(() -> {
            setVisible(true);

            btnOk.setEnabled(false);
            for (int i = 0; i < nations.size(); i++) {

                IfnmNation nation = nations.get(i);
                String name = nation.getLeft();
                String encryptedPass = nation.getRight();
                try {
                    NSConnection connection = new NSConnection(createApiQuery(name));
                    Map<String, String> entries = new HashMap<>();
                    entries.put("Password", coder.decrypt(encryptedPass)); // decrypt
                    connection.setHeaders(entries);
                    connection.connect();

                    appendText("Connected to \"" + nation.getName() + "\".");
                    nation.setExists(true);

                } catch (NSException | FileNotFoundException e) {
                    appendText(String.format("ERROR: \"%s\" does not exist.", nation.getName()));
                    nation.setExists(false);

                } catch (JTelegramException e) {
                    appendText(String.format("ERROR: \"%s\" password is wrong.", nation.getName()));
                    appendText("\tRemove and re-add that nation to update password.");

                } catch (IOException e) {
                    appendText("ERROR: Cannot connect to NationStates.");
                    e.printStackTrace();
                }

                setProgress(i + 1, nations.size());
            }
            btnOk.setEnabled(true);

        }).start();
    }

    /**
     * Creates an API query with the proper formatting and name
     * @param name to query to
     * @return the string version of the URL with which to connect
     */
    static String createApiQuery(String name) {
        name = name.trim().toLowerCase().replace(" ", "_");
        return NSConnection.API_PREFIX + String.format("nation=%s&q=unread", name);
    }

    /**
     * Appends text to the text area at the centre of the dialog
     * @param input to add
     * @return the dialog
     */
    private IfnmConnectWindow appendText(String input) {
        if (textArea.getText() == null || textArea.getText().trim().length() == 0)
            textArea.setText(input);

        else {
            textArea.append("\n" + input);
            textArea.setCaretPosition(textArea.getText().length());
        }
        return this;
    }

    /**
     * Sets how far we are on the dialog's progress bar
     * @param n   steps of <code>max</code>
     * @param max max steps
     * @return the dialog
     */
    private IfnmConnectWindow setProgress(int n, int max) {
        progressBar.setMaximum(max);
        progressBar.setValue(n);
        return this;
    }

}
