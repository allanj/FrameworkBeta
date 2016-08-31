package data.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.statnlp.commons.crf.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;


public class OntoNotesPreprocess {

	
	public static String[] datasets = {"pri","voa"};
	public static String[] valid = {"PERSON", "ORG", "GPE"};
	public static String[] validConvert = {"person", "organization", "gpe"};
	public static String[] others = {"NORP","FAC","LOC","PRODUCT","DATE","TIME","PERCENT","MONEY","QUANTITY","ORDINAL","CARDINAL","EVENT","WORK_OF_ART","LAW","LANGUAGE"};
	public static HashSet<String> dataNames;
	public static HashSet<String> validSet;
	public static HashMap<String, String> validMap;
	public static HashSet<String> otherSet;
	public static String filePrefix = "F:/phd/data/ontonotes-release-5.0_LDC2013T19/ontonotes-release-5.0_LDC2013T19/ontonotes-release-5.0/data/files/data/english/annotations/bn";
//	public static String filePrefix = "D:/Downloads/test";
	public static String tmpTreebank = "D:/Downloads/tmp/temp";
	public static String tmpOutput = "D:/Downloads/tmp/temp.output";
	public static String tmpError = "D:/Downloads/tmp/temp.error";
	public static String converterPath = "F:/Dropbox/SUTD/tools/pennconverter.jar";
	public static String converterLog = "F:/Dropbox/SUTD/tools/log.log";
	public static String outputPrefx = "E:/Framework/data/allanprocess/";
	
	//linux setup
//	public static String filePrefix = "/home/allan/data/ontonotes_subset";
//	public static String tmpTreebank = "/home/allan/temp/temp.txt";
//	public static String converterPath = "/home/allan/tools/pennconverter.jar";
//	public static String converterLog = "/home/allan/tools/log.log";
//	public static String outputPrefx = "/home/allan/jointdpe/data/allanprocess/";
	
	/**
	 * The sentence returned will have a root on the leftmost
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static Sentence convert() throws IOException, InterruptedException{
		ProcessBuilder pb = new ProcessBuilder("java","-jar", converterPath,"-rightBranching=false","-log", converterLog,"-stopOnError=false"); 
		pb.redirectInput(new File(tmpTreebank));
		pb.redirectOutput(ProcessBuilder.Redirect.to(new File(tmpOutput)));
		pb.redirectError(ProcessBuilder.Redirect.to(new File(tmpError)));
		Process p = pb.start();
		p.waitFor();
		BufferedReader reader =  RAWF.reader(tmpError); //new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String line = null;
		while ( (line = reader.readLine()) != null) {
			if(line.startsWith("Number of errors")){
				String trimmed = line.trim();
				String[] vals = trimmed.split("\\s+"); 
				
				if(!vals[vals.length-1].equals("0"))
					return null;
			}
		}
		reader.close();
		reader =  RAWF.reader(tmpOutput); //new BufferedReader(new InputStreamReader(p.getInputStream()));
		line = null;
		ArrayList<WordToken> wts = new ArrayList<WordToken>();
		wts.add(new WordToken("ROOT", "ROOT", -1, "O", "NOLABEL"));
		while ( (line = reader.readLine()) != null) {
			if(line.equals("")) continue;
			String trimmed = line.trim();
			String[] vals = trimmed.split("\\t");
			wts.add(new WordToken(vals[1], vals[3], Integer.valueOf(vals[6]), "O", vals[7]));
		}
		reader.close();
		WordToken[] wta  = new WordToken[wts.size()];
		wts.toArray(wta);
		Sentence sent = new Sentence(wta);
		return sent;
	}
	
	private static void processTheFiles(String file, ArrayList<Sentence> sents) throws IOException, InterruptedException{
		BufferedReader reader = RAWF.reader(file);
		String line = null;
		String flag = "ready";
		PrintWriter pw = null;
		int fake = 0;
		int currSentIdx = 0;
		while((line=reader.readLine())!=null){
//			System.out.println(line);
			if(flag.equals("ready") && line.startsWith("Tree:")) { flag = "tree"; continue; }
			if(flag.equals("tree") && line.startsWith("--")) {flag = "read_tree";   pw = RAWF.writer(tmpTreebank);   continue;}
			if(flag.equals("read_tree")){
				pw.write(line+"\n");
//				System.out.println("reading:"+line);
				if(line.equals("")) {
					flag = "finish_tree"; 
					pw.close();
//					Thread.sleep(100);
					if(flag.equals("finish_tree")){
						Sentence sent = convert();
						if(sent!=null) { sents.add(sent); flag = "waiting_entity";}
						else flag = "ready";
					}
				}
				continue;
			}
			
			if(flag.equals("waiting_entity") && line.startsWith("Leaves:")){flag = "entity"; continue;}
			if(flag.equals("entity") && line.startsWith("--")) {flag = "read_entity"; fake=0; currSentIdx=1; continue; }
			if(flag.equals("read_entity")){
				String trimmed = line.trim();
				
				
				Sentence sent = sents.get(sents.size()-1);
				if(line.equals("")) {
					flag = "ready";
					if((currSentIdx)!=sent.length()){
						System.out.println(file);
						System.out.println("wrong:"+sent.toString());
						throw new RuntimeException("Not with the same length:"+(currSentIdx)+ " (fake: "+fake+") and "+sent.length());
					}
					//System.out.println(sent.toString());
					continue;
				}
				
				String[] ccs = trimmed.split("[\\s\\t]+");
				if(ccs.length==2 && ccs[0].matches("\\d+")){
					
					//if(ccs[0].matches("\\d+") && (ccs[1].startsWith("*") || ccs[1].equals("0"))) {  fake++;}
					String word = ccs[1];
					if(!word.equals(sent.get(currSentIdx).getName())){
						fake++;
					}else currSentIdx++;
					//currSentIdx--;
				}
				
				if(trimmed.startsWith("name:")){
					//Sentence sent = sents.get(sents.size()-1);
//					System.out.println( file);
//					System.out.println( sent.length()+" "+sent.toString());
					String[] vals = trimmed.split("[\\s\\t]+");
					if(validSet.contains(vals[1])){
						String[] range = vals[2].split("-");
						int rangeLeft = Integer.valueOf(range[0])+1-fake;
						int rangeRight = Integer.valueOf(range[1])+1-fake;
						for(int i=rangeLeft;i<=rangeRight;i++){
							String entity = validMap.get(vals[1]);
							entity = i==rangeLeft? "B-"+entity: "I-"+entity;
							sent.get(i).setEntity(entity);
						}
					}else if(otherSet.contains(vals[1])){
						String[] range = vals[2].split("-");
						int rangeLeft = Integer.valueOf(range[0])+1-fake;
						int rangeRight = Integer.valueOf(range[1])+1-fake;
						for(int i=rangeLeft;i<=rangeRight;i++){
							String entity = i==rangeLeft? "B-MISC": "I-MISC";
							sent.get(i).setEntity(entity);
						}
					}else{
						throw new RuntimeException("cannot find this entity:"+trimmed);
					}
				}
			}
		}
//		for(Sentence sent: sents){
//			System.out.println(sent.toString());
//		}
		reader.close();
	}
	
	public static void process() throws IOException, InterruptedException{
		dataNames = new HashSet<String>();
		validSet = new HashSet<String>();
		validMap = new HashMap<String, String>();
		otherSet = new HashSet<String>();
		for(String file: datasets) dataNames.add(file);
		for(int i=0;i<valid.length;i++) { validSet.add(valid[i]);  validMap.put(valid[i], validConvert[i]);}
		for(String type: others) otherSet.add(type);
		
		File file = new File(filePrefix);
		String[] names = file.list();
		for(String data: names){
			if(dataNames.contains(data)){
				File subFile = new File(filePrefix+"/"+data); //the folder that abc/cnn/mnb
				String[] subNames = subFile.list();
				ArrayList<Sentence> sents = new ArrayList<Sentence>();
				for(String numberFolder: subNames){
					File theNumber = new File(filePrefix+"/"+data+"/"+numberFolder); //abc/00/
					if(theNumber.isDirectory()){
						String[] textFileList = theNumber.list();  //abc/00/0001.parse.. something like this.
						for(String textFile: textFileList){
							if(textFile.endsWith(".onf"))
								processTheFiles(filePrefix+"/"+data+"/"+numberFolder+"/"+textFile, sents);
						}
					}
				}
				System.out.print("[Info] Finishing dataset:"+data);
				//print these sentences.
				printConll(data,sents);
			}
		}
	}
	
	/**
	 * 
	 * @param datasetName: abc/cnn or something else
	 * @throws IOException
	 */
	private static void printConll(String datasetName, ArrayList<Sentence> sents) throws IOException{
		PrintWriter pw = RAWF.writer(outputPrefx+"/"+datasetName+"/all.conllx");
		System.out.println("   dataset:"+datasetName+":"+sents.size());
		for(Sentence sent: sents){
			for(int i=1; i<sent.length(); i++)
				pw.write(i+"\t"+sent.get(i).getName()+"\t_\t"+sent.get(i).getTag()+"\t"+sent.get(i).getTag()+"\t_\t"+sent.get(i).getHeadIndex()+"\t"+sent.get(i).getDepLabel()+"\t_\t_\t"+sent.get(i).getEntity()+"\n");
			pw.write("\n");
		}
	
		pw.close();
	}
	
	public static void main(String[] args) throws IOException, InterruptedException{
//		System.out.println(convert());
		process();
	}

}
