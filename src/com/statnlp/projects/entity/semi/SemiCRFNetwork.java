package com.statnlp.projects.entity.semi;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.TableLookupNetwork;

public class SemiCRFNetwork extends TableLookupNetwork {
	
	private static final long serialVersionUID = -8384557055081197941L;
	public int numNodes = -1;

	public SemiCRFNetwork() {}

	public SemiCRFNetwork(int networkId, Instance inst, LocalNetworkParam param, NetworkCompiler compiler) {
		super(networkId, inst, param,compiler);
	}

	public SemiCRFNetwork(int networkId, Instance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes, NetworkCompiler compiler) {
		super(networkId, inst, nodes, children, param,compiler);
		this.numNodes = numNodes;
	}
	
	public int countNodes(){
		if(numNodes < 0){
			return super.countNodes();
		}
		return numNodes;
	}
	
	public void remove(int k){}
	
	public boolean isRemoved(int k){
		return false;
	}

}
