package com.neocoretechs.bigsack.io.request.cluster;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import com.neocoretechs.bigsack.io.IoInterface;
import com.neocoretechs.bigsack.io.cluster.NodeBlockBuffer;
import com.neocoretechs.bigsack.io.cluster.NodeBlockBufferInterface;
import com.neocoretechs.bigsack.io.pooled.Datablock;


public final class FSeekAndReadRequest extends AbstractClusterWork implements CompletionLatchInterface, Serializable {
	private static final long serialVersionUID = 8403024200125854512L;
	private static final boolean DEBUG = false;
	private transient IoInterface ioUnit;
	private long offset;
	private Datablock dblk;
	private int tablespace;
	private transient CountDownLatch barrierCount;
	private transient NodeBlockBuffer blockBuffer;
	public FSeekAndReadRequest() {}
	public FSeekAndReadRequest(CountDownLatch barrierCount, long offset, Datablock dblk) {
		this.barrierCount = barrierCount;
		this.offset = offset;
		this.dblk = dblk;
	}
	@Override
	public void process() throws IOException {
		FseekAndRead(this.offset, this.dblk);
		barrierCount.countDown();
	}
	/**
	 * IoInterface should be set up before we come in here. We assume toffset is real block position
	 * in this tablespace since we have come here knowing our tablespace number and so our real block number
	 * was also extracted from the virtual block we started with.
	 * @param toffset
	 * @param tblk
	 * @throws IOException
	 */
	private void FseekAndRead(long toffset, Datablock tblk) throws IOException {
			if (tblk.isIncore())
				throw new RuntimeException(
					"FseekAndReadReuest block incore preempts read "
						+ toffset
						+ " "
						+ tblk);
			// see if its buffered

			Datablock dblk = blockBuffer.get(offset);
			if( dblk == null ) {
				ioUnit.Fseek(offset);
				tblk.readUsed(ioUnit);
				blockBuffer.put(offset, tblk);
			} else {
				dblk.doClone(tblk); // put to tblk from dblk
			}

		if( DEBUG ) System.out.println("FseekAndRead in "+this.toString()+" exiting");
	}
	@Override
	public long getLongReturn() {
		return offset;
	}

	@Override
	public Object getObjectReturn() {
		return this.dblk;
	}
	/**
	 * This interface implemented method is called by IoWorker before processing
	 */
	@Override
	public void setIoInterface(IoInterface ioi) {
		this.ioUnit = ioi;
		blockBuffer = ((NodeBlockBufferInterface)ioUnit).getBlockBuffer();
	}
	@Override
	public void setTablespace(int tablespace) {
		this.tablespace = tablespace;
	}
	
	public String toString() {
		return getUUID()+",tablespace:"+tablespace+" FSeekAndReadRequest:"+offset+" data: "+dblk;
	}
	/**
	 * The latch will be extracted by the UDPMaster and when a response comes back it will be tripped
	 */
	@Override
	public CountDownLatch getCountDownLatch() {
		return barrierCount;
	}

	@Override
	public void setCountDownLatch(CountDownLatch cdl) {
		barrierCount = cdl;
	}
	
	@Override
	public void setLongReturn(long val) {
		offset = val;
	}

	@Override
	public void setObjectReturn(Object o) {
		dblk = (Datablock) o;	
	}
	@Override
	public CyclicBarrier getCyclicBarrier() {
		return null;
	}
	@Override
	public void setCyclicBarrier(CyclicBarrier cb) {
	}
	@Override
	public boolean doPropagate() {
		return true;
	}

}
