package com.statnlp.projects.entity.lcr2d;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.Init;

public class E2DMain {

	public static String[] entities; 
	public static int trainNumber = -100;
	public static int testNumber = -100;
	public static int numIteration = -100;
	public static int numThreads = -100;
	public static String testFile = "";
	public static boolean isPipe = false;
	public static String nerOut;
	public static String nerRes;
	public static boolean isDev;
	public static String[] selectedEntities = {"person","organization","gpe","MISC"};
	public static HashSet<String> dataTypeSet;
	public static HashMap<String, Integer> entityMap;
	
	public static void initializeEntityMap(){
		entityMap = new HashMap<String, Integer>();
		int index = 0;
		entityMap.put("O", index++);  
		for(int i=0;i<selectedEntities.length;i++){
			entityMap.put(selectedEntities[i], index++);
//			entityMap.put("B-"+selectedEntities[i], index++);
//			entityMap.put("I-"+selectedEntities[i], index++);
		}
		entities = new String[entityMap.size()];
		Iterator<String> iter = entityMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			entities[entityMap.get(entity)] = entity;
		}
	}

	
	
	public static void main(String[] args) throws IOException, InterruptedException{
		// TODO Auto-generated method stub
		
		trainNumber = 80;
		testNumber = 2;
		numThreads = 5;
		numIteration = 200;
		isPipe = false;
		processArgs(args);
		dataTypeSet = Init.iniOntoNotesData();
		initializeEntityMap();
		String modelType = DPConfig.MODEL.ecrf.name();
		
		
		String middle = isDev? ".dev":".test";
		nerOut = DPConfig.data_prefix+modelType+middle+DPConfig.ner_eval_suffix;
		nerRes = DPConfig.data_prefix+modelType+middle+DPConfig.ner_res_suffix;
		testFile = isDev? DPConfig.devPath:DPConfig.testingPath;
		if(isPipe){
			testFile = isDev?DPConfig.dp2ner_dp_dev_input:DPConfig.dp2ner_dp_test_input;
			nerOut = DPConfig.data_prefix+middle+".pp.dp2ner.ner.eval.txt";
			nerRes = DPConfig.data_prefix+middle+".pp.dp2ner.ner.res.txt";
		}
		System.err.println("[Info] trainingFile: "+DPConfig.trainingPath);
		System.err.println("[Info] testFile: "+testFile);
		System.err.println("[Info] nerOut: "+nerOut);
		System.err.println("[Info] nerRes: "+nerRes);
		
		List<E2DInstance> trainInstances = null;
		List<E2DInstance> testInstances = null;
		/***********DEBUG*****************/
//		DPConfig.ecrftrain = "data/semeval10t1/ecrf.small.txt";
//		testFile="data/semeval10t1/ecrf.small.txt";
		trainNumber = 500;
		testNumber= -1;
		numIteration=300;
//		DPConfig.writeWeight = true;
//		DPConfig.weightPath = "data/semeval10t1/ecrf2dWeight.txt";
//		DPConfig.readWeight = false;
//		testFile = DPConfig.ecrftrain;
		/***************************/
		if(dataTypeSet.contains(DPConfig.dataType)){
			trainInstances = E2DReader.readCNN(DPConfig.trainingPath, true, trainNumber, entityMap);  //Error: this one should be changed to use a conll reader
			testInstances = E2DReader.readCNN(testFile, false, testNumber, entityMap);
		}else{
			trainInstances = E2DReader.readData(DPConfig.trainingPath,true,trainNumber, entityMap);
			testInstances = isPipe?E2DReader.readDP2NERPipe(testFile, testNumber,entityMap)
					:E2DReader.readData(testFile,false,testNumber,entityMap);
		}
		
//		Formatter.ner2Text(trainInstances, "data/testRandom2.txt");
//		System.exit(0);
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = DPConfig.L2;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = false;
		NetworkConfig.MAX_MARGINAL_DECODING = false;
		
		EntityViewer eViewer = new EntityViewer(entities);
		E2DFeatureManager fa = new E2DFeatureManager(new GlobalNetworkParam(),entities,isPipe);
		E2DNetworkCompiler compiler = new E2DNetworkCompiler(entityMap, entities,eViewer);
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		E2DInstance[] ecrfs = trainInstances.toArray(new E2DInstance[trainInstances.size()]);
		model.train(ecrfs, numIteration);
		Instance[] predictions = model.decode(testInstances.toArray(new E2DInstance[testInstances.size()]));
		E2DEval.evalNER(predictions, nerOut);
		E2DEval.writeNERResult(predictions, nerRes, true);
	}

	
	
	public static void processArgs(String[] args){
		if(args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") ){
			System.err.println("Linear-Chain CRF Version: Joint DEPENDENCY PARSING and Entity Recognition TASK: ");
			System.err.println("\t usage: java -jar dpe.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -pipe true");
			System.err.println("\t put numTrainInsts/numTestInsts = -1 if you want to use all the training/testing instances");
			System.exit(0);
		}else{
			for(int i=0;i<args.length;i=i+2){
				switch(args[i]){
					case "-trainNum": trainNumber = Integer.valueOf(args[i+1]); break;
					case "-testNum": testNumber = Integer.valueOf(args[i+1]); break;
					case "-iter": numIteration = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;
					case "-pipe": isPipe = args[i+1].equals("true")?true:false; break;
					case "-ent": selectedEntities = args[i+1].split(","); break;
					case "-testFile": testFile = args[i+1]; break;
					case "-reg": DPConfig.L2 = Double.valueOf(args[i+1]); break;
					case "-dev":isDev = args[i+1].equals("true")?true:false; break;
					case "-windows":DPConfig.windows = true; break;
					case "-comb": DPConfig.comb = true; break;
					case "-data":DPConfig.dataType=args[i+1];DPConfig.changeDataType(); break;
					default: System.err.println("Invalid arguments, please check usage."); System.exit(0);
				}
			}
			if(DPConfig.comb){
				DPConfig.changeTrainingPath();
			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] is Pipeline: "+isPipe);
			System.err.println("[Info] Using development set??: "+isDev);
			System.err.println("[Info] Selected Entities: "+Arrays.toString(selectedEntities));
			System.err.println("[Info] Data type: "+DPConfig.dataType);
			System.err.println("[Info] Regularization Parameter: "+DPConfig.L2);
			if(isPipe){
				System.err.println("[Info] *********PipeLine: from DP result to NER****");
			}
			String currentModel = isPipe? "Pipeline-DP2NER":"NER";
			System.err.println("[Info] CurrentModel:"+currentModel);
		}
	}
}