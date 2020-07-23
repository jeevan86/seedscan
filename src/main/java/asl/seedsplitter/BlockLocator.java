package asl.seedsplitter;

import asl.seedscan.metrics.MetricException;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joel D. Edwards
 * <p>
 * The BlockLocator class takes a ArrayList of {@code ArrayList<DataSet>} objects and builds a list
 * of contiguous data segments which are common across all of the {@code ArrayList<DataSet>}
 * objects.
 * <p>
 * This class used to extend SwingWorker, but this aspect was never actually used. If it is
 * determined that we want to make this runnable, we will want to convert to either a ScanWorker or
 * a basic Runnable.
 */
public class BlockLocator {

  private static final Logger logger = LoggerFactory
      .getLogger(asl.seedsplitter.BlockLocator.class);

  /**
   * Searches for contiguous blocks of data across all of the supplied {@code ArrayList<DataSet>}
   * objects.
   *
   * @return A ArrayList of ContiguousBlock objects.
   */
  public static ArrayList<ContiguousBlock> buildBlockList(ArrayList<ArrayList<DataSet>> dataLists)
      throws MetricException {
    // The first ArrayList sets up the base ArrayList of ContiguousBlock
    // objects
    // Step through each of the remaining ArrayLists and build a new group
    // of
    // ContiguousBlock objects that contain a valid subset of the blocks
    // within the original ArrayList and the current data.

    ArrayList<ContiguousBlock> blockList = _buildFirstList(dataLists.get(0));
    for (ArrayList<DataSet> datalist : dataLists) {
        blockList = _buildDependentList(datalist, blockList);
    }

    return blockList;
  }

  /**
   * Generates the initial list of contiguous data regions.
   *
   * @param dataList A list of DataSet objects containing the data from a channel.
   * @return An ArrayList of ContiguousBlock objects.
   */
  private static ArrayList<ContiguousBlock> _buildFirstList(
      ArrayList<DataSet> dataList) {
    ArrayList<ContiguousBlock> resultList = new ArrayList<>();
    ContiguousBlock tempBlock;

    for (DataSet tempData : dataList) {
      tempBlock = new ContiguousBlock(tempData.getStartTime(),
          tempData.getEndTime(), tempData.getInterval());
      resultList.add(tempBlock);
    }

    return resultList;
  }

  /**
   * Updates the list of contiguous data blocks based on the data in an additional data list.
   *
   * @param dataList  A new group of data which will be used to update the list of contiguous data
   *                  blocks.
   * @param blockList The previous list of contiguous data blocks.
   * @return A new list of contiguous data blocks.
   * @throws MetricException If the sample rate of any of the DataSets does not match
   *                                        with those of the ContiguousBlocks.
   */
  private static ArrayList<ContiguousBlock> _buildDependentList(
      ArrayList<DataSet> dataList, ArrayList<ContiguousBlock> blockList)
      throws MetricException {
    ArrayList<ContiguousBlock> resultList = new ArrayList<>();
    DataSet tempData;
    ContiguousBlock oldBlock;
    ContiguousBlock newBlock;
    long startTime;
    long endTime;

    for (int dataIndex = 0, blockIndex = 0; (dataIndex < dataList.size())
        && (blockIndex < blockList.size()); ) {
      tempData = dataList.get(dataIndex);
      oldBlock = blockList.get(blockIndex);

      if (tempData.getInterval() != oldBlock.getInterval()) {
        throw new MetricException(String.format(
            "_buildDependentList: interval1=[%s] and/or interval2=[%s]",
            tempData.getInterval(), oldBlock.getInterval()));
      }

      if (tempData.getEndTime() <= oldBlock.getStartTime()) {
        dataIndex++;
      } else if (tempData.getStartTime() >= oldBlock.getEndTime()) {
        blockIndex++;
      } else {
        // Ensure the new block is a subset of the time within the old
        // block.
        startTime = Math.max(tempData.getStartTime(), oldBlock.getStartTime());
        if (tempData.getEndTime() > oldBlock.getEndTime()) {
          endTime = oldBlock.getEndTime();
          blockIndex++;
        } else {
          endTime = tempData.getEndTime();
          dataIndex++;
        }
        newBlock = new ContiguousBlock(startTime, endTime,
            tempData.getInterval());
        resultList.add(newBlock);
      }
    }

    return resultList;
  }
}
