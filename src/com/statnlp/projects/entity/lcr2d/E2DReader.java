package com.statnlp.projects.entity.lcr2d;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.dep.utils.DPConfig;

public class E2DReader {

	public static List<E2DInstance> readData(String path, boolean setLabel, int number,HashMap<String, Integer> entityMap) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<E2DInstance> insts = new ArrayList<E2DInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				E2DInstance inst = new E2DInstance(index++,1.0,sent);
				inst.entities = es;
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split("\\t");
			//String entity = values[3].equals("O")? values[3]: values[3].substring(2, values[3].length());
			String entity = values[3];
//			if(!entityMap.containsKey(entity)) {
//				if(!entity.equals("O"))
//					entity = entity.substring(0, 2)+"MISC";
//				else entity = "O";
//			}
			words.add(new WordToken(values[1],values[2],Integer.valueOf(values[4])-1,entity));
			es.add(entity);
		}
		br.close();
		List<E2DInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}

	
	public static List<E2DInstance> readDP2NERPipe(String path, int number,HashMap<String, Integer> entityMap) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<E2DInstance> insts = new ArrayList<E2DInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				E2DInstance inst = new E2DInstance(index++,1.0,sent);
				inst.entities = es;
				inst.setUnlabeled();
				insts.add(inst);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split(" ");
			//String entity = values[3].equals("O")? values[3]: values[3].substring(2, values[3].length());
			String entity = values[3];
			if(!entityMap.containsKey(entity)) entity = "O";
			words.add(new WordToken(values[1],values[2],Integer.valueOf(values[5])-1,entity));
			es.add(entity);
		}
		br.close();
		List<E2DInstance> myInsts = insts;
		System.err.println("[Pipeline] Testing instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}
	
	public static List<E2DInstance> readCNN(String path, boolean setLabel, int number,HashMap<String, Integer> entityMap) throws IOException{
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<E2DInstance> insts = new ArrayList<E2DInstance>();
		int index =1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> es = new ArrayList<String>();
		while((line = br.readLine())!=null){
			if(line.startsWith("#")) continue;
			if(line.equals("")){
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				E2DInstance inst = new E2DInstance(index++,1.0,sent);
				inst.entities = es;
				if(setLabel) inst.setLabeled(); else inst.setUnlabeled();
				insts.add(inst);
				words = new ArrayList<WordToken>();
				es = new ArrayList<String>();
				if(number!=-1 && insts.size()==number) break;
				continue;
			}
			String[] values = line.split(" ");
			String entity = values[3];
			if(!entityMap.containsKey(entity)) entity = "O";
			words.add(new WordToken(values[1],values[2],Integer.valueOf(values[4])-1,entity));
			es.add(entity);
		}
		br.close();
		List<E2DInstance> myInsts = insts;
		String type = setLabel? "Training":"Testing";
		System.err.println(type+" instance, total:"+ myInsts.size()+" Instance. ");
		return myInsts;
	}

	
}