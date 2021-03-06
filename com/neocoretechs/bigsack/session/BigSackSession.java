package com.neocoretechs.bigsack.session;
import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Stream;

import com.neocoretechs.bigsack.DBPhysicalConstants;
import com.neocoretechs.bigsack.btree.BTreeMain;
import com.neocoretechs.bigsack.btree.TreeSearchResult;
import com.neocoretechs.bigsack.io.ThreadPoolManager;
import com.neocoretechs.bigsack.io.pooled.Datablock;
import com.neocoretechs.bigsack.io.pooled.GlobalDBIO;
import com.neocoretechs.bigsack.iterator.EntrySetIterator;
import com.neocoretechs.bigsack.iterator.HeadSetIterator;
import com.neocoretechs.bigsack.iterator.HeadSetKVIterator;
import com.neocoretechs.bigsack.iterator.KeySetIterator;
import com.neocoretechs.bigsack.iterator.SubSetIterator;
import com.neocoretechs.bigsack.iterator.SubSetKVIterator;
import com.neocoretechs.bigsack.iterator.TailSetIterator;
import com.neocoretechs.bigsack.iterator.TailSetKVIterator;
import com.neocoretechs.bigsack.stream.EntrySetStream;
import com.neocoretechs.bigsack.stream.HeadSetKVStream;
import com.neocoretechs.bigsack.stream.HeadSetStream;
import com.neocoretechs.bigsack.stream.KeySetStream;
import com.neocoretechs.bigsack.stream.SackStream;
import com.neocoretechs.bigsack.stream.SubSetKVStream;
import com.neocoretechs.bigsack.stream.SubSetStream;
import com.neocoretechs.bigsack.stream.TailSetKVStream;
import com.neocoretechs.bigsack.stream.TailSetStream;
/*
* Copyright (c) 2003, NeoCoreTechs
* All rights reserved.
* Redistribution and use in source and binary forms, with or without modification, 
* are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of
* conditions and the following disclaimer. 
* Redistributions in binary form must reproduce the above copyright notice, 
* this list of conditions and the following disclaimer in the documentation and/or
* other materials provided with the distribution. 
* Neither the name of NeoCoreTechs nor the names of its contributors may be 
* used to endorse or promote products derived from this software without specific prior written permission. 
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED 
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
* PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
* ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
* TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
* HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
* OF THE POSSIBILITY OF SUCH DAMAGE.
*
*/
/**
* Session object. Returned by SessionManager.Connect().
* Responsible for providing generic access to BTreeMain from which other
* specific collection types can obtain their functionality.  Operations include
* handing out iterators, inserting and deleting objects, size, navigation, clearing,
* and handling commit and rollback.
* @author Jonathan Groff (C) NeoCoreTechs 2003, 2017, 2021
*/
public final class BigSackSession {
	private boolean DEBUG = false;
	public static final boolean COMMIT = false;
	public static final boolean ROLLBACK = true;
	private int uid;
	private int gid;
	private BTreeMain bTree;
	/**
	* Create a new session
	* @param bTree The BTreeMain object than handles the BTree key pages indexing the objects in the deep store.
	* @param tuid The user
	* @param tgis The group
	* @exception IOException If global IO problem
	*/
	protected BigSackSession(BTreeMain bTree, int uid, int gid)  {
		this.bTree = bTree;
		this.uid = uid;
		this.gid = gid;
		if( DEBUG )
			System.out.println("BigSackSession constructed with db:"+getDBPath()+" using remote DB:"+getRemoteDBName());
	}

	public long getTransactionId() { return bTree.getIO().getTransId(); }
	
	public String getDBname() {
		return bTree.getIO().getDBName();
	}
	public String getDBPath() {
		return bTree.getIO().getDBPath();
	}
	public String getRemoteDBName() {
		return bTree.getIO().getRemoteDBName();
	}
	@Override
	public String toString() {
		return "BigSackSession using DB:"+getDBname()+" path:"+getDBPath()+" remote:"+getRemoteDBName();
	}
	protected int getUid() {
		return uid;
	}
	protected int getGid() {
		return gid;
	}

	public Object getMutexObject() {
		return bTree;
	}

	@SuppressWarnings("rawtypes")
	public boolean put(Comparable o) throws IOException {
		return (bTree.add(o) == 0 ? false : true);
	}

	@SuppressWarnings("rawtypes")
	public boolean put(Comparable key, Object o) throws IOException {
		return (bTree.add(key, o) == 0 ? false : true);
	}

	@SuppressWarnings("rawtypes")
	public Object get(Comparable o) throws IOException {
		TreeSearchResult tsr = bTree.seekKey(o);
		if(tsr.atKey)
			return bTree.getCurrentObject();
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public Object getValue(Object o) throws IOException {
		return bTree.seekObject(o);
	}
	
	@SuppressWarnings("rawtypes")
	public TreeSearchResult locate(Comparable key) throws IOException {
		TreeSearchResult tsr = bTree.locate(key);
		return tsr;
	}
	
	/**
	* Not a real subset, returns iterator vs set.
	* 'from' element inclusive, 'to' element exclusive
	* @param fkey Return from fkey
	* @param tkey Return from fkey to strictly less than tkey
	* @return The Iterator over the subSet
	* @exception IOException If we cannot obtain the iterator
	*/
	@SuppressWarnings("rawtypes")
	public Iterator<?> subSet(Comparable fkey, Comparable tkey)
		throws IOException {
		return new SubSetIterator(fkey, tkey, bTree);
	}
	
	public Stream<?> subSetStream(Comparable fkey, Comparable tkey)
			throws IOException {
		return new SubSetStream(new SubSetIterator(fkey, tkey, bTree));
	}
	
	/**
	* Not a real subset, returns iterator vs set.
	* 'from' element inclusive, 'to' element exclusive
	* @param fkey Return from fkey
	* @param tkey Return from fkey to strictly less than tkey
	* @return The KeyValuePair Iterator over the subSet
	* @exception IOException If we cannot obtain the iterator
	*/
	@SuppressWarnings("rawtypes")
	public Iterator<?> subSetKV(Comparable fkey, Comparable tkey)
		throws IOException {
		return new SubSetKVIterator(fkey, tkey, bTree);
	}
	
	public Stream<?> subSetKVStream(Comparable fkey, Comparable tkey)
			throws IOException {
			return new SubSetKVStream(fkey, tkey, bTree);
	}
	
	/**
	* Not a real subset, returns iterator
	* @return The Iterator over the entrySet
	* @exception IOException If we cannot obtain the iterator
	*/
	public Iterator<?> entrySet() throws IOException {
		return new EntrySetIterator(bTree);
	}
	
	public Stream<?> entrySetStream() throws IOException {
		return new EntrySetStream(bTree);
	}
	/**
	* Not a real subset, returns Iterator
	* @param tkey return from head to strictly less than tkey
	* @return The Iterator over the headSet
	* @exception IOException If we cannot obtain the iterator
	*/
	@SuppressWarnings("rawtypes")
	public Iterator<?> headSet(Comparable tkey) throws IOException {
		return new HeadSetIterator(tkey, bTree);
	}
	
	public Stream<?> headSetStream(Comparable tkey) throws IOException {
		return new HeadSetStream(tkey, bTree);
	}
	/**
	* Not a real subset, returns Iterator
	* @param tkey return from head to strictly less than tkey
	* @return The KeyValuePair Iterator over the headSet
	* @exception IOException If we cannot obtain the iterator
	*/
	@SuppressWarnings("rawtypes")
	public Iterator<?> headSetKV(Comparable tkey) throws IOException {
		return new HeadSetKVIterator(tkey, bTree);
	}
	
	public Stream<?> headSetKVStream(Comparable tkey) throws IOException {
		return new HeadSetKVStream(tkey, bTree);
	}
	/**
	* Return the keyset Iterator over all elements
	* @return The Iterator over the keySet
	* @exception IOException If we cannot obtain the iterator
	*/
	public Iterator<?> keySet() throws IOException {
		return new KeySetIterator(bTree);
	}
	
	public Stream<?> keySetStream() throws IOException {
		return new KeySetStream(bTree);
	}
	/**
	* Not a real subset, returns Iterator
	* @param fkey return from value to end
	* @return The Iterator over the tailSet
	* @exception IOException If we cannot obtain the iterator
	*/
	@SuppressWarnings("rawtypes")
	public Iterator<?> tailSet(Comparable fkey) throws IOException {
		return new TailSetIterator(fkey, bTree);
	}
	
	public Stream<?> tailSetStream(Comparable fkey) throws IOException {
		return new TailSetStream(fkey, bTree);
	}
	/**
	* Not a real subset, returns Iterator
	* @param fkey return from value to end
	* @return The KeyValuePair Iterator over the tailSet
	* @exception IOException If we cannot obtain the iterator
	*/
	@SuppressWarnings("rawtypes")
	public Iterator<?> tailSetKV(Comparable fkey) throws IOException {
		return new TailSetKVIterator(fkey, bTree);
	}
	
	public Stream<?> tailSetKVStream(Comparable fkey) throws IOException {
		return new TailSetKVStream(fkey, bTree);
	}
	/**
	 * Contains a value object
	 * @param o
	 * @return boolean if the value object is found
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public boolean containsValue(Object o) throws IOException {
		Object obj = bTree.seekObject(o);
		if( obj != null ) {
			bTree.getIO().deallocOutstanding();
			if(DEBUG)
				System.out.println("sought object:"+o+" found:"+obj);
			return obj.equals(o);
		}
		bTree.getIO().deallocOutstanding();
		return false;
	}
	
	/**
	 * Contains a value object
	 * @param o
	 * @return boolean if the value object is found
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public boolean contains(Comparable o) throws IOException {
		// return TreeSearchResult
		TreeSearchResult tsr = bTree.seekKey(o);
		bTree.getIO().deallocOutstanding();
		return tsr.atKey;
	}
	
	/**
	* @return null or previous object
	*/
	@SuppressWarnings("rawtypes")
	public Object remove(Comparable o) throws IOException {
		bTree.delete(o);
		return o; //fluent interface style
	}
	
	public Object first() throws IOException {
		bTree.rewind();
		Object retVal = bTree.getCurrentObject();
		bTree.getIO().deallocOutstanding();
		bTree.clearStack();
		return retVal;
	}
	
	@SuppressWarnings("rawtypes")
	public Comparable firstKey() throws IOException {
		bTree.rewind();
		Comparable retVal = bTree.getCurrentKey();
		bTree.getIO().deallocOutstanding();
		bTree.clearStack();
		return retVal;
	}
	
	public Object last() throws IOException {
		bTree.toEnd();
		Object retVal = bTree.getCurrentObject();
		bTree.getIO().deallocOutstanding();
		bTree.clearStack();
		return retVal;
	}
	
	@SuppressWarnings("rawtypes")
	public Comparable lastKey() throws IOException {
		bTree.toEnd();
		Comparable retVal = bTree.getCurrentKey();
		bTree.getIO().deallocOutstanding();
		bTree.clearStack();
		return retVal;
	}
	
	public long size() throws IOException {
		return bTree.count();
	}

	public boolean isEmpty() throws IOException {
		return bTree.isEmpty();
	}

	/**
	* Close this session.
	* @param rollback true to roll back, false to commit
	* @exception IOException For low level failure
	*/
	public void Close(boolean rollback) throws IOException {
		rollupSession(rollback);
	}
	
	public void Open() throws IOException {
		bTree.getIO().getIOManager().Fopen();
	}
	
	/**
	* @exception IOException for low level failure
	*/
	void Rollback() throws IOException {
		Close(true);
	}
	
	/**
	* Commit the blocks.
	* @exception IOException For low level failure
	*/
	void Commit() throws IOException {
		Close(false);
	}
	/**
	 * Checkpoint the current transaction
	 * @throws IOException 
	 * @throws IllegalAccessException 
	 */
	void Checkpoint() throws IllegalAccessException, IOException {
		for(int i = 0; i < DBPhysicalConstants.DTABLESPACES; i++)
				bTree.getIO().getIOManager().getUlog(i).checkpoint();
	}
	/**
	* Generic session roll up.  Data is committed based on rollback param.
	* We deallocate the outstanding block
	* We iterate the tablespaces for each db removing obsolete log files.
	* Remove the NODESPLITWORKER threads from BTreeMain, then remove this session from the SessionManager
	* @param rollback true to roll back, false to commit
	* @exception IOException For low level failure
	*/
	private void rollupSession(boolean rollback) throws IOException {
		if (rollback) {
			bTree.getIO().deallocOutstandingRollback();
		} else {
			// calls commitbufferflush
			bTree.getIO().deallocOutstandingCommit();
		}
		for(int i = 0; i < DBPhysicalConstants.DTABLESPACES; i++)
			bTree.getIO().getIOManager().getUlog(i).getLogToFile().deleteOnlineArchivedLogFiles();
		SessionManager.releaseSession(this);
	}
	
	/**
	* This forces a close with rollback.
	* for offlining of db's
	* @exception IOException if low level error occurs
	*/
	protected void forceClose() throws IOException {
		Close(true);
	}
	/**
	 * hidden easter egg or dangerous secret, its up to you
	 */
	public BTreeMain getBTree() { return bTree; }
	/**
	 * Scans through tablespaces and analyzes space utilization
	 * BEWARE - doing this under multithreading, well, youre asking for it..
	 * @exception Exception
	 */
	public void analyze(boolean verbose) throws Exception {
		System.out.println("Begin BigSack analysis verbose:"+verbose);
		float totutil = 0;
		Datablock db = new Datablock(DBPhysicalConstants.DBLOCKSIZ);
		int numberBlks = 1;
		long minBlock = 0;
		int itab = 0;
		do {
			int tnumberBlks = 1;
			float ttotutil = 0;
			int zeroBlocks = 0;
			long fsiz = bTree.getIO().getIOManager().Fsize(itab)-DBPhysicalConstants.DBLOCKSIZ;
			minBlock =0;
			//long maxBlock = GlobalDBIO.makeVblock(itab, fsiz);
			
			System.out.println("<<BigSack Analysis|Tablespace number "+itab+" File size bytes: " + fsiz+">>");
			while (minBlock < fsiz) {
				bTree.getIO().getIOManager().FseekAndReadFully(GlobalDBIO.makeVblock(itab, minBlock), db);
				if( db.getBytesused() == 0 || db.getBytesinuse() == 0 ) {		
					++zeroBlocks;
				} else {
					//if(db.getData()[0] == 0xAC && db.getData()[1] == 0xED && db.getData()[2] == 0x00 && db.getData()[3] == 0x05) 
					//	System.out.println("Detected object stream header for tablespace"+itab+" pos:"+minBlock);
				}
				//if( Props.DEBUG ) System.out.println("Block:" + xblk + ": " + db.toBriefString());
				if( verbose ) {
						System.out.println("BigSack Block tablespace "+itab+". Current zero blocks:"+zeroBlocks+". Pos:"+minBlock+". Data:"+db.blockdump());
				}
				totutil += ((float) (db.getBytesinuse()));
				ttotutil += ((float) (db.getBytesinuse()));
				minBlock += DBPhysicalConstants.DBLOCKSIZ;
	
				++numberBlks;
				++tnumberBlks;
				//if( ((float)(tnumberBlks))/1000.0f == (float)(tnumberBlks/1000) ) System.out.print(tnumberBlks+"\r");
			}
			int ttTotal = DBPhysicalConstants.DBLOCKSIZ * tnumberBlks; // bytes total theoretical
			int taTotal = (int) ((ttotutil / (float)ttTotal) * 100.0); // ratio of total to used
			System.out.println("BigSack Tablespace "+itab+" utilization: " + (int)ttotutil + " bytes in "+tnumberBlks+" blocks");
			System.out.println("Maximum possible utilization is "+ttTotal+" bytes, making data Utilization "+taTotal+"%");
		} while( (minBlock = GlobalDBIO.nextTableSpace(itab++)) != 0);
		System.out.println("Total BigSack utilization: " + (int)totutil + " bytes in "+numberBlks+" blocks");
		int tTotal = DBPhysicalConstants.DBLOCKSIZ * numberBlks; // bytes total theoretical
		int aTotal = (int) ((totutil / (float)tTotal) * 100.0); // ratio of total to used
		System.out.println("Maximum possible utilization is "+tTotal+" bytes, making data Utilization "+aTotal+"%");
	}
	
	public boolean equals(Object o) {
		return( getTransactionId() == ((Long)o).longValue());
	}
}
