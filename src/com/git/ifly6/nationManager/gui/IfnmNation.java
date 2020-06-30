package com.git.ifly6.nationManager.gui;

import com.git.ifly6.iflyLibrary.generics.IflyPair;
import com.git.ifly6.nsapi.ApiUtils;

/**
 * The <code>IfnmNation</code> class holds a nation reference name (automatically formatted) and the <b>hash</b> of the
 * nation's password. It also holds a variable about whether it exists or not.
 * @author ifly6
 */
public class IfnmNation extends IflyPair<String, String> {

    private static final long serialVersionUID = IflyNationManager.VERSION.major;
    private boolean exists = true;

    public IfnmNation(String n, String hash) {
        super(ApiUtils.ref(n), hash);
    }

    public boolean exists() {
        return exists;
    }

    public IfnmNation setExists(boolean exists) {
        this.exists = exists;
        return this;
    }

    public String getName() {
        return super.getLeft();
    }

    public String getPassword() {
        return super.getRight();
    }

    /**
     * Returns string representation of the nation, which is its name
     * @return the nation name, with asterisk appended if nation does not exist
     */
    @Override
    public String toString() {    // override to allow for correct display
        return (exists ? "" : "* ") + this.getName();
    }

}
