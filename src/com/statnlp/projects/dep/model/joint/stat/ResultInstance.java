package com.statnlp.projects.dep.model.joint.stat;

public class ResultInstance {

	//1-indexed
	public String[] entities;
	public int[] headIdxs;
	
	public ResultInstance(String[] entities, int[] headIdxs) {
		this.entities = entities;
		this.headIdxs = headIdxs;
	}
}
