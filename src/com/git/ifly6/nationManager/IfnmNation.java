/* Copyright (c) 2017 Kevin Wong. All Rights Reserved. */
package com.git.ifly6.nationManager;

import com.git.ifly6.iflyLibrary.generics.IflyPair;

/** @author ifly6 */
public class IfnmNation extends IflyPair<String, String> {
	
	private static final long serialVersionUID = IflyNationManager.VERSION.major;
	private boolean exists = true;
	
	public IfnmNation(String left, String right) {
		super(left.trim().toLowerCase().replace(" ", "_"), right);
	}
	
	public String getName() {
		return super.getLeft();
	}
	
	public String getPassword() {
		return super.getRight();
	}
	
	@Override public String toString() {
		return this.getLeft() + (exists ? "" : "*");
	}
	
	public boolean exists() {
		return exists;
	}
	
	public void setExists(boolean exists) {
		this.exists = exists;
	}
	
}
