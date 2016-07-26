package com.statnlp.dp.model.hp2d;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.model.nerwknowndp.NKDNetwork;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkException;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.ui.visualize.VisualizationViewerEngine;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class H2DNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -5080640847287255079L;

	private long[] _nodes;
	private final int maxSentLen = 128;
	private int[][][] _children;
	private HashMap<String, Integer> typeMap;
	private String[] types;
	private enum NODE {normal};
	private  VisualizationViewerEngine viewer;
	public static String OE = DPConfig.OE;
	public static String ONE = DPConfig.ONE;
	
	private static boolean DEBUG = false;
	
	/**
	 * Compiler constructor
	 * @param typeMap: typeMap from DPConfig
	 */
	public H2DNetworkCompiler(HashMap<String, Integer> typeMap, VisualizationViewerEngine viewer) {
		// rightIndex, rightIndex-leftIndex, completeness, direction, entity type, node type
		int[] capacity = new  int[]{1000,1000,2,3,15,2};
		NetworkIDMapper.setCapacity(capacity);
		this.typeMap = typeMap;
		this.types = new String[typeMap.size()]; 
		Iterator<String> iter = typeMap.keySet().iterator();
		while(iter.hasNext()){
			String entity = iter.next();
			types[typeMap.get(entity)] = entity;
		}
		this.viewer = viewer;
	}

	@Override
	public Network compile(int networkId, Instance inst, LocalNetworkParam param) {
		H2DInstance di = (H2DInstance)inst;
		if(di.isLabeled()){
			//System.err.println("[Info] Compiling Labeled Network...");
			return this.compileLabledInstance(networkId, di, param);
		}else{
			//System.err.println("[Info] Compiling Unlabeled Network...");
			return this.compileUnLabledInstance(networkId, di, param);
		}
	}

	public H2DNetwork compileLabledInstance(int networkId, H2DInstance inst, LocalNetworkParam param){
		H2DNetwork network = new H2DNetwork(networkId,inst,param);
		Sentence sent = inst.getInput();
		Tree tree = inst.getOutput();
		this.compile(network, sent, tree);
		if(DEBUG){
			H2DNetwork unlabeled = compileUnLabledInstance(networkId, inst, param);
			if(!unlabeled.contains(network)){
				System.err.println(sent.toString());
				throw new NetworkException("Labeled network is not contained in the unlabeled version");
			}
		}
		return network;
	}
	
	private void compile(H2DNetwork network, Sentence sent, Tree output){
		output.setSpans();
		long rootNode = this.toNode_generalRoot(sent.length());
		network.addNode(rootNode);
		CoreLabel cl = (CoreLabel)(output.label());
		String[] info = cl.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		String pa_type = info[4];
		long treeRootNode = toNode(pa_leftIndex, pa_rightIndex, pa_direction, pa_completeness,pa_type);
		network.addNode(treeRootNode);
		network.addEdge(rootNode, new long[]{treeRootNode});
		addToNetwork(network,output);
		network.finalizeNetwork();
		//viewer.visualizeNetwork(network, null, "Labeled Network");
		//System.err.println(network);
		//System.err.println(output.pennString());
		
	}
	
	private void addToNetwork(H2DNetwork network, Tree parent){
		if(parent.isLeaf()) return; //means headindex==modifier index
		CoreLabel cl = (CoreLabel)(parent.label());
		String[] info = cl.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		String pa_type = info[4];
		if(pa_leftIndex==pa_rightIndex) return; //means the span width now is 1, already enough
		long parentNode = toNode(pa_leftIndex, pa_rightIndex, pa_direction, pa_completeness,pa_type);
		Tree[] children = parent.children();
		if(children.length!=2){
			throw new RuntimeException("The children length must be 2 in the labeled hyper edge model tree.");
		}
		CoreLabel childLabel_1 = (CoreLabel)(children[0].label());
		String[] childInfo_1 = childLabel_1.value().split(",");
		int child_leftIndex_1 = Integer.valueOf(childInfo_1[0]);
		int child_rightIndex_1 = Integer.valueOf(childInfo_1[1]);
		int child_direction_1 = Integer.valueOf(childInfo_1[2]);
		int child_completeness_1 = Integer.valueOf(childInfo_1[3]);
		String c1type = childInfo_1[4];
		long childNode_1 = toNode(child_leftIndex_1, child_rightIndex_1, child_direction_1, child_completeness_1,c1type);
		
		CoreLabel childLabel_2 = (CoreLabel)(children[1].label());
		String[] childInfo_2 = childLabel_2.value().split(",");
		int child_leftIndex_2 = Integer.valueOf(childInfo_2[0]);
		int child_rightIndex_2 = Integer.valueOf(childInfo_2[1]);
		int child_direction_2 = Integer.valueOf(childInfo_2[2]);
		int child_completeness_2 = Integer.valueOf(childInfo_2[3]);
		String c2type = childInfo_2[4];
		long childNode_2 = toNode(child_leftIndex_2, child_rightIndex_2, child_direction_2, child_completeness_2,c2type);
		
		network.addNode(childNode_1);
		network.addNode(childNode_2);
		network.addEdge(parentNode, new long[]{childNode_1,childNode_2});
		addToNetwork(network, children[0]);
		addToNetwork(network, children[1]);
	}
	
	public H2DNetwork compileUnLabledInstance(int networkId, H2DInstance inst, LocalNetworkParam param){
		H2DNetwork construct = this.compileUnlabeled(inst);
		long root = this.toNode_generalRoot(inst.getInput().length());
		int rootIdx = Arrays.binarySearch(this._nodes, root);
		H2DNetwork network = new H2DNetwork(networkId,inst,this._nodes,this._children, param,rootIdx+1 );
		//viewer.visualizeNetwork(network, null, "UnLabeled Network");
		//System.err.println("[Info] Compile Unlabeled instance, length: "+inst.getInput().length());
		//System.err.println("My root:"+Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		//System.err.println("root index:"+rootIdx);
		//System.err.println(Arrays.toString(NetworkIDMapper.toHybridNodeArray(this._nodes[this._nodes.length-5])));
		//System.err.println("Root: "+ Arrays.toString(NetworkIDMapper.toHybridNodeArray(root)));
		//System.err.println("Number of nodes under this root Index: "+ (rootIdx+1));
		//network.contains(network);
		return network;
	}
	
	public H2DNetwork compileUnlabeled(H2DInstance inst){
		int[] heads = inst.getHead();
		H2DNetwork network = new H2DNetwork();
		//add the root word and other nodes
		//all are complete nodes.
		long rootE = this.toNode(0, 0, 1, 1, ONE);
		network.addNode(rootE);
		
		for(int rightIndex = 1; rightIndex<=inst.size()-1;rightIndex++){
			//eIndex: 1,2,3,4,5,..n
			for(String e: types){
				if(e.equals(OE)) continue;
				long wordRightNodeE = this.toNode(rightIndex, rightIndex, 1, 1, e); //the leaf node entity, right direction.
				network.addNode(wordRightNodeE);
				long wordLeftNodeE  = this.toNode(rightIndex, rightIndex, 0, 1, e);
				network.addNode(wordLeftNodeE);
			}

			
			for(int L=1;L<=rightIndex;L++){
				//L:(1),(1,2),(1,2,3),(1,2,3,4),(1,2,3,4,5),...(1..n)
				//bIndex:(0),(1,0),(2,1,0),(3,2,1,0),(4,3,2,1,0),...(n-1..0)
				//span: {(0,1)},{(1,2),(0,2)}
				int leftIndex = rightIndex - L;
				//span:[bIndex, rightIndex]
				
				for(int complete=0;complete<=1;complete++){
					for(int direction=0;direction<=1;direction++){
						if(leftIndex==0 && direction==0) continue;
						if(complete==0){
							//incomplete span decompose to two complete spans
							if(direction==1 && heads[rightIndex]!=leftIndex) continue;
							if(direction==0 && heads[leftIndex]!=rightIndex) continue;
							for(int m=leftIndex;m<rightIndex;m++){
								for(int t=0;t<types.length;t++){
									long parent = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									long leftChild = -1;
									long rightChild = -1;
									if(isEntity(types[t]) || types[t].equals(ONE)){
										leftChild = this.toNode(leftIndex, m, 1, 1, types[t]);
										rightChild = this.toNode(m+1, rightIndex, 0, 1,types[t]);
										if(network.contains(leftChild) && network.contains(rightChild)){
											network.addNode(parent);
											network.addEdge(parent, new long[]{leftChild, rightChild});
										}
									}else{ // must be OE
										for(int t1=0;t1<types.length;t1++){
											for(int t2=0;t2<types.length;t2++){
												if(!types[t1].equals(OE) && types[t1].equals(types[t2])) continue;
												leftChild = this.toNode(leftIndex, m, 1, 1, types[t1]);
												rightChild = this.toNode(m+1, rightIndex, 0, 1,types[t2]);
												if(network.contains(leftChild) && network.contains(rightChild)){
													network.addNode(parent);
													network.addEdge(parent, new long[]{leftChild, rightChild});
												}
											}
										}
									}
									
									
								}
							}
						}
						
						if(complete==1 && direction==0){
							for(int m=leftIndex;m<rightIndex;m++){
								if(heads[m]!=rightIndex) continue;
								for(int t=0;t<types.length;t++){
									long parent = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									long leftChild = -1;
									long rightChild = -1;
									if(isEntity(types[t]) || types[t].equals(ONE)){
										leftChild = this.toNode(leftIndex, m, 0, 1, types[t]);
										rightChild = this.toNode(m, rightIndex, 0, 0, types[t]);
										if(network.contains(leftChild) && network.contains(rightChild)){
											network.addNode(parent);
											network.addEdge(parent, new long[]{leftChild, rightChild});
										}
									}else{ // must be OE
										for(int t1=0;t1<types.length;t1++){
											for(int t2=0;t2<types.length;t2++){
												if(!types[t1].equals(OE) && types[t1].equals(types[t2])) continue;
												//if(types[t1].equals(ONE) && isEntity(types[t2])) continue;
												//if(isEntity(types[t1]) && isEntity(types[t2])) continue;
												//if(isEntity(types[t1]) && types[t2].equals(ONE)) continue;
												leftChild = this.toNode(leftIndex, m, 0, 1, types[t1]);
												rightChild = this.toNode(m, rightIndex, 0, 0, types[t2]);
												if(network.contains(leftChild) && network.contains(rightChild)){
													network.addNode(parent);
													network.addEdge(parent, new long[]{leftChild, rightChild});
												}
											}
										}
									}
								}
							}
						}
						
						if(complete==1 && direction==1){
							boolean[] rootAdded = new boolean[types.length];
							for(int x=0;x<rootAdded.length;x++) rootAdded[x]=false;
							for(int m=leftIndex+1;m<=rightIndex;m++){
								if(heads[m]!=leftIndex) continue;
								for(int t=0;t<types.length;t++){
									long parent  = this.toNode(leftIndex, rightIndex, direction, complete, types[t]);
									long leftChild = -1;
									long rightChild = -1;
									if(isEntity(types[t]) || types[t].equals(ONE)){
										leftChild = this.toNode(leftIndex, m, 1, 0, types[t]);
										rightChild = this.toNode(m, rightIndex, 1, 1, types[t]);
										if(network.contains(leftChild) && network.contains(rightChild)){
											network.addNode(parent);
											network.addEdge(parent, new long[]{leftChild, rightChild});
										}
									}else{// parent must be OE
										for(int t1=0;t1<types.length;t1++){
											for(int t2=0;t2<types.length;t2++){
												if(!types[t1].equals(OE) && types[t1].equals(types[t2])) continue;
												//if(types[t1].equals(ONE) && isEntity(types[t2])) continue;
												//if(isEntity(types[t1]) && isEntity(types[t2])) continue;
												//if(isEntity(types[t1]) && types[t2].equals(ONE)) continue;
												
												leftChild = this.toNode(leftIndex, m, 1, 0, types[t1]);
												rightChild = this.toNode(m, rightIndex, 1, 1, types[t2]);
												if(network.contains(leftChild) && network.contains(rightChild)){
													network.addNode(parent);
													network.addEdge(parent, new long[]{leftChild, rightChild});
												}
											}
										}
									}
									if(!rootAdded[t] && network.contains(parent) && leftIndex==0 && rightIndex>leftIndex){
										long subRoot = this.toNode(leftIndex, rightIndex, direction, complete, "null");
										network.addNode(subRoot);
										network.addEdge(subRoot, new long[]{parent});
										rootAdded[t] = true;
									}
									
								}
							}
						}
						
					}
				}
			}
		}
		
		network.finalizeNetwork();
		this._nodes = network.getAllNodes();
		this._children = network.getAllChildren();
		
		//printNodes(this._nodes);
		System.err.println(network.countNodes()+" nodes..");
		//System.err.println(network.toString());
		return network;
		//viewer.visualizeNetwork(network, null, "unLabeled Model");
		//printNetwork(network, null);
		//System.exit(0);
	}
	
	
	@Override
	public Instance decompile(Network network) {
		H2DNetwork dependNetwork = (H2DNetwork)network;
		H2DInstance inst = (H2DInstance)(dependNetwork.getInstance());
		inst = inst.duplicate();
		if(dependNetwork.getMax()==Double.NEGATIVE_INFINITY) return inst;
		//viewer.visualizeNetwork(dependNetwork, null, "Testing Labeled Model:"+network.getInstance().getInstanceId());
		Tree forest = this.toTree(dependNetwork,inst);
//		printNetwork(dependNetwork, (Sentence)dependNetwork.getInstance().getInput());
//		System.err.println("[Result] "+forest.pennString());
		inst.setPrediction(forest);
		return inst;
	}
	
	private Tree toTree(H2DNetwork network,H2DInstance inst){
		
		Tree root = new LabeledScoredTreeNode();
		CoreLabel rootLabel = new CoreLabel();
		rootLabel.setValue("0,"+(inst.getInput().length()-1)+",1,1,null");
		root.setLabel(rootLabel);
		this.toTreeHelper(network, network.countNodes()-1, root);
		return root.getChild(0);
	}
	
	private void toTreeHelper(H2DNetwork network, int node_k, Tree parentNode){
		int[] children_k = network.getMaxPath(node_k);
//		System.err.println("node_k:"+node_k);
//		System.err.println("Parent Node:"+parentNode.toString());
//		System.err.println("Children length:"+children_k.length);
//		System.err.println(node_k+" final score:"+network.getMax(node_k));
		for(int k=0;k<children_k.length;k++){
			long child = network.getNode(children_k[k]);
			int[] ids_child = NetworkIDMapper.toHybridNodeArray(child);
			Tree childNode = new LabeledScoredTreeNode();
			CoreLabel childLabel = new CoreLabel();
			int leftIndex = ids_child[0]-ids_child[1];
			StringBuilder sb = new StringBuilder();
			//ids_child: rightIndex, rightIndex-leftIndex, completeness, direction.
			sb.append(leftIndex);  sb.append(",");
			sb.append(ids_child[0]);  sb.append(",");
			sb.append(ids_child[3]); sb.append(",");
			sb.append(ids_child[2]); sb.append(",");
			String type = null;
			if(ids_child[4]==types.length)
				type = "null";
			else type=types[ids_child[4]];
			sb.append(type);
			childLabel.setValue(sb.toString());
			childNode.setLabel(childLabel);
			parentNode.addChild(childNode);
			this.toTreeHelper(network, children_k[k], childNode);
		}
		
	}
	
	 

	
	//Node composition
	//Span Len (eIndex-bIndex), eIndex, direction(0--left,1--right), complete (0--incomplete,1), node Type
	public long toNode_generalRoot(int sentLen){
		int sentence_len = sentLen;
		//Largest span and the node id is sentence len, because the id 0 to sentence len-1, EMPTY is the general type
		return NetworkIDMapper.toHybridNodeID(new int[]{sentence_len-1, sentence_len-1,1,1,typeMap.size(),NODE.normal.ordinal()});
	}
	
	public long toNode(int leftIndex, int rightIndex, int direction, int complete, String type){
		if(!typeMap.containsKey(type) && !type.equals("null")){
			System.err.println("The type is:"+type);
		}
		if(type.equals("null"))
			return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, typeMap.size(),NODE.normal.ordinal()});
		return NetworkIDMapper.toHybridNodeID(new int[]{rightIndex,rightIndex-leftIndex,complete, direction, typeMap.get(type),NODE.normal.ordinal()});
	}
	
	private boolean isEntity(String type){
		return !type.equals(OE) &&!type.equals(ONE);
	}

}
