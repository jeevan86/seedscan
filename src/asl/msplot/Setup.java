package asl.msplot;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class Setup
extends     JDialog
implements  ActionListener,
            FocusListener
{

    /**
     * 1 	Initial distribution
     */
    private static final long   serialVersionUID = 1L;

    private static boolean      bNetChange=false;
    private static boolean      bDisplayChange=false;
    private static boolean      bSmallChange=false;
    private static String       hostname;
    private static String       locChan1;
    private static String       locChan2;
    private static String       locChan3;
    private static int          port;
    private static int          secondsDuration;
    private static int          unitDivisor;
    private static int          minRange;

    private JButton             okButton;
    private JButton             cancelButton;
    private JFormattedTextField hostField;
    private JFormattedTextField portField;
    private JFormattedTextField minRangeField;
    private JFormattedTextField locChan1Field;
    private JFormattedTextField locChan2Field;
    private JFormattedTextField locChan3Field;
    private JFormattedTextField secondsField;
    private JRadioButton        unitHoursOption;
    private JRadioButton        unitSecondsOption;
    private JRadioButton        unitDaysOption;
    private JLabel              labelDuration;

    private String              saveFocusString;
    private JTextField          statusField;
    private MSPreferences         prefs;
    public static final int     DEFAULT_WIDTH  = 350;
    public static final int     DEFAULT_HEIGHT = 300;	

    public static final double  MAX_HOURS = 48.0;

    public Setup(JFrame owner,
            String oldHostname, int oldPort, int minRange,
            String oldLocChan1, String oldLocChan2, String oldLocChan3,
            int oldSecondsDuration, int oldUnitDivisor)
    {
        super(owner, "Setup Seismic Channel Plot", true);

        hostname = oldHostname;
        port = oldPort;
        locChan1 = oldLocChan1;
        locChan2 = oldLocChan2;
        locChan3 = oldLocChan3;
        secondsDuration = oldSecondsDuration;
        unitDivisor = oldUnitDivisor;
        bNetChange = false;
        bDisplayChange = false;
        bSmallChange = false;

        // ======== this ========
        prefs = new MSPreferences();
        Container contentPane = getContentPane();
        setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setMinimumSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setBounds(prefs.GetOriginX(), prefs.GetOriginY(), DEFAULT_WIDTH, DEFAULT_HEIGHT);

        JPanel panel = new JPanel();
        GridLayout layout = new GridLayout(7,2);
        panel.setLayout(layout);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        hostField = new JFormattedTextField();
        hostField.setValue(oldHostname);
        hostField.setColumns(10);
        hostField.setName("hostField");
        JLabel labelHost = new JLabel("Host: ",	JLabel.TRAILING);
        labelHost.setLabelFor(hostField);
        panel.add(labelHost);
        panel.add(hostField);
        hostField.addFocusListener(this);

        portField = new JFormattedTextField();
        portField.setValue(Integer.toString(oldPort, 10));
        portField.setColumns(5);
        portField.setName("portField");
        JLabel labelPort = new JLabel("Port: ",	JLabel.TRAILING);
        labelPort.setLabelFor(portField);
        panel.add(labelPort);
        panel.add(portField);
        portField.addFocusListener(this);

        locChan1Field = new JFormattedTextField();
        locChan1Field.setValue(oldLocChan1);
        locChan1Field.setColumns(10);
        locChan1Field.setName("locChan1Field");
        JLabel labelLocChan1 = new JLabel("Loc/Chan 1: ",	JLabel.TRAILING);
        labelLocChan1.setLabelFor(locChan1Field);
        panel.add(labelLocChan1);
        panel.add(locChan1Field);
        locChan1Field.addFocusListener(this);

        locChan2Field = new JFormattedTextField();
        locChan2Field.setValue(oldLocChan2);
        locChan2Field.setColumns(20);
        locChan2Field.setName("locChan2Field");
        JLabel labelLocChan2 = new JLabel("Loc/Chan 2: ",  JLabel.TRAILING);
        labelLocChan2.setLabelFor(locChan2Field);
        panel.add(labelLocChan2);
        panel.add(locChan2Field);
        locChan2Field.addFocusListener(this);

        locChan3Field = new JFormattedTextField();
        locChan3Field.setValue(oldLocChan3);
        locChan3Field.setColumns(30);
        locChan3Field.setName("locChan3Field");
        JLabel labelLocChan3 = new JLabel("Loc/Chan 3: ",  JLabel.TRAILING);
        labelLocChan3.setLabelFor(locChan3Field);
        panel.add(labelLocChan3);
        panel.add(locChan3Field);
        locChan3Field.addFocusListener(this);

        minRangeField = new JFormattedTextField();
        minRangeField.setValue(Integer.toString(minRange, 10));
        minRangeField.setColumns(10);
        minRangeField.setName("minRangeField");
        JLabel labelMinRange = new JLabel("Min Range: ", JLabel.TRAILING);
        labelMinRange.setLabelFor(minRangeField);
        panel.add(labelMinRange);
        panel.add(minRangeField);
        minRangeField.addFocusListener(this);

        secondsField = new JFormattedTextField();
        secondsField.setValue(Integer.toString(secondsDuration/unitDivisor, 10));
        secondsField.setColumns(10);
        secondsField.setName("secondsField");
        if (unitDivisor == 1)
            labelDuration = new JLabel("Seconds: ",  JLabel.TRAILING);
        else if (unitDivisor == 3600)
            labelDuration = new JLabel("Hours: ", JLabel.TRAILING);
        else
            labelDuration = new JLabel("Days: ", JLabel.TRAILING);
        labelDuration.setLabelFor(secondsField);
        panel.add(labelDuration);
        panel.add(secondsField);
        secondsField.addFocusListener(this);

        add(panel);


        unitHoursOption = new JRadioButton("Hours");
        unitSecondsOption = new JRadioButton("Seconds");
        unitDaysOption = new JRadioButton("Days");
        unitHoursOption.setSelected(unitDivisor==3600);
        unitSecondsOption.setSelected(unitDivisor==1);
        unitDaysOption.setSelected(unitDivisor==3600*24);
        unitHoursOption.addActionListener(this);
        unitSecondsOption.addActionListener(this);
        unitDaysOption.addActionListener(this);
        JPanel panelTime = new JPanel(new GridLayout(1,3));
        panelTime.add(unitDaysOption);
        panelTime.add(unitHoursOption);
        panelTime.add(unitSecondsOption);
        panelTime.setBorder(new EmptyBorder(0, 10, 10, 10));
        add(panelTime);

        JPanel midPanel = new JPanel();
        midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.X_AXIS));
        okButton = new JButton("OK");
        okButton.addActionListener(this);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        midPanel.add(okButton);
        midPanel.add(Box.createHorizontalGlue());
        midPanel.add(cancelButton);
        midPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
        add(midPanel);

        // Status field
        statusField = new JTextField();
        statusField.setEditable(false);
        statusField.setText("Enter changes");
        statusField.setMinimumSize(new Dimension(DEFAULT_WIDTH, 1));
        statusField.setPreferredSize(new Dimension(DEFAULT_WIDTH, 1));
        add(statusField);
    } // Setup() constructor

    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();
        Object source = e.getSource();
        if (source == okButton)
        {	
            if (locChan1Field.getText().compareToIgnoreCase(locChan1) != 0)
            {
                bDisplayChange = true;
                locChan1 = locChan1Field.getText();
            }
            if (locChan2Field.getText().compareToIgnoreCase(locChan2) != 0)
            {
                bDisplayChange = true;
                locChan2 = locChan2Field.getText();
            }
            if (locChan3Field.getText().compareToIgnoreCase(locChan3) != 0)
            {
                bDisplayChange = true;
                locChan3 = locChan3Field.getText();
            }
            if (hostField.getText().compareTo(hostname) != 0)
            {
                bNetChange = true;
                hostname = hostField.getText();
            }
            if (port != Integer.parseInt(portField.getText()))
            {
                bNetChange = true;
                port = Integer.parseInt(portField.getText());
            }
            if (unitHoursOption.isSelected() && unitDivisor != 3600)
            {
                bSmallChange = true;
                unitDivisor = 3600;
            }
            if (unitDaysOption.isSelected() && unitDivisor != 3600*24)
            {
                bSmallChange = true;
                unitDivisor = 3600*24;
            }
            if (unitSecondsOption.isSelected() && unitDivisor != 1)
            {
                bSmallChange = true;
                unitDivisor = 1;
            }
            if (secondsDuration != Integer.parseInt(secondsField.getText()) * unitDivisor)
            {
                bSmallChange = true;
                secondsDuration = Integer.parseInt(secondsField.getText()) * unitDivisor;
            }
            if (minRange != Integer.parseInt(minRangeField.getText()))
            {
                bSmallChange = true;
                minRange = Integer.parseInt(minRangeField.getText());
            }
            setVisible(false);
        } // okButton was pressed
        else if (source == cancelButton)
        {
            bNetChange = false;
            bDisplayChange = false;
            bSmallChange = false;
            setVisible(false);
        }
        else if (source == unitHoursOption)
        {
            int unit = 3600;
            if (Integer.parseInt(secondsField.getText()) > 2000000000/unit)
            {
                secondsField.setText(Integer.toString(2000000000/unit));
                statusField.setText("Reset duration to maximum value of" + 2000000000/unit + " hours\n");
                Toolkit.getDefaultToolkit().beep();
            }
            labelDuration.setText("Hours: ");
            unitSecondsOption.setSelected(false);
            unitDaysOption.setSelected(false);
        }
        else if (source == unitSecondsOption)
        {
            int unit = 1;
            if (Integer.parseInt(secondsField.getText()) > 2000000000/unit)
            {
                secondsField.setText(Integer.toString(2000000000/unit));
                statusField.setText("Reset duration to maximum value of" + 2000000000/unit + " seconds\n");
                Toolkit.getDefaultToolkit().beep();
            }
            labelDuration.setText("Seconds: ");
            unitHoursOption.setSelected(false);
            unitDaysOption.setSelected(false);
        }
        else if (source == unitDaysOption)
        {
            int unit = 3600*24;
            if (Integer.parseInt(secondsField.getText()) > 2000000000/unit)
            {
                secondsField.setText(Integer.toString(2000000000/unit));
                statusField.setText("Reset duration to maximum value of" + 2000000000/unit + " days\n");
                Toolkit.getDefaultToolkit().beep();
            }
            labelDuration.setText("Days: ");
            unitHoursOption.setSelected(false);
            unitSecondsOption.setSelected(false);
        } else
        {
            System.err.println("Unmanaged setup actionPerformed " + command);
        }
    } // actionPerformed()

    public void focusGained(FocusEvent e)
    {
        JFormattedTextField field = (JFormattedTextField) e.getComponent();

        // Save the current field value in case the user botches up the edit.
        // This allows us to restore the prior value upon field exit
        saveFocusString = field.getText();
    } // focusGained

    public void focusLost(FocusEvent e)
    {
        Object source = e.getSource();
        if (source == portField)
        {
            try
            {
                if (Integer.parseInt(portField.getText()) < 1)
                {
                    portField.setText("1");
                    statusField.setText("Reset port to minimum value of 1\n");
                    Toolkit.getDefaultToolkit().beep();
                }

                if (Integer.parseInt(portField.getText()) > 65535)
                {
                    portField.setText(Integer.toString(65535));
                    statusField.setText("Reset port to maximum value of 65535\n");
                    Toolkit.getDefaultToolkit().beep();
                }
            } catch (NumberFormatException e1)
            {
                statusField.setText("Non integer '" + portField.getText()
                        + "' in port field, restoring former value\n");
                portField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }
        } // if portField

        else if (source == secondsField)
        {
            try
            {
                if (Integer.parseInt(secondsField.getText()) < 1)
                {
                    secondsField.setText("1");
                    statusField.setText("Reset duration to minimum value of 1\n");
                    Toolkit.getDefaultToolkit().beep();
                }

                if (Integer.parseInt(secondsField.getText()) > 2000000000/unitDivisor)
                {
                    secondsField.setText(Integer.toString(2000000000/unitDivisor));
                    statusField.setText("Reset duration to maximum value of" + 2000000000/unitDivisor + "\n");
                    Toolkit.getDefaultToolkit().beep();
                }
            } catch (NumberFormatException e1)
            {
                statusField.setText("Non integer '" + secondsField.getText()
                        + "' in duration field, restoring former value\n");
                secondsField.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }
        } // if secondsField

        else if (source == locChan1Field)
        {
            if (locChan1Field.getText().length() != 0 &&
                    locChan1Field.getText().length() != 3 &&
                    locChan1Field.getText().length() != 4 &&
                    locChan1Field.getText().length() != 6)
            {
                statusField.setText("Loc/Chan 1 name '" + locChan1Field.getText()
                        + "' doesn't match formats <blank>, ccc, /ccc or ll/ccc, restoring prior name.\n");
                locChan1Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

            if (locChan1Field.getText().length() == 4
                    && locChan1Field.getText().charAt(0) != '/')
            {
                statusField.setText("Expected /ccc format, got '" + locChan1Field.getText()
                        + "', restoring prior name.\n");
                locChan1Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

            if (locChan1Field.getText().length() == 6
                    && locChan1Field.getText().charAt(2) != '/')
            {
                statusField.setText("Expected ll/ccc format, got '" + locChan1Field.getText()
                        + "', restoring prior name.\n");
                locChan1Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

            if (locChan1Field.getText().contains("*") || locChan1Field.getText().contains("?"))
            {
                statusField.setText("No channel name wildcards '" + locChan1Field.getText()
                        + "', restoring prior name.\n");
                locChan1Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

        } // if locChan1Field

        else if (source == locChan2Field)
        {
            if (locChan2Field.getText().length() != 0 &&
                    locChan2Field.getText().length() != 3 &&
                    locChan2Field.getText().length() != 4 &&
                    locChan2Field.getText().length() != 6)
            {
                statusField.setText("Loc/Chan 2 name '" + locChan2Field.getText()
                        + "' doesn't match formats <blank>, ccc, /ccc or ll/ccc, restoring prior name.\n");
                locChan2Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

            if (locChan2Field.getText().length() == 4
                    && locChan2Field.getText().charAt(0) != '/')
            {
                statusField.setText("Expected /ccc format, got '" + locChan2Field.getText()
                        + "', restoring prior name.\n");
                locChan2Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

            if (locChan2Field.getText().length() == 6
                    && locChan2Field.getText().charAt(2) != '/')
            {
                statusField.setText("Expected ll/ccc format, got '" + locChan2Field.getText()
                        + "', restoring prior name.\n");
                locChan2Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

            if (locChan2Field.getText().contains("*") || locChan2Field.getText().contains("?"))
            {
                statusField.setText("No channel name wildcards '" + locChan2Field.getText()
                        + "', restoring prior name.\n");
                locChan2Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

        } // if locChan2Field

        else if (source == locChan3Field)
        {
            if (locChan3Field.getText().length() != 0 &&
                    locChan3Field.getText().length() != 3 &&
                    locChan3Field.getText().length() != 4 &&
                    locChan3Field.getText().length() != 6)
            {
                statusField.setText("Loc/Chan 3 name '" + locChan3Field.getText()
                        + "' doesn't match formats <blank>, ccc, /ccc or ll/ccc, restoring prior name.\n");
                locChan3Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

            if (locChan3Field.getText().length() == 4
                    && locChan3Field.getText().charAt(0) != '/')
            {
                statusField.setText("Expected /ccc format, got '" + locChan3Field.getText()
                        + "', restoring prior name.\n");
                locChan3Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

            if (locChan3Field.getText().length() == 6
                    && locChan3Field.getText().charAt(2) != '/')
            {
                statusField.setText("Expected ll/ccc format, got '" + locChan3Field.getText()
                        + "', restoring prior name.\n");
                locChan3Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

            if (locChan3Field.getText().contains("*") || locChan3Field.getText().contains("?"))
            {
                statusField.setText("No channel name wildcards '" + locChan3Field.getText()
                        + "', restoring prior name.\n");
                locChan3Field.setText(saveFocusString);
                Toolkit.getDefaultToolkit().beep();
            }

        } // if locChan3Field

    } // focusLost()

    public boolean GetNetChange()
    {
        return bNetChange;
    }

    public boolean GetDisplayChange()
    {
        return bDisplayChange;
    }

    public boolean GetSmallChange()
    {
        return bSmallChange;
    }

    public String GetHostname()
    {
        return hostname;
    }

    public String GetLocChan1()
    {
        return locChan1;
    }

    public String GetLocChan2()
    {
        return locChan2;
    }

    public String GetLocChan3()
    {
        return locChan3;
    }

    public int GetPort()
    {
        return port;
    }

    public int GetMinRange()
    {
        return minRange;
    }

    public int GetSecondsDuration()
    {
        return secondsDuration;
    }

    public int GetUnitDivisor()
    {
        return unitDivisor;
    }

} // class Setup
