package com.statnlp.projects.dep.model.hyperedge.stat;

import com.statnlp.projects.dep.model.hyperedge.HPEInstance;
import com.statnlp.projects.dep.model.hyperedge.Span;

public class Analyzer {

	
	
	//check if head is the multitokens span.
	public static void checkMultiwordsHead(HPEInstance[] instances) {
		for (HPEInstance inst: instances) {
			//System.out.println("Current instance id: " + inst.getInstanceId()+ " " + inst.getInput().toString());
			for (int i = 1; i < inst.getOutput().size(); i++) {
				Span span = inst.getOutput().get(i);
				if (span.length() > 3 && span.headSpan.length() > 3) {
					System.out.println("current span: " + span.toString() + " and head Span: " + span.headSpan.toString());
				}
				
//				if(!span.label.form.equals("O") && !span.headSpan.label.form.equals("O") && span.length() > 2 && span.headSpan.length() > 2) {
//					System.out.println("current span: " + span.toString() + " and head Span: " + span.headSpan.toString());
//				}
			}
		}
	}
		
}
