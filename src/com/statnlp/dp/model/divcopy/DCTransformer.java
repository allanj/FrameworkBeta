package com.statnlp.dp.model.divcopy;

import java.util.ArrayList;
import java.util.Iterator;

import com.statnlp.commons.types.Sentence;
import com.statnlp.dp.Transformer;
import com.statnlp.dp.utils.DPConfig;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;

public class DCTransformer extends Transformer {

	
	public static String PARENT_IS = DPConfig.PARENT_IS;
	public static String OE = DPConfig.OE;
	public static String ONE = DPConfig.ONE;
	
	public static String O_TYPE = DPConfig.O_TYPE;
	public static String E_B_PREFIX = DPConfig.E_B_PREFIX;
	public static String E_I_PREFIX = DPConfig.E_I_PREFIX;
	
	
	private String[] getLeavesInfo(Sentence sent){
		String[] sentEntities = new String[sent.length()];
		sentEntities[0] = ONE;
		for(int i=1;i<sentEntities.length;i++){
			String type = sent.get(i).getEntity();
			if(type.equals(O_TYPE)){
				sentEntities[i] = ONE;
			}else sentEntities[i] = type.substring(2);
		}
		return sentEntities;
	}

	@Override
	public Tree toSpanTree(Tree dependencyRoot, Sentence sentence){
		boolean haveEntities = false;
		String[] sentEntities = new String[sentence.length()];
		for(int i=0;i<sentEntities.length;i++){
			String type = sentence.get(i).getEntity();
			sentEntities[i] = type.equals(O_TYPE)? type: type.substring(2, type.length());
			if(!type.equals(O_TYPE))
				haveEntities = true;
		}
		Tree spanTreeRoot = new LabeledScoredTreeNode();
		CoreLabel spanRootLabel = new CoreLabel();
		spanRootLabel.setValue("0,"+(sentence.length()-1)+",1,1,"+PARENT_IS+"null");
		spanTreeRoot.setLabel(spanRootLabel);
		Tree spanTreeERoot = new LabeledScoredTreeNode();
		CoreLabel label = new CoreLabel();
		String type = !haveEntities? ONE:OE;
		label.setValue("0,"+(sentence.length()-1)+",1,1,"+type);
		spanTreeERoot.setLabel(label);
		spanTreeRoot.addChild(spanTreeERoot);
		String[] leaves = getLeavesInfo(sentence);
		constructSpanTree(spanTreeERoot,dependencyRoot, leaves);
		//System.err.println(spanTreeRoot.pennString() );
		return spanTreeRoot;
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
				
				Tree leftChildSpan = new LabeledScoredTreeNode();
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				
				boolean isMixed = false;
				String leftType = leaves[pa_leftIndex];
				for(int i=pa_leftIndex+1;i<=maxSentIndex;i++){
					if(!leaves[i].equals(leftType)){isMixed = true; break;}
				}
				leftType = isMixed? OE:leftType;
				
				CoreLabel leftSpanLabel = new CoreLabel(); 
				leftSpanLabel.setValue(pa_leftIndex+","+maxSentIndex+",1,1,"+PARENT_IS+pa_type);
				leftChildSpan.setLabel(leftSpanLabel);
				CoreLabel leftSubSpanLabel = new CoreLabel();
				
				currType = leftType;
				
				leftSubSpanLabel.setValue(pa_leftIndex+","+maxSentIndex+",1,1,"+currType);
				leftChildSubSpan.setLabel(leftSubSpanLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				isMixed = false;
				String rightType = leaves[pa_rightIndex];
				for(int i=maxSentIndex+1; i<=pa_rightIndex-1;i++){
					if(!leaves[i].equals(rightType)) {isMixed = true; break;}
				}
				rightType =isMixed?OE:rightType;
				
				CoreLabel rightSpanLabel = new CoreLabel(); 
				rightSpanLabel.setValue((maxSentIndex+1)+","+pa_rightIndex+",0,1,"+PARENT_IS+pa_type);
				
				currType = rightType;
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); 
				rightSpanSubLabel.setValue((maxSentIndex+1)+","+pa_rightIndex+",0,1,"+currType);
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				rightChildSpan.addChild(rightChildSubSpan);
				
				rightChildSpan.setLabel(rightSpanLabel);
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
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
				Tree leftChildSpan = new LabeledScoredTreeNode();
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				boolean isMixed = false;
				String leftType = leaves[pa_leftIndex];
				for(int i=pa_leftIndex+1;i<=minIndex-1;i++){
					if(!leaves[i].equals(leftType)){isMixed = true; break;}
				}
				leftType = isMixed? OE:leftType;
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+(minIndex-1)+",1,1,"+PARENT_IS+pa_type);
				leftChildSpan.setLabel(leftSpanLabel);
				
				currType = leftType;
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); leftSpanSubLabel.setValue(pa_leftIndex+","+(minIndex-1)+",1,1,"+currType);
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				isMixed = false;
				String rightType = leaves[pa_rightIndex];
				for(int i=minIndex;i<=pa_rightIndex-1;i++){
					if(!leaves[i].equals(rightType)) {isMixed = true; break;}
				}
				rightType =isMixed?OE:rightType;
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(minIndex+","+pa_rightIndex+",0,1,"+PARENT_IS+pa_type);
				rightChildSpan.setLabel(rightSpanLabel);
				
				currType = rightType;
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); rightSpanSubLabel.setValue(minIndex+","+pa_rightIndex+",0,1,"+currType);
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				rightChildSpan.addChild(rightChildSubSpan);
				
				
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
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
				Tree leftChildSpan = new LabeledScoredTreeNode();
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				
				boolean isMixed = false;
				String leftType = leaves[pa_leftIndex];
				for(int i=pa_leftIndex+1;i<=lastChildWordIndex;i++){
					if(!leaves[i].equals(leftType)){isMixed = true; break;}
				}
				leftType = isMixed? OE:leftType;
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+lastChildWordIndex+",1,0,"+PARENT_IS+pa_type);
				leftChildSpan.setLabel(leftSpanLabel);
				
				currType = leftType;
				
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); leftSpanSubLabel.setValue(pa_leftIndex+","+lastChildWordIndex+",1,0,"+currType);
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				
				isMixed = false;
				String rightType = leaves[lastChildWordIndex];
				for(int i=lastChildWordIndex+1;i<=pa_rightIndex;i++){
					if(!leaves[i].equals(rightType)){isMixed= true; break;}
				}
				rightType = isMixed? OE:rightType;
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(lastChildWordIndex+","+pa_rightIndex+",1,1,"+PARENT_IS+pa_type);
				rightChildSpan.setLabel(rightSpanLabel);
				
				currType = rightType;
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); rightSpanSubLabel.setValue(lastChildWordIndex+","+pa_rightIndex+",1,1,"+currType);
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				rightChildSpan.addChild(rightChildSubSpan);
				
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
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
				Tree leftChildSpan = new LabeledScoredTreeNode();
				Tree rightChildSpan = new LabeledScoredTreeNode();
				Tree leftChildSubSpan = new LabeledScoredTreeNode();
				Tree rightChildSubSpan = new LabeledScoredTreeNode();
				boolean isMixed = false;
				String leftType = leaves[firstChildWordIndex];
				for(int i=pa_leftIndex;i<=firstChildWordIndex-1;i++){
					if(!leaves[i].equals(leftType)){isMixed = true; break;}
				}
				leftType = isMixed? OE:leftType;	
			
				CoreLabel leftSpanLabel = new CoreLabel(); leftSpanLabel.setValue(pa_leftIndex+","+firstChildWordIndex+",0,1,"+PARENT_IS+pa_type);
				leftChildSpan.setLabel(leftSpanLabel);
				
				currType = leftType;
				
				CoreLabel leftSpanSubLabel = new CoreLabel(); leftSpanSubLabel.setValue(pa_leftIndex+","+firstChildWordIndex+",0,1,"+currType);
				leftChildSubSpan.setLabel(leftSpanSubLabel);
				leftChildSpan.addChild(leftChildSubSpan);
				
				
				isMixed = false;
				String rightType = leaves[firstChildWordIndex];
				for(int i=firstChildWordIndex+1;i<=pa_rightIndex;i++){
					if(!leaves[i].equals(rightType)){isMixed=true; break;}
				}
				rightType = isMixed? OE:rightType;	
				
				CoreLabel rightSpanLabel = new CoreLabel(); rightSpanLabel.setValue(firstChildWordIndex+","+pa_rightIndex+",0,0,"+PARENT_IS+pa_type);
				rightChildSpan.setLabel(rightSpanLabel);
				
				currType = rightType;
				
				CoreLabel rightSpanSubLabel = new CoreLabel(); rightSpanSubLabel.setValue(firstChildWordIndex+","+pa_rightIndex+",0,0,"+currType);
				rightChildSubSpan.setLabel(rightSpanSubLabel);
				rightChildSpan.addChild(rightChildSubSpan);
				
				currentSpan.addChild(leftChildSpan);
				currentSpan.addChild(rightChildSpan);
				constructSpanTree(leftChildSubSpan, copyFirstChildWord,leaves);
				constructSpanTree(rightChildSubSpan, currentDependency,leaves);
			}
		}
	}

	

}