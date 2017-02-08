package asl.metadata;

import java.util.Hashtable;

public class StageData {
	private int stageID;
	private Hashtable<Integer, Blockette> blockettes;

	// Constructor(s)
	StageData(int stageID) {
		this.stageID = stageID;
		blockettes = new Hashtable<Integer, Blockette>();
	}

	// stageID
	public int getStageID() {
		return stageID;
	}

	// blockettes
	int addBlockette(Blockette blockette) {
		int blocketteNumber = blockette.getNumber();
		if (blockettes.containsKey(blocketteNumber)) {
			// MTH: Most of the old IU_ANMO.--.??? channels have duplicate stage
			// 3 blockette 054's --> IS THIS AN ERROR ?
			// System.out.format("**** addBlockette() BLOCKETTES already CONTAINS blockette Number %d\n",
			// blocketteNumber);
			// MTH: This throw is causing the reading of file zfoo (=rdseed -s)
			// to cease:
			// throw new DuplicateBlocketteException();
		}
		blockettes.put(blocketteNumber, blockette);
		return blocketteNumber;
	}

	public boolean hasBlockette(int blocketteNumber) {
		return blockettes.containsKey(blocketteNumber);
	}

	public Blockette getBlockette(int blocketteNumber) {
		return blockettes.get(blocketteNumber);
	}

	public Hashtable<Integer, Blockette> getBlockettes() {
		return blockettes;
	}
}
