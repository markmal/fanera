package org.markmal.fanera;

/**
 * This class sets properties of plywood.
 * 
 * @license GNU LGPL (LGPL.txt):
 * 
 * @author Mark Malakanov
 * @version 1.2.2.10
 * 
 **/

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.text.NumberFormatter;

import java.beans.*; //property change stuff
import java.text.NumberFormat;
import java.awt.*;
import java.awt.event.*;
 
class PlywoodDialog extends JDialog
                   implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private JOptionPane optionPane;
    private String btnString1 = "Ok";
    private String btnString2 = "Cancel";

    boolean result = false;
    JButton okButton = new JButton("Ok");
    JButton cancelButton = new JButton("Cancel");
    
    JFormattedTextField plyThickness;
    JFormattedTextField glueRatio;
    JLabel plyThicknessLabel = new JLabel("Ply Thickness, mm");
    JLabel glueRatioLabel = new JLabel(
    			"<html>Glue Ratio, how many times glue is thinner than the Ply.<br>"
    			+ "For example, it is 8 if glue is 1/8 of the ply thickness.</html>");
    
    public PlywoodDialog(Frame aFrame, String aWord, Component parent) {
        super(aFrame, true);
        setLocationRelativeTo(aFrame);
        //setUndecorated(true);
        //getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        setTitle("Define Plywood");

	NumberFormat numberFormatInt = NumberFormat.getIntegerInstance();
	NumberFormat numberFormatDec = NumberFormat.getNumberInstance();
	NumberFormatter numberFormatterInt = new NumberFormatter(numberFormatInt);
	NumberFormatter numberFormatterDec = new NumberFormatter(numberFormatDec);

	numberFormatterInt.setAllowsInvalid(false);
	numberFormatterInt.setMinimum(1);
	numberFormatterDec.setAllowsInvalid(false);
	numberFormatterDec.setMinimum(0.1f);

        
        plyThickness = new JFormattedTextField(numberFormatterDec);
        plyThickness.setColumns(5);
        plyThickness.setPreferredSize(new Dimension(60,20));
        
        glueRatio = new JFormattedTextField(numberFormatterInt);
        glueRatio.setColumns(5);
        glueRatio.setPreferredSize(new Dimension(60,20));
        
        plyThickness.setValue(new Float(1.5f));
        glueRatio.setValue(new Integer(8));
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel,BorderLayout.SOUTH);

        JPanel fieldPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(20,20,5,20);
        c.gridx = 0; c.gridy = 0; c.weightx = 0.5;
        fieldPanel.add(plyThicknessLabel,c);

        c.gridx = 1; c.gridy = 0; c.weightx = 1;
        fieldPanel.add(plyThickness,c);

        c.insets = new Insets(5,20,20,20);
        c.gridx = 0; c.gridy = 1; c.weightx = 0.5;
        fieldPanel.add(glueRatioLabel,c);

        c.gridx = 1; c.gridy = 1; c.weightx = 1;
        fieldPanel.add(glueRatio,c);
          
        contentPanel.add(fieldPanel,BorderLayout.CENTER);
        contentPanel.add(new JSeparator(), BorderLayout.NORTH);
        
        setContentPane(contentPanel);
        
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        
        //Handle window closing correctly.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
 
        //Ensure the text field always gets the first focus.
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent ce) {
            	if (plyThickness != null)
            		plyThickness.requestFocusInWindow();
            }
        });
        
    }
 
    /** This method handles events for the text field. */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton){
        	result = true;
        	setVisible(false);
        }
        if (e.getSource() == cancelButton){
        	result = false;
        	setVisible(false);
        }
    }

	public float getPlyThickness() {
		return (Float)this.plyThickness.getValue();
	}
	public void setPlyThickness(float v) {
		this.plyThickness.setValue(new Float(v));
	}

	public int getGlueRatio() {
		return (Integer)this.glueRatio.getValue();
	}
	public void setGlueRatio(int v) {
		this.glueRatio.setValue(new Integer(v));
	}
 
}