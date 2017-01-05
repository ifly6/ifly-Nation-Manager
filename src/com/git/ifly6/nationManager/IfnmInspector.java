/* Copyright (c) 2017 Kevin Wong. All Rights Reserved. */
package com.git.ifly6.nationManager;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.git.ifly6.nsapi.NSNation;

/** @author ifly6 */
public class IfnmInspector {
	
	private JFrame frame;
	protected HashMap<String, NSNation> cachedData = new HashMap<>();
	
	/** Create the application. */
	public IfnmInspector(List<IfnmNation> items) {
		
		frame = new JFrame("Inspector");
		frame.setBounds(100, 100, 500, 300);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		DefaultListModel<IfnmNation> tableModel = new DefaultListModel<>();
		items.stream().forEach(tableModel::addElement);
		
		JList<IfnmNation> list = new JList<>(tableModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JScrollPane scrollPane = new JScrollPane(list);
		
		JPanel panel = new JPanel();
		
		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
								.addContainerGap()
								.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 119, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(panel, GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
								.addContainerGap()));
		groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup(Alignment.TRAILING)
						.addGroup(groupLayout.createSequentialGroup()
								.addGap(7)
								.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
										.addComponent(scrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 265,
												Short.MAX_VALUE)
										.addComponent(panel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 265,
												Short.MAX_VALUE))
								.addContainerGap()));
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] { 0, 0, 0 };
		gbl_panel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_panel.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_panel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panel.setLayout(gbl_panel);
		
		JLabel lblProperName = new JLabel("Proper Name");
		GridBagConstraints gbc_lblProperName = new GridBagConstraints();
		gbc_lblProperName.anchor = GridBagConstraints.WEST;
		gbc_lblProperName.insets = new Insets(0, 0, 5, 5);
		gbc_lblProperName.gridx = 0;
		gbc_lblProperName.gridy = 0;
		panel.add(lblProperName, gbc_lblProperName);
		
		JLabel lblName = new JLabel("");
		GridBagConstraints gbc_lblName = new GridBagConstraints();
		gbc_lblName.anchor = GridBagConstraints.WEST;
		gbc_lblName.insets = new Insets(0, 0, 5, 0);
		gbc_lblName.gridx = 1;
		gbc_lblName.gridy = 0;
		panel.add(lblName, gbc_lblName);
		
		JLabel lblRegion = new JLabel("Region");
		GridBagConstraints gbc_lblRegion = new GridBagConstraints();
		gbc_lblRegion.anchor = GridBagConstraints.WEST;
		gbc_lblRegion.insets = new Insets(0, 0, 5, 5);
		gbc_lblRegion.gridx = 0;
		gbc_lblRegion.gridy = 1;
		panel.add(lblRegion, gbc_lblRegion);
		
		JLabel lblNationregion = new JLabel("");
		GridBagConstraints gbc_lblNationregion = new GridBagConstraints();
		gbc_lblNationregion.insets = new Insets(0, 0, 5, 0);
		gbc_lblNationregion.anchor = GridBagConstraints.WEST;
		gbc_lblNationregion.gridx = 1;
		gbc_lblNationregion.gridy = 1;
		panel.add(lblNationregion, gbc_lblNationregion);
		
		JLabel lblCategory = new JLabel("Category");
		GridBagConstraints gbc_lblCategory = new GridBagConstraints();
		gbc_lblCategory.anchor = GridBagConstraints.WEST;
		gbc_lblCategory.insets = new Insets(0, 0, 5, 5);
		gbc_lblCategory.gridx = 0;
		gbc_lblCategory.gridy = 2;
		panel.add(lblCategory, gbc_lblCategory);
		
		JLabel lblNationcategory = new JLabel("");
		GridBagConstraints gbc_lblNationcategory = new GridBagConstraints();
		gbc_lblNationcategory.insets = new Insets(0, 0, 5, 0);
		gbc_lblNationcategory.anchor = GridBagConstraints.WEST;
		gbc_lblNationcategory.gridx = 1;
		gbc_lblNationcategory.gridy = 2;
		panel.add(lblNationcategory, gbc_lblNationcategory);
		
		JLabel lblEndorsements = new JLabel("Endorsements");
		GridBagConstraints gbc_lblEndorsements = new GridBagConstraints();
		gbc_lblEndorsements.insets = new Insets(0, 0, 0, 5);
		gbc_lblEndorsements.gridx = 0;
		gbc_lblEndorsements.gridy = 3;
		panel.add(lblEndorsements, gbc_lblEndorsements);
		
		JLabel lblEndos = new JLabel("");
		GridBagConstraints gbc_lblEndos = new GridBagConstraints();
		gbc_lblEndos.anchor = GridBagConstraints.WEST;
		gbc_lblEndos.gridx = 1;
		gbc_lblEndos.gridy = 3;
		panel.add(lblEndos, gbc_lblEndos);
		frame.getContentPane().setLayout(groupLayout);
		
		list.addListSelectionListener(new ListSelectionListener() {
			@Override public void valueChanged(ListSelectionEvent e) {
				IfnmNation data = list.getSelectedValuesList().get(0);
				NSNation nation;
				if (cachedData.get(data.getName()) == null) {
					nation = new NSNation(data.getName());
					try {
						nation.populateData();
						cachedData.put(data.getName(), nation);
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(frame, "Cannot connect to NationStates.", "Error",
								JOptionPane.PLAIN_MESSAGE, null);
					}
				} else {
					nation = cachedData.get(data.getName());
				}
				lblName.setText(nation.getNationName());
				lblNationregion.setText(nation.getRegion());
				lblNationcategory.setText(nation.getCategory());
				lblEndos.setText(String.valueOf(nation.getEndoCount()));
			}
		});
		
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
