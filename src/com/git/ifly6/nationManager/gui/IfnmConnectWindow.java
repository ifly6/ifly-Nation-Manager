package com.git.ifly6.nationManager.gui;

import com.git.ifly6.iflyLibrary.IflySystem;
import com.git.ifly6.nationManager.IfnmCoder;
import com.git.ifly6.nsapi.NSConnection;
import com.git.ifly6.nsapi.NSException;
import com.git.ifly6.nsapi.telegram.JTelegramException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
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
    private JButton btnClose;

    /**
     * Create the dialog.
     */
    IfnmConnectWindow() {

        setTitle("Connecting...");

        Dimension size = new Dimension(500, 300);
        setSize(size);
        setMinimumSize(size);

        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setLayout(new BorderLayout(5, 5));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(content, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        if (IflySystem.IS_OS_MAC)
            progressBar.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        textArea = new JTextArea();
        textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        textArea.setEditable(false);

        btnClose = new JButton("Close");
        btnClose.addActionListener((ae) -> {
            setVisible(false);
            dispose();
        });

        {
            content.add(new JScrollPane(textArea), BorderLayout.CENTER);
            content.add(new JLabel("Detailed connection progress:"), BorderLayout.NORTH);

            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BorderLayout(5, 5));
            bottomPanel.add(progressBar, BorderLayout.CENTER);
            bottomPanel.add(btnClose, BorderLayout.EAST);

            content.add(bottomPanel, BorderLayout.SOUTH);
        }

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

            btnClose.setEnabled(false);
            for (int i = 0; i < nations.size(); i++) {

                IfnmNation nation = nations.get(i);
                String name = nation.getName();
                String encryptedPass = nation.getPassword();
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
            btnClose.setEnabled(true);

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
