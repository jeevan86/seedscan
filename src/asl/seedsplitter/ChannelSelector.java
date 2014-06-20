/*
 * Copyright 2012, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */

package asl.seedsplitter;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Frank Shelly <fshelly@usgs.gov>
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 * 
 *         A graphical interface for acquiring a data set based on a supplied
 *         list of files and filters.
 */
public class ChannelSelector extends JDialog implements ActionListener,
		PropertyChangeListener, ListSelectionListener, ListDataListener {
	/**
	 * 1 Initial distribution
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory
			.getLogger(asl.seedsplitter.ChannelSelector.class);
	private static final Formatter formatter = new Formatter();

	private JButton addFilesButton;
	private JButton removeSelectedButton;

	private JList fileList;

	private JLabel filterNetworkLabel;
	private JTextField filterNetworkField;
	private JLabel filterStationLabel;
	private JTextField filterStationField;
	private JLabel filterLocationLabel;
	private JTextField filterLocationField;
	private JLabel filterChannelLabel;
	private JTextField filterChannelField;

	private JButton readFilesButton;
	private JButton cancelReadButton;

	private JLabel[] channelLabels;
	private JComboBox[] channelCombos;

	private JButton okButton;
	private JButton cancelButton;

	private JFileChooser fileChooser;

	private JProgressBar processProgress;
	private JTextField statusField;

	private String defaultDir;

	private boolean bProcessOK = false;
	private boolean bCancel = false;

	private boolean reading = false;
	private boolean locating = false;

	private DefaultListModel filesModel = null;
	private DefaultComboBoxModel[] channelModels = null;

	private int channelCount = 1;

	SeedSplitter splitter = null;
	Hashtable<String, ArrayList<DataSet>> allDataSets = new Hashtable<String, ArrayList<DataSet>>();
	ArrayList<ArrayList<DataSet>> dataSets = null;
	ArrayList<ContiguousBlock> blocks = null;
	BlockLocator locator = null;

	public static final int MINIMUM_WIDTH = 450;
	public static final int MINIMUM_HEIGHT = 550;

	public static final int DEFAULT_WIDTH = 650;
	public static final int DEFAULT_HEIGHT = 550;

	/**
	 * Constructor.
	 * 
	 * @param owner
	 *            The parent container.
	 * @param xStart
	 *            The initial x coordinate.
	 * @param yStart
	 *            The initial y coordinate.
	 * @param defaultDirectory
	 *            The default directory in which to open the file dialog.
	 * @param channelCount
	 *            The number of channels
	 */
	public ChannelSelector(JFrame owner, int xStart, int yStart,
			String defaultDirectory, int channelCount) {
		super(owner, "Collect Azimuth Files", true);
		defaultDir = defaultDirectory;
		bCancel = false;
		bProcessOK = false;
		splitter = null;
		dataSets = null;
		blocks = null;
		locator = null;
		this.channelCount = channelCount;

		// ======== this ========

		setMinimumSize(new Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT));
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		setBounds(xStart, yStart, DEFAULT_WIDTH, DEFAULT_HEIGHT);
		Container contentPane = getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		// Controls for adding or removing files from the list
		JPanel filesPanel = new JPanel(new BorderLayout());
		filesPanel.setBorder(new EmptyBorder(5, 5, 0, 5));
		addFilesButton = new JButton("Add Files");
		addFilesButton.addActionListener(this);
		filesPanel.add(addFilesButton, BorderLayout.WEST);
		removeSelectedButton = new JButton("Remove Selected");
		removeSelectedButton.addActionListener(this);
		filesPanel.add(removeSelectedButton, BorderLayout.EAST);
		add(filesPanel);

		// Scrollable list of files
		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.setBorder(new EmptyBorder(5, 5, 0, 5));
		filesModel = new DefaultListModel();
		fileList = new JList(filesModel);
		fileList.addListSelectionListener(this);
		fileList.setVisibleRowCount(100);
		JScrollPane scrollPane = new JScrollPane(fileList);
		listPanel.add(scrollPane, BorderLayout.CENTER);
		add(listPanel);

		// Filters
		JPanel filterPad = new JPanel(new BorderLayout());
		filterPad.setBorder(new EmptyBorder(5, 3, 0, 3));
		GridBagLayout filterGrid = new GridBagLayout();
		JPanel filterPanel = new JPanel(filterGrid);
		filterPanel.setBorder(new TitledBorder(new EtchedBorder(), "Filters"));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.insets.top = 0;
		c.insets.left = 5;
		c.insets.bottom = 5;
		c.insets.right = 5;

		// create
		filterNetworkField = new JTextField();
		filterNetworkField.setColumns(6);
		filterNetworkField.setEditable(true);
		filterNetworkField.setSize(DEFAULT_WIDTH, 1);
		filterNetworkLabel = new JLabel("Network: ", JLabel.TRAILING);
		filterNetworkLabel.setLabelFor(filterNetworkField);
		// add to grid
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		filterGrid.setConstraints(filterNetworkLabel, c);
		filterPanel.add(filterNetworkLabel);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 50.0;
		filterGrid.setConstraints(filterNetworkField, c);
		filterPanel.add(filterNetworkField);

		// create
		filterStationField = new JTextField();
		filterStationField.setColumns(6);
		filterStationField.setEditable(true);
		filterStationField.setSize(DEFAULT_WIDTH, 1);
		filterStationLabel = new JLabel("Station: ", JLabel.TRAILING);
		filterStationLabel.setLabelFor(filterStationField);
		// add to grid
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1.0;
		filterGrid.setConstraints(filterStationLabel, c);
		filterPanel.add(filterStationLabel);
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 50.0;
		filterGrid.setConstraints(filterStationField, c);
		filterPanel.add(filterStationField);

		// create
		filterLocationField = new JTextField();
		filterLocationField.setColumns(6);
		filterLocationField.setEditable(true);
		filterLocationField.setSize(DEFAULT_WIDTH, 1);
		filterLocationLabel = new JLabel("Location: ", JLabel.TRAILING);
		filterLocationLabel.setLabelFor(filterLocationField);
		// add to grid
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1.0;
		filterGrid.setConstraints(filterLocationLabel, c);
		filterPanel.add(filterLocationLabel);
		c.gridx = 1;
		c.gridy = 2;
		c.weightx = 50.0;
		filterGrid.setConstraints(filterLocationField, c);
		filterPanel.add(filterLocationField);

		c.insets.top = 0;
		c.insets.bottom = 8;

		// create
		filterChannelField = new JTextField();
		filterChannelField.setColumns(6);
		filterChannelField.setEditable(true);
		filterChannelField.setSize(DEFAULT_WIDTH, 1);
		filterChannelLabel = new JLabel("Channel: ", JLabel.TRAILING);
		filterChannelLabel.setLabelFor(filterChannelField);
		// add to grid
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 1.0;
		filterGrid.setConstraints(filterChannelLabel, c);
		filterPanel.add(filterChannelLabel);
		c.gridx = 1;
		c.gridy = 3;
		c.weightx = 50.0;
		// c.gridwidth = GridBagConstraints.REMAINDER;
		filterGrid.setConstraints(filterChannelField, c);
		filterPanel.add(filterChannelField);

		filterPad.add(filterPanel, BorderLayout.CENTER);
		add(filterPad);

		// Read Area (control and status)
		JPanel readPanel = new JPanel(new BorderLayout());
		readPanel.setBorder(new EmptyBorder(10, 5, 5, 5));

		readFilesButton = new JButton("Read Files");
		readFilesButton.addActionListener(this);
		readPanel.add(readFilesButton, BorderLayout.WEST);

		cancelReadButton = new JButton("Cancel Read");
		cancelReadButton.addActionListener(this);
		readPanel.add(cancelReadButton, BorderLayout.EAST);

		add(readPanel);

		// Progress Indicator
		JPanel progressPanel = new JPanel();
		progressPanel.setLayout(new BorderLayout());
		progressPanel.setBorder(new EmptyBorder(5, 5, 10, 5));
		processProgress = new JProgressBar(SwingConstants.HORIZONTAL);
		progressPanel.add(processProgress, BorderLayout.NORTH);

		add(progressPanel);

		// Channel selection area
		JPanel channelPad = new JPanel(new BorderLayout());
		channelPad.setBorder(new EmptyBorder(5, 3, 0, 3));
		GridBagLayout channelGrid = new GridBagLayout();
		JPanel channelPanel = new JPanel(channelGrid);
		channelPanel
				.setBorder(new TitledBorder(new EtchedBorder(), "Channels"));

		channelModels = new DefaultComboBoxModel[channelCount];
		channelLabels = new JLabel[channelCount];
		channelCombos = new JComboBox[channelCount];

		c.insets.top = 0;
		c.insets.left = 5;
		c.insets.bottom = 5;
		c.insets.right = 5;
		for (int i = 0; i < channelCount; i++) {
			if (i == (channelCount - 1)) {
				c.insets.top = 0;
				c.insets.bottom = 8;
			}
			channelModels[i] = new DefaultComboBoxModel();
			channelLabels[i] = new JLabel("Channel " + i + ": ");
			channelCombos[i] = new JComboBox(channelModels[i]);
			channelLabels[i].setLabelFor(channelCombos[i]);
			c.gridx = 0;
			c.gridy = i;
			c.gridwidth = 1;
			c.weightx = 1.0;
			c.fill = GridBagConstraints.NONE;
			channelGrid.setConstraints(channelLabels[i], c);
			channelPanel.add(channelLabels[i]);
			c.gridx = 1;
			c.gridy = i;
			c.gridwidth = 1;
			c.weightx = 50.0;
			c.fill = GridBagConstraints.HORIZONTAL;
			channelGrid.setConstraints(channelCombos[i], c);
			channelPanel.add(channelCombos[i]);
		}

		channelPad.add(channelPanel, BorderLayout.CENTER);
		add(channelPad);

		// Ok and Cancel buttons area
		JPanel controlPanel = new JPanel(new BorderLayout());
		controlPanel.setBorder(new EmptyBorder(5, 5, 10, 5));
		okButton = new JButton("OK");
		okButton.addActionListener(this);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		controlPanel.add(okButton, BorderLayout.WEST);
		controlPanel.add(cancelButton, BorderLayout.EAST);
		add(controlPanel);

		// Status field
		statusField = new JTextField();
		statusField.setEditable(false);
		statusField.setText("Select Azimuth seed files");
		statusField.setSize(DEFAULT_WIDTH, 1);
		add(statusField);

		// Widget Configuration
		removeSelectedButton.setEnabled(false);
		readFilesButton.setEnabled(false);
		cancelReadButton.setEnabled(false);
		filterChannelField.setText("LH*");
		for (int i = 0; i < channelCount; i++) {
			channelCombos[i].setEnabled(false);
			channelCombos[i].addActionListener(this);
		}
		okButton.setEnabled(false);
		filesModel.addListDataListener(this);

		updateGui();

	} // AzFileSelect() constructor

	// Customization
	public void setWindowIcon(Image icon) {
		setIconImage(icon);
	}

	public void setAddFilesButtonIcon(ImageIcon icon) {
		addFilesButton.setIcon(icon);
	}

	public void setRemoveSelectedButtonIcon(ImageIcon icon) {
		removeSelectedButton.setIcon(icon);
	}

	public void setReadFilesButtonIcon(ImageIcon icon) {
		readFilesButton.setIcon(icon);
	}

	public void setCancelReadButtonIcon(ImageIcon icon) {
		cancelReadButton.setIcon(icon);
	}

	public void setOkButtonIcon(ImageIcon icon) {
		okButton.setIcon(icon);
	}

	public void setCancelButtonIcon(ImageIcon icon) {
		cancelButton.setIcon(icon);
	}

	public void setChannelLabel(int index, String label)
			throws ArrayIndexOutOfBoundsException {
		channelLabels[index].setText(label);
	}

	// Event Handling
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			processProgress.setValue((Integer) evt.getNewValue());
			if (splitter.isDone()) {
				allDataSets = splitter.getTable();

				if (splitter.getFinalProgress().errorOccurred()) {
					statusField.setText(splitter.getFinalProgress()
							.getErrorMessage());
					processProgress.setValue(0);
					reading = false;
					updateGui();
				} else {
					// Now find the data segments that match up between all
					// three files
					for (int i = 0; i < channelCount; i++) {
						channelModels[i].removeAllElements();
						channelModels[i].addElement("");
					}

					Object[] channels = allDataSets.keySet().toArray();
					Arrays.sort(channels);
					logger.debug("CHANNEL COUNT: "
							+ allDataSets.keySet().size());
					for (Object channel : channels) {
						for (int i = 0; i < channelCount; i++) {
							channelModels[i].addElement((String) channel);
						}
					}
				}
				reading = false;
				updateGui();
			}
			if (locator != null && locator.isDone()) {
				if (locator.getFinalProgress().errorOccurred()) {
					statusField.setText(locator.getFinalProgress()
							.getErrorMessage());
					processProgress.setValue(0);
					locator = null;
					splitter = null;
					locating = false;
					updateGui();
				} else {
					// add DataSets to return list
					blocks = locator.getBlocks();
					// So we can visually inspect the ContiguousBlock time
					// ranges
					logger.debug("ContiguousBlocks:");
					for (ContiguousBlock block : blocks) {
						logger.debug(formatter
								.format("  ContiguousBlock : %s ---> %s\n",
										Sequence.timestampToString(block
												.getStartTime()),
										Sequence.timestampToString(block
												.getEndTime())).toString());
					}
					bProcessOK = true;
					locator = null;
					locating = false;
					updateGui();
					setVisible(false);
				}
			}
		} // "progress" event

	} // propertyChange()

	public void actionPerformed(ActionEvent evt) {
		Object source = evt.getSource();
		String command = evt.getActionCommand();

		if (source == readFilesButton) {
			allDataSets = null;
			dataSets = null;
			blocks = null;
			// Lets try to garbage collect here. The user is already expecting
			// some delay, and we just released a lot of data.
			Runtime.getRuntime().gc();
			reading = true;
			updateGui();
			blocks = null;
			bProcessOK = false;
			Object[] items = filesModel.toArray();
			File[] files = new File[items.length];
			for (int i = 0; i < items.length; i++) {
				files[i] = (File) (items[i]);
			}
			splitter = new SeedSplitter(files);
			if (filterNetworkField.getText().length() > 0) {
				try {
					splitter.setFilter(filterNetworkField.getText(),
							SeedSplitter.NETWORK);
				} catch (InvalidFilterException e) {
					statusField.setText("Invalid network filter!");
					logger.error("InvalidFilterException:", e);
					reading = false;
					updateGui();
					return;
				}
			}
			if (filterStationField.getText().length() > 0) {
				try {
					splitter.setFilter(filterStationField.getText(),
							SeedSplitter.STATION);
				} catch (InvalidFilterException e) {
					statusField.setText("Invalid station filter!");
					logger.error("InvalidFilterException:", e);
					reading = false;
					updateGui();
					return;
				}
			}
			if (filterLocationField.getText().length() > 0) {
				try {
					splitter.setFilter(filterLocationField.getText(),
							SeedSplitter.LOCATION);
				} catch (InvalidFilterException e) {
					statusField.setText("Invalid location filter!");
					logger.error("InvalidFilterException:", e);
					reading = false;
					updateGui();
					return;
				}
			}
			if (filterChannelField.getText().length() > 0) {
				try {
					splitter.setFilter(filterChannelField.getText(),
							SeedSplitter.CHANNEL);
				} catch (InvalidFilterException e) {
					statusField.setText("Invalid channel filter!");
					logger.error("InvalidFilterException:", e);
					reading = false;
					updateGui();
					return;
				}
			}
			splitter.addPropertyChangeListener(this);
			splitter.execute();
		} // readFilesButton was pressed
		else if (source == okButton) {
			// Find the data segments that match up between all three files
			locating = true;
			updateGui();
			blocks = null;
			dataSets = new ArrayList<ArrayList<DataSet>>(3);

			for (int i = 0; i < channelCount; i++) {
				int channelIndex = channelCombos[i].getSelectedIndex();
				if (channelIndex > 0) {
					String channelKey = (String) channelModels[i]
							.getElementAt(channelIndex);
					dataSets.add(allDataSets.get(channelKey));
				}
			}

			// So we can visually inspect gaps in channel data
			logger.debug("DataSets:");
			for (ArrayList<DataSet> list : dataSets) {
				for (DataSet set : list) {
					logger.debug(formatter.format(
							"  DataSet %s_%s %s-%s: %s ---> %s\n",
							set.getNetwork(), set.getStation(),
							set.getLocation(), set.getChannel(),
							Sequence.timestampToString(set.getStartTime()),
							Sequence.timestampToString(set.getEndTime()))
							.toString());
				}
				logger.debug("========================================");
			}

			locator = new BlockLocator(dataSets);
			locator.addPropertyChangeListener(this);
			locator.execute();
		} // okButton was pressed
		else if (source == cancelReadButton) {
			if (splitter != null) {
				splitter.cancel(true);
			}
			reading = false;
			updateGui();
		} else if (source == cancelButton) {
			if (locator != null) {
				locator.cancel(true);
			}
			bCancel = true;
			setVisible(false);
		} // cancelButton was pressed
		else if (source == addFilesButton) {
			fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Select seed files");
			fileChooser.setCurrentDirectory(new File(defaultDir));
			fileChooser.setMultiSelectionEnabled(true);
			int returnVal = fileChooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				defaultDir = fileChooser.getCurrentDirectory()
						.getAbsolutePath();
				TreeSet<File> set = new TreeSet<File>();
				Object[] items = null;
				File[] files = null;
				items = filesModel.toArray();
				for (Object item : items) {
					set.add((File) item);
				}
				files = fileChooser.getSelectedFiles();
				for (File file : files) {
					set.add(file);
				}
				items = set.toArray();
				Arrays.sort(items);
				filesModel.clear();
				for (Object file : items) {
					filesModel.addElement((File) file);
				}
			}
		} // addFileButton was pressed
		else if (source == removeSelectedButton) {
			int[] indices = fileList.getSelectedIndices();
			Arrays.sort(indices);
			for (int i = (indices.length - 1); i >= 0; i--) {
				filesModel.removeElementAt(indices[i]);
			}
			fileList.clearSelection();
		} // removeSelectedButton was pressed
		else {
			boolean found = false;
			for (int i = 0; i < channelCount; i++) {
				if (source == channelCombos[i]) {
					found = true;
					updateGui();
				}
			}
			if (!found) {
				logger.warn("Unmanaged setup actionPerformed " + command);
			}
		}
	} // actionPerformed()

	// From Interface ListSelectionListener
	public void valueChanged(ListSelectionEvent e) {
		updateGui();
	}

	// From Interface ListDataListener
	public void contentsChanged(ListDataEvent e) {
		updateGui();
	}

	public void intervalAdded(ListDataEvent e) {
		updateGui();
	}

	public void intervalRemoved(ListDataEvent e) {
		updateGui();
	}

	/**
	 * Updates the user interface based on changes to the GUI's state.
	 */
	private void updateGui() {
		if (reading) {
			addFilesButton.setEnabled(false);
			readFilesButton.setEnabled(false);
			removeSelectedButton.setEnabled(false);
			okButton.setEnabled(false);
			cancelButton.setEnabled(false);
			cancelReadButton.setEnabled(true);
		} else if (locating) {
			addFilesButton.setEnabled(false);
			readFilesButton.setEnabled(false);
			removeSelectedButton.setEnabled(false);
			okButton.setEnabled(false);
			cancelButton.setEnabled(true);
			cancelReadButton.setEnabled(false);
		} else {
			processProgress.setValue(0);
			addFilesButton.setEnabled(true);
			cancelReadButton.setEnabled(false);
			cancelButton.setEnabled(true);
			readFilesButton.setEnabled((filesModel.getSize() > 0) ? true
					: false);
			removeSelectedButton
					.setEnabled((fileList.getSelectedIndex() > -1) ? true
							: false);
			boolean okEnabled = true;
			for (int i = 0; i < channelCount; i++) {
				channelCombos[i]
						.setEnabled((channelModels[i].getSize() > 0) ? true
								: false);
				if (channelCombos[i].getSelectedIndex() < 1) {
					okEnabled = false;
				}
			}
			okButton.setEnabled(okEnabled);
		}
	}

	public boolean GetProcessed() {
		return bProcessOK;
	}

	public boolean GetCancel() {
		return bCancel;
	}

	public String GetDefaultDirectory() {
		return defaultDir;
	}

	public ArrayList<ArrayList<DataSet>> GetDataSets() {
		return dataSets;
	}

	public ArrayList<ContiguousBlock> GetBlocks() {
		return blocks;
	}
} // class AzFileSelect
