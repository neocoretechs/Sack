package com.neocoretechs.bigsack.io.cluster;

import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import com.neocoretechs.bigsack.io.IoInterface;
import com.neocoretechs.bigsack.io.pooled.Datablock;
/**
 * Implementation of the simple block pool cache for a remote worker node.
 * We deal strictly with long pointers and datablocks, no further semantics at this level.
 * A concurrentHashMap of entries with fundamental operations is supported.
 * @author jg
 *
 */
public final class NodeBlockBuffer  {
	private static boolean DEBUG = false;
	private static int NODEPOOLBLOCKS = 10000;
	private ConcurrentHashMap<Long, Datablock> blockBuffer;
	private IoInterface rawStore;
	
	public NodeBlockBuffer(IoInterface rawStore) {
		this.rawStore = rawStore;
		blockBuffer = new ConcurrentHashMap<Long,Datablock>(NODEPOOLBLOCKS);
	}
	
	public void put(Long ptr, Datablock dblk) {
		if( blockBuffer.size() >= NODEPOOLBLOCKS ) {
			// must toss one first
			Enumeration<Long> e = blockBuffer.keys();
			Long rec = -1L;
			while(e.hasMoreElements()) {
				rec = e.nextElement();
				if( rec != 0L ) {
					Datablock tblk = blockBuffer.get(rec);
					// if it is still waiting for outstanding write, dont dump it
					if( !tblk.isIncore() )
						break;
				}
			}
			if( rec == -1L ) 
				throw new RuntimeException("NodeBlockBuffer unable to clear buffer slot for new block, buffer full.");
			blockBuffer.remove(rec);
		}
		blockBuffer.put(ptr, dblk);
	}
	/**
	 * Stop a designated ioUnit attached to this buffer
	 * @throws IOException 
	 */
	public void force() throws IOException {
		if( DEBUG )
		System.out.println("NodeBlockBuffer.force writing node block buffer with "+blockBuffer.size()+" entries.");
		int stillIn = 0;
		Enumeration<Long> e = blockBuffer.keys();
		Long rec = -1L;
		while(e.hasMoreElements()) {
			rec = e.nextElement();
				Datablock tblk = blockBuffer.get(rec);
				// if it is still waiting for outstanding write, dont dump it
				if( tblk.isIncore() ) {
					if( DEBUG )
					System.out.println("!!!!!NodeBlockBuffer, block "+rec+" is in core during shutdown:"+tblk);
					tblk.write(rawStore);
					tblk.setIncore(false);
					++stillIn;
				}
		}
		rawStore.Fforce(); // synch
		if( DEBUG )
		System.out.println("Node block buffer cleared with "+stillIn+" blocks outstanding written.");
	}
	
	public Datablock get(Long ptr) {
		return blockBuffer.get(ptr);
	}
}
