package com.git.ifly6.nationManager.gui.components;

import com.git.ifly6.nationManager.gui.IfnmNation;
import com.git.ifly6.nsapi.ApiUtils;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.ListModel;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class IfnmList extends JList<IfnmNation> {

    private IfnmList(ListModel<IfnmNation> model) {
        super(model);
    }

    public DefaultListModel<IfnmNation> getListModel() {
        return (DefaultListModel<IfnmNation>) this.getModel();
    }

    /**
     * @return list of all elements in list model
     */
    public List<IfnmNation> getAll() {
        List<IfnmNation> l = new ArrayList<>(this.getModel().getSize());
        for (int i = 0; i < this.getModel().getSize(); i++)
            l.add(this.getModel().getElementAt(i));

        return l;
    }

    /**
     * Determines if a nation of provided name is already present in the list. Matches after applying {@link
     * ApiUtils#ref(String)}.
     * @param name to look for
     * @return true if present
     */
    public boolean contains(String name) {
        if (name == null) return false;
        if (ApiUtils.isEmpty(name)) return false;

        final String needle = ApiUtils.ref(name);
        for (int i = 0; i < this.getModel().getSize(); i++)
            if (ApiUtils.ref(this.getModel().getElementAt(i).getName()).equals(needle))
                return true;

        return false;
    }

    /**
     * Convenience method to get selected values
     * @return selected values
     */
    public List<IfnmNation> getSelected() {
        return this.getSelectedValuesList();
    }

    /**
     * Factory method to initialise properly
     * @param data to initialise with
     * @return properly initialised {@link JList}
     */
    public static IfnmList createList(List<IfnmNation> data) {
        DefaultListModel<IfnmNation> listModel = new DefaultListModel<>();
        data.forEach(listModel::addElement);

        IfnmList list = new IfnmList(listModel);
        list.setSelectionModel(new SelectionModel());
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        return list;
    }

    private static class SelectionModel extends DefaultListSelectionModel {
        private static final long serialVersionUID = 1L;
        boolean gestureStarted = false;

        @Override
        public void setSelectionInterval(int index0, int index1) {
            if (!gestureStarted)
                if (isSelectedIndex(index0)) super.removeSelectionInterval(index0, index1);
                else super.addSelectionInterval(index0, index1);
            gestureStarted = true;
        }

        @Override
        public void setValueIsAdjusting(boolean isAdjusting) {
            if (!isAdjusting)
                gestureStarted = false;
        }
    }
}
