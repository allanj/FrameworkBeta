package com.statnlp.projects.dep.model.hyperedge;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.dep.utils.DataChecker;

public class HPEReader {

	public static String ROOT_WORD = "ROOT";
	public static String ROOT_TAG = "ROOT";

	public static HPEInstance[] readCoNLLXData(String fileName, boolean isLabeled, int number, boolean checkProjective) throws IOException{
		BufferedReader br = RAWF.reader(fileName);
		ArrayList<HPEInstance> result = new ArrayList<HPEInstance>();
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		List<Integer> originalHeads = new ArrayList<>();
		Map<Integer, Integer> idx2SpanIdx = new HashMap<>();
		int maxSentenceLength = -1;
		int maxEntityLength = -1;
		words.add(new WordToken(ROOT_WORD, ROOT_TAG));
		originalHeads.add(-1);
		ArrayList<Span> output = new ArrayList<Span>();
		output.add(new Span(0, 0, Label.get("O")));
		idx2SpanIdx.put(0, output.size()-1);
		int instanceId = 1;
		int start = -1;
		int end = 0;
		Label prevLabel = null;
		while(br.ready()){
			String line = br.readLine().trim();
			if(line.length() == 0){
				end = words.size()-1;
				if(start != -1){
					createSpan(output, start, end, prevLabel, idx2SpanIdx);
					maxEntityLength = Math.max(maxEntityLength, output.get(output.size()-1).length());
				}
				start = -1;
				WordToken[] wtArr = new WordToken[words.size()];
				HPEInstance instance = new HPEInstance(instanceId, 1.0, new Sentence(words.toArray(wtArr)));
				instance.input.setRecognized();
				instance.output = output;
				//assign span with head span
				for (Span span: instance.output) {
					if (span.length() == 1) {
						if (originalHeads.get(span.start) != -1){
							span.headSpan = output.get(idx2SpanIdx.get(originalHeads.get(span.start)));
						}
					} else {
						int entityHead = -1; 
						//by default select the from the last
						for(int i = span.end; i >= span.start; i--){
							if (originalHeads.get(i) < span.start || originalHeads.get(i) > span.end) {
								entityHead = originalHeads.get(i);
								break;
							}
						}
						if (entityHead == -1)
							throw new RuntimeException("The head of entity is -1 ?");
						span.headSpan = output.get(idx2SpanIdx.get(entityHead));
					}
				}
				
				boolean projectiveness = DataChecker.checkProjective(originalHeads);
				if(checkProjective && !projectiveness) {
					words = new ArrayList<WordToken>();
					words.add(new WordToken(ROOT_WORD, ROOT_TAG));
					originalHeads = new ArrayList<>();
					originalHeads.add(-1);
					output = new ArrayList<Span>();
					output.add(new Span(0, 0, Label.get("O")));
					idx2SpanIdx = new HashMap<>();
					idx2SpanIdx.put(0, output.size()-1);
					continue;
				}
				if(isLabeled){
					instance.setLabeled(); // Important!
				} else {
					instance.setUnlabeled();
				}
				instanceId++;
				result.add(instance);
				maxSentenceLength = Math.max(maxSentenceLength, instance.size());
				words = new ArrayList<WordToken>();
				words.add(new WordToken(ROOT_WORD, ROOT_TAG));
				output = new ArrayList<Span>();
				output.add(new Span(0, 0, Label.get("O")));
				originalHeads = new ArrayList<>();
				originalHeads.add(-1);
				idx2SpanIdx = new HashMap<>();
				idx2SpanIdx.put(0, output.size()-1);
				prevLabel = null;
				start = -1;
				end = 0;
				if(result.size()==number)
					break;
			} else {
				String[] values = line.split("[\t ]");
				int index = Integer.valueOf(values[0]); //because it is starting from 1
				String word = values[1];
				String pos = values[4];
				int headIdx = Integer.parseInt(values[6]);
				String depLabel = values[7];
				String form = values[10];
				if(depLabel.contains("|")) throw new RuntimeException("Mutiple label?");
				words.add(new WordToken(word, pos));
				originalHeads.add(headIdx);
				Label label = null;
				if(form.startsWith("B")){
					if(start != -1){
						end = index - 1;
						createSpan(output, start, end, prevLabel, idx2SpanIdx);
						maxEntityLength = Math.max(maxEntityLength, output.get(output.size()-1).length());
					}
					start = index;
					label = Label.get(form.substring(2));
					
				} else if(form.startsWith("I")){
					label = Label.get(form.substring(2));
				} else if(form.startsWith("O")){
					if(start != -1){
						end = index - 1;
						createSpan(output, start, end, prevLabel, idx2SpanIdx);
						maxEntityLength = Math.max(maxEntityLength, output.get(output.size()-1).length());
					}
					start = -1;
					createSpan(output, index, index, Label.get("O"), idx2SpanIdx);
					maxEntityLength = Math.max(maxEntityLength, output.get(output.size()-1).length());
					label = Label.get("O");
				}
				prevLabel = label;
			}
		}
		br.close();
		String type = isLabeled? "train":"test";
		System.err.println("[Info] number of "+type+" instances:"+result.size());
		System.err.println("[Info] max sentence length: " + maxSentenceLength);
		System.err.println("[Info] max entity length: " + maxEntityLength);
		return result.toArray(new HPEInstance[result.size()]);
	}
	
 	private static void createSpan(List<Span> output, int start, int end, Label label, Map<Integer, Integer> idx2SpanIdx){
		if (label == null) {
			throw new RuntimeException("The label is null");
		}
		if (start > end) {
			throw new RuntimeException("start cannot be larger than end");
		}
		if (label.form.equals("O")) {
			for (int i = start; i <= end; i++) {
				output.add(new Span(i, i, label));
				idx2SpanIdx.put(i, output.size()-1);
			}
		} else {
			output.add(new Span(start, end, label));
			for (int i = start; i <= end; i++) {
				idx2SpanIdx.put(i, output.size()-1);
			}
		}
	}
}
