/* Copyright (c) 2017 Kevin Wong. All Rights Reserved. */
package com.git.ifly6.nationManager;

import java.util.List;

public class IfnmData {
	
	private List<IfnmNation> nations;
	
	public IfnmData(List<IfnmNation> nations) {
		this.setNations(nations);
	}
	
	public List<IfnmNation> getNations() {
		return nations;
	}
	
	public void setNations(List<IfnmNation> nations) {
		this.nations = nations;
	}
	
}
