/**
 * 
 */
package asl.ofcweb;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * @author fshelly
 *
 */
public class DateSelect
    extends     JDialog
    implements  ActionListener,
                FocusListener
{
    /**
     * Date			Ver	Comments
     * ======== === ============================================================
     * 24Jun10	1		Creation
     */
    private static final long serialVersionUID = 1L;

    private	Date startDate;
    private	Date endDate;
    private	Date fileStartDate;
    private Date fileEndDate;
    private	int  daysBack;

    public boolean bCancel=true;

    private JFormattedTextField startDateField;
    private JFormattedTextField finishDateField;
    private JFormattedTextField fileStartDateField;
    private JFormattedTextField fileFinishDateField;	
    private JFormattedTextField daysBackField;	
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField statusField;

    private Calendar gDate;
    private String saveFocusString;

    public static final int DEFAULT_WIDTH  = 350;
    public static final int DEFAULT_HEIGHT = 250;	

    public DateSelect(
            Frame owner,
            Date fileStartDate, 
            Date fileEndDate,
            Date oldStartDate, 
            Date oldEndDate,
            int daysBack)
    {
        super(owner, "Select chart date range", true);

        // Truncate dates to just year,doy

        Calendar gDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Calendar gYearDoy = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        gDate.setTime(fileStartDate);
        gYearDoy.clear();
        gYearDoy.set(Calendar.YEAR, 
                gDate.get(Calendar.YEAR));
        gYearDoy.set(Calendar.DAY_OF_YEAR, 
                gDate.get(Calendar.DAY_OF_YEAR));		
        this.fileStartDate = new Date(gYearDoy.getTimeInMillis());

        gDate.setTime(fileEndDate);
        gYearDoy.set(Calendar.YEAR, 
                gDate.get(Calendar.YEAR));
        gYearDoy.set(Calendar.DAY_OF_YEAR, 
                gDate.get(Calendar.DAY_OF_YEAR));		
        this.fileEndDate = new Date(gYearDoy.getTimeInMillis());

        gDate.setTime(oldStartDate);
        gYearDoy.set(Calendar.YEAR, 
                gDate.get(Calendar.YEAR));
        gYearDoy.set(Calendar.DAY_OF_YEAR, 
                gDate.get(Calendar.DAY_OF_YEAR));		
        startDate = new Date(gYearDoy.getTimeInMillis());

        gDate.setTime(oldEndDate);
        gYearDoy.set(Calendar.YEAR, 
                gDate.get(Calendar.YEAR));
        gYearDoy.set(Calendar.DAY_OF_YEAR, 
                gDate.get(Calendar.DAY_OF_YEAR));		
        endDate = new Date(gYearDoy.getTimeInMillis());

        this.daysBack = daysBack;
        System.err.printf("DEBUG %s %s \n%s %s %d\n", 
                this.fileStartDate.toString(),
                this.fileEndDate.toString(),
                startDate.toString(),
                endDate.toString(),
                daysBack);		

        // ======== this ========
        setLayout(new BorderLayout());
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        JPanel panel = new JPanel();
        GridLayout layout = new GridLayout(7,3);
        panel.setLayout(layout);
        panel.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        gDate.setTime(startDate);
        startDateField = new JFormattedTextField();
        startDateField.setValue(String.format("%04d,%03d",
                    gDate.get(Calendar.YEAR), gDate.get(Calendar.DAY_OF_YEAR)));
        startDateField.setColumns(8);
        startDateField.setName("startDateField");
        startDateField.addFocusListener(this);
        //		startDateField.setBorder(new EmptyBorder(5, 5, 5, 5));

        gDate.setTime(endDate);
        finishDateField = new JFormattedTextField();
        finishDateField.setValue(String.format("%04d,%03d",
                    gDate.get(Calendar.YEAR), gDate.get(Calendar.DAY_OF_YEAR)));
        finishDateField.setColumns(8);
        finishDateField.setName("finishDateField");
        finishDateField.addFocusListener(this);
        //		finishDateField.setBorder(new EmptyBorder(5, 5, 5, 5));

        gDate.setTime(this.fileStartDate);
        fileStartDateField = new JFormattedTextField();
        fileStartDateField.setValue(String.format("%04d,%03d",
                    gDate.get(Calendar.YEAR), gDate.get(Calendar.DAY_OF_YEAR)));
        fileStartDateField.setColumns(8);
        fileStartDateField.setName("fileStartDateField");
        fileStartDateField.setEditable(false);
        //		fileStartDateField.setBorder(new EmptyBorder(5, 5, 5, 5));

        gDate.setTime(this.fileEndDate);
        fileFinishDateField = new JFormattedTextField();
        fileFinishDateField.setValue(String.format("%04d,%03d",
                    gDate.get(Calendar.YEAR), gDate.get(Calendar.DAY_OF_YEAR)));
        fileFinishDateField.setColumns(8);
        fileFinishDateField.setName("fileFinishDateField");
        fileFinishDateField.setEditable(false);
        //		fileFinishDateField.setBorder(new EmptyBorder(5, 5, 5, 5));

        if (daysBack == 0)
        {
            daysBack = (int)((endDate.getTime() - startDate.getTime()) / 86400000);
        }
        daysBackField = new JFormattedTextField();
        daysBackField.setValue(String.format("%d", daysBack));
        daysBackField.setColumns(8);
        daysBackField.setName("daysBackField");
        daysBackField.addFocusListener(this);
        //		daysBackField.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel dateLabel = new JLabel("Date YYYY/MM/DD");
        JLabel availLabel = new JLabel("Available");
        JLabel startLabel = new JLabel("Start");
        JLabel finishLabel = new JLabel("Finish");
        JLabel daysBackLabel = new JLabel("Days Back");

        buttonOK = new JButton("OK");
        buttonOK.addActionListener(this);

        buttonCancel = new JButton("Cancel");
        buttonCancel.addActionListener(this);

        panel.add(new JLabel(""));
        panel.add(dateLabel);
        panel.add(availLabel);
        panel.add(startLabel);
        panel.add(startDateField);
        panel.add(fileStartDateField);
        panel.add(finishLabel);
        panel.add(finishDateField);
        panel.add(fileFinishDateField);
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        panel.add(daysBackLabel);
        panel.add(daysBackField);
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        panel.add(buttonOK);
        panel.add(buttonCancel);

        add(panel);

        // Status field
        statusField = new JTextField();
        statusField.setEditable(false);
        statusField.setText("Enter changes");
        statusField.setSize(DEFAULT_WIDTH, 1);
        add(statusField, BorderLayout.SOUTH);

    } // DateSelect() constructor

    @Override
        public void actionPerformed(ActionEvent e)
        {
            Object source = e.getSource();
            if (source == buttonOK)
            {	
                bCancel = false;
                setVisible(false);
            } // okButton was pressed
            else if (source == buttonCancel)
            {
                bCancel = true;
                setVisible(false);
            }
        } // actionPerformed()

    @Override
        public void focusGained(FocusEvent e)
        {
            JFormattedTextField field = (JFormattedTextField) e.getComponent();

            // Save the current field value in case the user botches up the edit.
            // This allows us to restore the prior value upon field exit
            saveFocusString = field.getText();		
        } // focusGained()

    @Override
        public void focusLost(FocusEvent e)
        {
            Object source = e.getSource();

            Pattern pattern_year_doy= Pattern.compile("(\\d{4}),(\\d{1,3})");

            if (source == daysBackField)
            {
                int  maxDays;

                maxDays = (int)((fileEndDate.getTime() - fileStartDate.getTime()) / 86400000);
                if (((int)((endDate.getTime() - fileStartDate.getTime()) / 86400000)) < maxDays)
                    maxDays = (int)((endDate.getTime() - fileStartDate.getTime()) / 86400000);

                try
                {
                    if (Integer.parseInt(daysBackField.getText()) < 0)
                    {
                        daysBackField.setText("0");
                        statusField.setText("Reset days back to minimum value of 0");
                        Toolkit.getDefaultToolkit().beep();
                    }

                    if (Integer.parseInt(daysBackField.getText()) > maxDays)
                    {
                        daysBackField.setText(Integer.toString(maxDays));
                        statusField.setText("Reseting days back to maximum value of " + maxDays);
                        Toolkit.getDefaultToolkit().beep();
                    }
                    daysBack = Integer.parseInt(daysBackField.getText());
                }
                catch (NumberFormatException e1)
                {
                    statusField.setText("Non integer '" + daysBackField.getText() 
                            + "' in days back field, restoring former value\n");
                    daysBackField.setText(saveFocusString);
                    Toolkit.getDefaultToolkit().beep();
                }

                // Now set new start date
                startDate = new Date(endDate.getTime() - ((long)daysBack * 86400000));
                gDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                gDate.setTimeInMillis(startDate.getTime());
                startDateField.setValue(String.format("%04d,%03d",
                            gDate.get(Calendar.YEAR), gDate.get(Calendar.DAY_OF_YEAR)));
            }
            else if (source == startDateField)
            {
                Matcher matcher = pattern_year_doy.matcher(startDateField.getText());
                if (!matcher.matches())
                {
                    statusField.setText("Invalid year,doy format, reseting start date");
                    startDateField.setText(saveFocusString);
                    Toolkit.getDefaultToolkit().beep();
                }
                else
                {
                    int year;
                    int doy;

                    year = Integer.parseInt(matcher.group(1));
                    doy  = Integer.parseInt(matcher.group(2));
                    Calendar gYearDoy = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                    gYearDoy.clear();
                    gYearDoy.set(Calendar.YEAR, year);
                    gYearDoy.set(Calendar.DAY_OF_YEAR, doy);
                    startDate.setTime(gYearDoy.getTimeInMillis());

                    if (startDate.before(fileStartDate))
                    {
                        startDate.setTime(fileStartDate.getTime());
                        statusField.setText("Reset to earliest available date " 
                                + fileStartDateField.getText());
                        startDateField.setText(fileStartDateField.getText());
                        Toolkit.getDefaultToolkit().beep();	
                    }
                    else if (startDate.after(endDate))
                    {
                        startDate.setTime(endDate.getTime());
                        statusField.setText("Reset to not be after end date " 
                                + finishDateField.getText());
                        startDateField.setText(finishDateField.getText());
                        Toolkit.getDefaultToolkit().beep();						
                    }
                    else
                    {
                        startDate.setTime(gYearDoy.getTimeInMillis());
                    }
                    daysBack = (int)((endDate.getTime() - startDate.getTime()) / 86400000);
                    daysBackField.setText(Integer.toString(daysBack));
                } // input matched year,doy pattern
                System.err.printf("DEBUG %s %s \n%s %s %d\n", 
                        fileStartDate.toString(),
                        fileEndDate.toString(),
                        startDate.toString(),
                        endDate.toString(),
                        daysBack);
            } // startDateField
            else if (source == finishDateField)
            {
                Matcher matcher = pattern_year_doy.matcher(finishDateField.getText());
                if (!matcher.matches())
                {
                    statusField.setText("Invalid year,doy format, reseting finish date");
                    finishDateField.setText(saveFocusString);
                    Toolkit.getDefaultToolkit().beep();
                }
                else
                {
                    int year;
                    int doy;

                    year = Integer.parseInt(matcher.group(1));
                    doy  = Integer.parseInt(matcher.group(2));
                    Calendar gYearDoy = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                    gYearDoy.clear();
                    gYearDoy.set(Calendar.YEAR, year);
                    gYearDoy.set(Calendar.DAY_OF_YEAR, doy);
                    endDate.setTime(gYearDoy.getTimeInMillis());

                    if (endDate.after(fileEndDate))
                    {
                        endDate.setTime(fileEndDate.getTime());
                        statusField.setText("Reset to latest available date " 
                                + fileFinishDateField.getText());
                        finishDateField.setText(fileFinishDateField.getText());
                        Toolkit.getDefaultToolkit().beep();	
                    }
                    else if (endDate.before(startDate))
                    {
                        endDate.setTime(startDate.getTime());
                        statusField.setText("Reset to not be before start date " 
                                + startDateField.getText());
                        finishDateField.setText(startDateField.getText());
                        Toolkit.getDefaultToolkit().beep();						
                    }
                    else
                    {
                        endDate.setTime(gYearDoy.getTimeInMillis());
                    }
                    daysBack = (int)((endDate.getTime() - startDate.getTime()) / 86400000);
                    daysBackField.setText(Integer.toString(daysBack));
                    System.err.printf("DEBUG %s %s \n%s %s %d\n", 
                            fileStartDate.toString(),
                            fileEndDate.toString(),
                            startDate.toString(),
                            endDate.toString(),
                            daysBack);
                } // input matched year,doy pattern
            } // finishDateField
        } // focusLost()

    public	Date	getStartDate()	{return startDate;}
    public	Date	getEndDate()		{return endDate;}
    public	int		getDaysBack()		{return daysBack;}

} // class DateSelect
