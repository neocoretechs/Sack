package com.neocoretechs.bigsack.btree;

import java.io.IOException;

import com.neocoretechs.bigsack.io.pooled.ObjectDBIO;

/**
 * Request sent to NodeSplitThread operating on parent nodes. If another NodeSplitThread
 * request is also in progress with the same data and sharing, a synchronization lock on the former parent node is needed.
 * A CyclicBarrier with node split participants as waiters is used to determine a barrier synchronization point.
 * This barrier is set up in the main calling thread and awaited in the node split processors 
 * @author jg Copyright (c) NeoCoreTechs 2015
 *
 */
public final class NodeSplitRequest extends AbstractNodeSplitRequest {
	private static boolean DEBUG = false;
	BTreeKeyPage newNode = null;
	static enum NODETYPE {NODE_LEFT,NODE_RIGHT};
	// from rootOffs to MAXKEYS is range of keys to extract
	private static int rootOffs = BTreeKeyPage.MAXKEYS - keysToMove; // the place in root to start pulling keys
	private int nodeOffs;

	public NodeSplitRequest(ObjectDBIO globalIO, BTreeKeyPage targetPage, NODETYPE ntype) {
				this.globalIO = globalIO;
				this.oldRoot = targetPage;
				switch(ntype) {
					case NODE_LEFT:
						nodeOffs = 0;
						break;
					case NODE_RIGHT:
						nodeOffs = rootOffs;
				}
	}
	/**
	 * Attempt insert, we may not have it on this node. A slit is always assumed necessary if we get here.
	 */
	@Override
	public void process() throws IOException {
		// get the new page
		newNode = BTreeKeyPage.getPageFromPool(globalIO);

		// pull everything from the right nodes and move it down
		synchronized(oldRoot) {
			// 
			for(int i = 0; i < keysToMove; i++) {
					BTreeMain.moveKeyData(oldRoot, nodeOffs+i, newNode, i, true);
					BTreeMain.moveChildData(oldRoot, nodeOffs+i, newNode, i, true);
			}
			//
			newNode.numKeys = keysToMove;
			// See if leaf node, simply, no pointers out
			newNode.mIsLeafNode = true;
			for( int i = 0; i < newNode.numKeys; i++) {
				if( newNode.pageIdArray[i] != -1L ) {
					newNode.mIsLeafNode = false;
					break;
				}
			}
			newNode.setUpdated(true);
			// set our new left/right pointers from root
			if(nodeOffs == 0) { // left
				oldRoot.pageIdArray[BTreeMain.T-1] = newNode.pageId;
				oldRoot.pageArray[BTreeMain.T-1] = newNode;
			} else {
				oldRoot.pageIdArray[BTreeMain.T] = newNode.pageId;
				oldRoot.pageArray[BTreeMain.T] = newNode;
			}
				
			oldRoot.setUpdated(true);
		}

	}

	@Override
	public long getLongReturn() {
		return newNode.pageId;
	}

	@Override
	public Object getObjectReturn() {
		return newNode;
	}

}