package com.statnlp.dp.model.lab;

import java.util.ArrayList;
import java.util.Iterator;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.Transformer;
import com.statnlp.dp.commons.Entity;
import com.statnlp.dp.utils.DPConfig;
import com.statnlp.dp.utils.DataChecker;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class LABTransformer extends Transformer {

	
	
	public static String O_TYPE = DPConfig.O_TYPE;
	public static String E_B_PREFIX = DPConfig.E_B_PREFIX;
	public static String E_I_PREFIX = DPConfig.E_I_PREFIX;
	
	
	private String[] getLeavesInfo(Sentence sent){
		String[] sentEntities = new String[sent.length()];
		sentEntities[0] = O_TYPE;
		for(int i=1;i<sentEntities.length;i++){
			String type = sent.get(i).getEntity();
			if(type.equals(O_TYPE)){
				sentEntities[i]= O_TYPE;
			}else if(type.startsWith(E_B_PREFIX)){
				sentEntities[i] = type.substring(2);
			}else if(type.startsWith(E_I_PREFIX)){
				sentEntities[i] = type.substring(2);
			}
		}
		return sentEntities;
	}

	@Override
	public Tree toSpanTree(Tree dependencyRoot, Sentence sentence){
		Tree spanTreeERoot = new LabeledScoredTreeNode();
		CoreLabel label = new CoreLabel();
		label.setValue(setSpanInfo(0, sentence.length()-1, 1, 1, DPConfig.O_TYPE));
		spanTreeERoot.setLabel(label);
		String[] leaves = getLeavesInfo(sentence);
		constructSpanTree(spanTreeERoot,dependencyRoot, leaves);
//		System.err.println(spanTreeERoot.pennString() );
		return spanTreeERoot;
	}
	
	/**
	 * Construct the span tree recursively using the dependency tree.
	 * @param currentSpan
	 * @param currentDependency
	 * @param sentence
	 */
	private void constructSpanTree(Tree currentSpan, Tree currentDependency,String[] leaves){
		
		CoreLabel currentSpanLabel = (CoreLabel)(currentSpan.label());
		String[] info = currentSpanLabel.value().split(",");
		int pa_leftIndex = Integer.valueOf(info[0]);
		int pa_rightIndex = Integer.valueOf(info[1]);
		int pa_direction = Integer.valueOf(info[2]);
		int pa_completeness = Integer.valueOf(info[3]);
		String pa_type = info[4];
		String currType = null;
		if(pa_leftIndex==pa_rightIndex){
			return;
		}
		if(pa_completeness==0){
			if(pa_direction==1){
				Tree lastChildWord = currentDependency.lastChild();
				Tree copyLastChildWord = lastChildWord.deepCopy(); 
				currentDependency.removeChild(currentDependency.numChildren()-1);
				Iterator<Tree> iter = currentDependency.iterator();
				int maxSentIndex = -1;
				while(iter.hasNext()){
					Tree now = iter.next();
					CoreLabel cl = (CoreLabel)(now.label());
					if(cl.sentIndex()>maxSentIndex) maxSentIndex = cl.sentIndex();
				}
				
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				
				CoreLabel leftSubSpanLabel = new CoreLabel();
				leftSubSpanLabel.setValue(this.setSpanInfo(pa_leftIndex, maxSentIndex, 1, 1, pa_type));
				leftChildSubSpan.setLabel(leftSubSpanLabel);
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); 
				rightSpanSubLabel.setValue(this.setSpanInfo(maxSentIndex+1, pa_rightIndex, 0, 1, pa_type));
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				
				currentSpan.addChild(leftChildSubSpan);
				currentSpan.addChild(rightChildSubSpan);
				constructSpanTree(leftChildSubSpan, currentDependency,leaves);
				constructSpanTree(rightChildSubSpan, copyLastChildWord,leaves);
			}else{
				Tree firstChildWord = currentDependency.firstChild();
				Tree copyFirstChildWord = firstChildWord.deepCopy();
				currentDependency.removeChild(0);
				Iterator<Tree> iter = currentDependency.iterator();
				int minIndex = leaves.length+1;
				while(iter.hasNext()){
					Tree now = iter.next();
					CoreLabel cl = (CoreLabel)(now.label());
					if(cl.sentIndex()<minIndex) minIndex = cl.sentIndex();
				}
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); 
				leftSpanSubLabel.setValue(this.setSpanInfo(pa_leftIndex, minIndex-1, 1, 1, pa_type));
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); 
				rightSpanSubLabel.setValue(this.setSpanInfo(minIndex, pa_rightIndex, 0, 1, pa_type));
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				
				currentSpan.addChild(leftChildSubSpan);
				currentSpan.addChild(rightChildSubSpan);
				constructSpanTree(leftChildSubSpan, copyFirstChildWord,leaves);
				constructSpanTree(rightChildSubSpan, currentDependency,leaves);
			}
		}else{
			if(pa_direction==1){
				//complete and right
				Tree lastChildWord = currentDependency.lastChild();
				
				CoreLabel lastChildWordLabel = (CoreLabel)(lastChildWord.label());
				int lastChildWordIndex = lastChildWordLabel.sentIndex();
				Tree copyLastChildWord = lastChildWord.deepCopy();
				Tree[] children = lastChildWord.children();
				ArrayList<Integer> idsRemove4Last = new ArrayList<Integer>();
				ArrayList<Integer> idsRemove4Copy = new ArrayList<Integer>();
				for(int i=0; i<children.length;i++){
					CoreLabel label = (CoreLabel)(children[i].label());
					int sentIndex = label.sentIndex();
					if(sentIndex<lastChildWordIndex){
						idsRemove4Copy.add(i);
					}else{
						idsRemove4Last.add(i);
					}
				}
				for(int j=idsRemove4Last.size()-1;j>=0;j--) lastChildWord.removeChild(idsRemove4Last.get(j));
				for(int j=idsRemove4Copy.size()-1;j>=0;j--) copyLastChildWord.removeChild(idsRemove4Copy.get(j));
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				boolean isMixed = false;
				String leftType = leaves[pa_leftIndex];
				for(int i=pa_leftIndex+1;i<=lastChildWordIndex;i++){
					if(!leaves[i].equals(leftType)){isMixed = true; break;}
				}
				leftType = isMixed? O_TYPE:leftType;
				currType = leftType;
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); 
				leftSpanSubLabel.setValue(this.setSpanInfo(pa_leftIndex, lastChildWordIndex, 1, 0, currType));
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); 
				rightSpanSubLabel.setValue(this.setSpanInfo(lastChildWordIndex, pa_rightIndex, 1, 1, pa_type));
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				
				currentSpan.addChild(leftChildSubSpan);
				currentSpan.addChild(rightChildSubSpan);
				constructSpanTree(leftChildSubSpan, currentDependency,leaves);
				constructSpanTree(rightChildSubSpan, copyLastChildWord,leaves);
			}else{
				//complete and left
				Tree firstChildWord = currentDependency.firstChild();
				CoreLabel firstChildWordLabel = (CoreLabel)(firstChildWord.label());
				int firstChildWordIndex = firstChildWordLabel.sentIndex();
				Tree copyFirstChildWord = firstChildWord.deepCopy();
				Tree[] children = firstChildWord.children();
				ArrayList<Integer> idsRemove4First = new ArrayList<Integer>();
				ArrayList<Integer> idsRemove4Copy = new ArrayList<Integer>();
				for(int i=0; i<children.length;i++){
					CoreLabel label = (CoreLabel)(children[i].label());
					int sentIndex = label.sentIndex();
					if(sentIndex<firstChildWordIndex){
						idsRemove4First.add(i);
					}else{
						idsRemove4Copy.add(i);
					}
				}
				for(int j=idsRemove4First.size()-1;j>=0;j--) firstChildWord.removeChild(idsRemove4First.get(j));
				for(int j=idsRemove4Copy.size()-1;j>=0;j--) copyFirstChildWord.removeChild(idsRemove4Copy.get(j));
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); 
				leftSpanSubLabel.setValue(this.setSpanInfo(pa_leftIndex, firstChildWordIndex, 0, 1, pa_type));
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				
				
				boolean isMixed = false;
				String rightType = leaves[firstChildWordIndex];
				for(int i=firstChildWordIndex+1;i<=pa_rightIndex;i++){
					if(!leaves[i].equals(rightType)){isMixed=true; break;}
				}
				rightType = isMixed? O_TYPE:rightType;	
				currType = rightType;
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); 
				rightSpanSubLabel.setValue(this.setSpanInfo(firstChildWordIndex, pa_rightIndex, 0, 0, currType));
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				
				currentSpan.addChild(leftChildSubSpan);
				currentSpan.addChild(rightChildSubSpan);
				constructSpanTree(leftChildSubSpan, copyFirstChildWord,leaves);
				constructSpanTree(rightChildSubSpan, currentDependency,leaves);
			}
		}
	}

	
	
	
	private String setSpanInfo(int start, int end, int direction, int completeness, String type){
		return new String(start+","+end+","+direction+","+completeness+","+type);
	}
}
