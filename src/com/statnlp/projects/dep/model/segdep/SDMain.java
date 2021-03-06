package com.statnlp.projects.dep.model.segdep;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.projects.dep.utils.DPConfig;
import com.statnlp.projects.dep.utils.DPConfig.MODEL;

/**
 * This one is modified for the whole named entity version
 * @author allan_jie
 * @version 2.0
 */
public class SDMain {
	
	public static int trainNumber = -1;
	public static int testNumber = -1;
	public static int numIteration = 100;
	public static int numThreads = 2;
	public static String trainingPath;
	public static String testingPath;
	public static String devPath;
	public static boolean isDev = false;
	public static boolean lenOne = false;
	public static HashSet<String> dataTypeSet;
	protected static boolean saveModel = false;
	protected static boolean readModel = false;
	protected static String modelFile;
	
	public static void main(String[] args) throws InterruptedException, IOException, ClassNotFoundException {
		
		
		processArgs(args);
		String modelType = MODEL.SegDep.name();
		DPConfig.currentModel = modelType;
		String middle = isDev? ".dev":".test";
		String dpRes = DPConfig.data_prefix + modelType+middle+DPConfig.dp_res_suffix; 
		String nerEval = DPConfig.data_prefix + modelType+middle+DPConfig.ner_eval_suffix;
		String jointRes = DPConfig.data_prefix + modelType+middle+DPConfig.joint_res_suffix;
		modelFile = DPConfig.data_prefix + modelType + middle + ".model";
		trainingPath = DPConfig.trainingPath;
		testingPath = DPConfig.testingPath;
		devPath = DPConfig.devPath;
		
		System.err.println("[Info] Current Model:"+modelType);
		/******Debug********/
//		trainingPath = "data/allanprocess/voa/train.conllx";
//		testingPath = "data/allanprocess/voa/test.conllx";
//		trainNumber = -1;
//		testNumber = -1;
//		numIteration = 500;
//		numThreads = 8;
//		testingPath = trainingPath;
//		lenOne = false;
		/************/
		
		
		String decodePath = isDev?devPath:testingPath;
		System.err.println("[Info] train path: "+trainingPath);
		System.err.println("[Info] testFile: "+decodePath);
		System.err.println("[Info] dpRes: "+dpRes);
		System.err.println("[Info] ner eval: "+nerEval);
		System.err.println("[Info] modelFile: "+modelFile);
		System.err.println("[Info] joint Res: "+jointRes);
		SDInstance[] trainingInsts = SDReader.readCoNLLXData(trainingPath, true, trainNumber, true, lenOne);
		SDInstance[] testingInsts = SDReader.readCoNLLXData(decodePath, false, testNumber, false, lenOne);
		SpanLabel.get(DPConfig.EMPTY);
		System.err.println("The label set: " + SpanLabel.Label_Index.toString());
		
		//debug
		SpanLabel.lock();
		
		//debug:
//		System.err.println("checking training");
//		Analyzer.checkMultiwordsHead(trainingInsts);
//		System.err.println("checking testing");
//		Analyzer.checkMultiwordsHead(testingInsts);
//		System.exit(0);
		
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = DPConfig.L2; //DPConfig.L2;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		
		NetworkModel model = null;
		if (readModel) {
			System.err.println("[Info] Reading the network model.");
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(modelFile));
			NetworkIDMapper.setCapacity(new int[]{500, 500, 5, 5, 10});
			model =(NetworkModel)in.readObject();
			in.close();
			System.err.println("[Info] Model is read.");
		} else {
			SDFeatureManager hpfm = new SDFeatureManager(new GlobalNetworkParam());
			SDNetworkCompiler dnc = new SDNetworkCompiler();
			model = DiscriminativeNetworkModel.create(hpfm, dnc);
			model.train(trainingInsts, numIteration); 
		}
		
		
		if (saveModel) {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(modelFile));
			out.writeObject(model);
			out.close();
		}
		
		/****************Evaluation Part**************/
		System.err.println("*****Evaluation*****");
		Instance[] predInsts = model.decode(testingInsts);
		SDEval.evalDep(predInsts, false);
	}
	
	public static void processArgs(String[] args){
		String usage = "\t usage: java -jar hyperedge.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -train path -test path";
		if(args.length > 0) {
			if(args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") ){
				System.err.println("UModel Version: Joint DEPENDENCY PARSING and Entity Recognition TASK: ");
				System.err.println(usage);
				System.err.println("\t put numTrainInsts/numTestInsts = -1 if you want to use all the training/testing instances");
				System.err.println("\t By default: trainNum=-1, testNum=-1, thread=2, iter=100");
				System.exit(0);
			}else{
				for(int i=0;i<args.length;i=i+2){
					switch(args[i]){
						case "-trainNum": trainNumber = Integer.valueOf(args[i+1]); break;
						case "-testNum": testNumber = Integer.valueOf(args[i+1]); break;
						case "-iter": numIteration = Integer.valueOf(args[i+1]); break;
						case "-thread": numThreads = Integer.valueOf(args[i+1]); break;
						case "-train":trainingPath = args[i+1];break;
						case "-test":testingPath = args[i+1];break;
						case "-debug": DPConfig.DEBUG = args[i+1].equals("true")? true:false; break;
						case "-reg": DPConfig.L2 = Double.parseDouble(args[i+1]); break;
						case "-dev": isDev = args[i+1].equals("true")? true:false; break;
						case "-windows": DPConfig.windows = true; break;
						case "-data":DPConfig.dataType=args[i+1];DPConfig.changeDataType(); break;
						case "-rw": DPConfig.weightPath=args[i+1]; DPConfig.readWeight = true;DPConfig.writeWeight = false; break;
						case "-ww":DPConfig.weightPath=args[i+1]; DPConfig.readWeight = false; DPConfig.writeWeight = true; break;
						case "-lenone": lenOne = args[i+1].equals("true")? true:false; break;
						case "-saveModel": saveModel = args[i+1].equals("true")? true:false; break;
						case "-readModel": readModel = args[i+1].equals("true")? true:false; break;
						default: System.err.println("Invalid arguments: "+args[i]+", please check usage."); System.err.println(usage);System.exit(0);
					}
				}
				System.err.println("[Info] trainNum: "+trainNumber);
				System.err.println("[Info] testNum: "+testNumber);
				System.err.println("[Info] numIter: "+numIteration);
				System.err.println("[Info] numThreads: "+numThreads);
				
				System.err.println("[Info] Regularization Parameter: "+DPConfig.L2);
				System.err.println("[Info] Using development set??: "+isDev);
				
			}
		}
	}

}
