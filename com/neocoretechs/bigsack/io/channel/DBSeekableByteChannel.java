package com.neocoretechs.bigsack.io.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.neocoretechs.bigsack.io.pooled.OffsetDBIOInterface;
/**
 * This class bridges the block pool and the computational elements.
 * It functions as a bytechannel into the block pool to read and write 
 * pages stored there. The primary storage buffers are ByteBuffer. The serialization
 * of objects is performed by acquiring streams through the 'Channels' nio class.
 * @author jg
 *
 */
public final class DBSeekableByteChannel implements SeekableByteChannel {
	private OffsetDBIOInterface sdbio;
	private long blockNum;
	private int position = 0;
	public DBSeekableByteChannel(OffsetDBIOInterface sdbio) {
		this.sdbio = sdbio;
	}
	public synchronized void setBlockNumber(long bnum) {
		this.blockNum = bnum;
	}
	@Override
	public void close() throws IOException {
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public synchronized long position() throws IOException {
		return position;
	}

	@Override
	public synchronized SeekableByteChannel position(long arg0) throws IOException {
		synchronized(sdbio) {
			sdbio.objseek(blockNum);
			position = (int) arg0;
			if( position > 0 )
				sdbio.seek_fwd(position);
		}
		return this;
	}

	@Override
	public synchronized int read(ByteBuffer arg0) throws IOException {
		synchronized(sdbio) {
			int size = sdbio.readn(arg0, arg0.limit());
			position += size;
			return (size == 0 ? -1: size);
		}
	}

	@Override
	public synchronized long size() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public synchronized SeekableByteChannel truncate(long arg0) throws IOException {
		return this;
	}

	@Override
	public synchronized int write(ByteBuffer arg0) throws IOException {
		synchronized(sdbio) {
		return sdbio.writen(arg0, arg0.limit());
		}
	}

}
