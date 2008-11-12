package de.muhqu.jslint;

import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;
import org.gjt.sp.jedit.msg.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import errorlist.*;
import java.util.*;


public class JSLintOptionPane extends AbstractOptionPane
	implements ActionListener
{
	private Hashtable<String, JCheckBox> OptionCheckBoxes = new Hashtable<String, JCheckBox>();
	private Hashtable<String, JRadioButton> PresetRadioButtons = new Hashtable<String, JRadioButton>();
	private ButtonGroup preSelectBtnGroup = new ButtonGroup();
	
	private JCheckBox chk_runOnSave;
	private JCheckBox chk_runOnSwitch;
	
	
	private String advOptions
			= "passfail|Stop on first error|the scan should stop on first error\n"
			+ "white|Strict white space|strict whitespace rules apply\n"
			+ "browser|Assume a browser|standard browser globals should be predefined\n"
			+ "widget|Assume a Yahoo Widget|the Yahoo Widgets globals should be predefined\n"
			+ "sidebar|Assume a Windows Sidebar Gadget|the Windows Sidebar Gadgets globals should be predefined\n"
			+ "rhino|Assume Rhino|the Rhino environment globals should be predefined\n"
			+ "safe|Safe Subset|the safe subset rules are enforced. These rules are used by ADsafe.\n"
			+ "adsafe|ADsafe|ADsafe.org rules widget pattern should be enforced.\n"
			
			+ "debug|Tolerate debugger statements|debugger statements should be allowed\n"
			+ "evil|Tolerate eval|eval should be allowed\n"
			+ "cap|Tolerate HTML case|upper case HTML should be allowed\n"
			+ "on|Tolerate HTML event handlers|HTML event handlers should be allowed\n"
			+ "fragment|Tolerate HTML fragments|HTML fragments should be allowed\n"
			+ "laxbreak|Tolerate sloppy line breaking|statement breaks should not be checked\n"
			+ "forin|Tolerate unfiltered for in|unfiltered for in statements should be allowed\n"
			+ "sub|Tolerate inefficient subscripting|subscript notation may be used for expressions better expressed in dot notation\n"
			+ "css|Tolerate CSS workarounds|CSS workarounds should be tolerated\n"
			
			+ "undef|Disallow undefined variables|undefined global variables are errors\n"
			+ "nomen|Disallow leading _ in identifiers|names should be checked for initial underbars\n"
			+ "eqeqeq|Disallow  ==  and  !=|=== should be required\n"
			+ "plusplus|Disallow ++ and --|++ and -- should not be allowed\n"
			+ "bitwise|Disallow bitwise operators|bitwise operators should not be allowed\n"
			+ "regexp|Disallow . in RegExp literals|. should not be allowed in RegExp literals\n"
			
			+ "onevar|Allow only one var statement per function|only one var statement per function should be allowed\n"
			+ "strict|Require \"use strict\";|the ES3.1 \"use strict\"; pragma is required.";
	

	public JSLintOptionPane()
	{
		super(JSLintPlugin.NAME);
	}

	protected void _init()
	{
		setUp();
	}

	public void _save()
	{
		jEdit.setBooleanProperty("jslint.runonsave",
			chk_runOnSave.isSelected());
		jEdit.setBooleanProperty("jslint.runonbufferswitch",
			chk_runOnSwitch.isSelected());
		saveSettings();
	}

	public void setUp()
	{
		JPanel pnlMain = new JPanel(new BorderLayout());

		//General Options setup
		GridLayout gLay = new GridLayout();
		gLay.setColumns(1);
		gLay.setRows(0);
		
		JPanel pnlGeneral = new JPanel(gLay);
		pnlGeneral.setBorder(new TitledBorder("General Options"));
		
		chk_runOnSave = new JCheckBox(
			"Run JSLint on Buffer save",
			jEdit.getBooleanProperty("jslint.runonsave")
		);
		pnlGeneral.add(chk_runOnSave);
		
		chk_runOnSwitch = new JCheckBox(
			"Run JSLint on Buffer switch",
			jEdit.getBooleanProperty("jslint.runonbufferswitch")
		);
		pnlGeneral.add(chk_runOnSwitch);
		
		JRadioButton pre_recommended = new JRadioButton("Recommended Options");
		PresetRadioButtons.put("recommended",pre_recommended);
		pre_recommended.addActionListener(this);
		preSelectBtnGroup.add(pre_recommended);
		pnlGeneral.add(pre_recommended);
		
		JRadioButton pre_goodparts = new JRadioButton("Good Parts");
		PresetRadioButtons.put("goodparts",pre_goodparts);
		pre_goodparts.addActionListener(this);
		preSelectBtnGroup.add(pre_goodparts);
		pnlGeneral.add(pre_goodparts);
		
		JRadioButton pre_clearall = new JRadioButton("Clear all");
		PresetRadioButtons.put("clearall",pre_clearall);
		pre_clearall.addActionListener(this);
		preSelectBtnGroup.add(pre_clearall);
		pnlGeneral.add(pre_clearall);

		pnlMain.add(BorderLayout.NORTH, pnlGeneral);
		pnlMain.add(BorderLayout.CENTER, getOptionsPanel());
		//pnlMain.add(getOptionsPanel());

		addComponent(pnlMain);
		loadSettings();
	}
	
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		System.out.println("Action: "+action);
		String[] actionSplit = action.split("_");
		
		if (actionSplit[0].equals("checkbox")) {
			// checkbox action
			String key = actionSplit[1];
			System.out.println("key: "+key);
			
			checkPreselection();
		}
		else {
			// must be a radio button switch...
			System.out.println("must be radio button switch");
		
			Enumeration enumeration = PresetRadioButtons.keys();
			while(enumeration.hasMoreElements())
			{
				String key = (String)enumeration.nextElement();
				JRadioButton radiobtn = (JRadioButton)PresetRadioButtons.get(key);
				if (radiobtn.isSelected()) {
					this.applyPreselect(key);
				}
			}
		}
	}
	public void checkPreselection() {
		Enumeration enum1 = PresetRadioButtons.keys();
		while(enum1.hasMoreElements())
		{
			String radiokey = (String)enum1.nextElement();
			JRadioButton radiobtn = (JRadioButton)PresetRadioButtons.get(radiokey);
			String options = this.getPreselectOptions(radiokey);
			System.out.println("radiokey: "+radiokey+"   options: "+options);
			
			Enumeration enum2 = OptionCheckBoxes.keys();
			int wrong = 0;
			while(enum2.hasMoreElements())
			{
				String checkkey = (String)enum2.nextElement();
				JCheckBox checkbox = (JCheckBox)OptionCheckBoxes.get(checkkey);
				Boolean shouldbechecked = options.indexOf("|"+checkkey+"|") >= 0;
				if (checkbox.isSelected() != shouldbechecked) wrong++;
			}
			System.out.println("wrong: "+wrong);
			if (wrong == 0) radiobtn.setSelected(true);
			else if (radiobtn.isSelected()) {
				// deselect the button...
				// need this workaround to deselect a radiobutton in a ButtonGroup
				preSelectBtnGroup.remove(radiobtn);
				radiobtn.setSelected(false);
				preSelectBtnGroup.add(radiobtn);
			}
		}
	}
	
	public void applyPreselect(String preset) {
		String options = this.getPreselectOptions(preset);
		Enumeration enumeration = OptionCheckBoxes.keys();
		while(enumeration.hasMoreElements())
		{
			String key = (String)enumeration.nextElement();
			JCheckBox checkbox = (JCheckBox)OptionCheckBoxes.get(key);
			checkbox.setSelected( options.indexOf("|"+key+"|")>=0 ? true : false );
		}
		if (PresetRadioButtons.containsKey(preset)) {
			((JRadioButton)PresetRadioButtons.get(preset)).setSelected(true);
		}
	}
	
	public String getPreselectString() {
		return "";
	}
	
	public String getPreselectOptions(String preset) {
		String options = "|"; // clearall
		if (preset == "recommended") {
			options = "|eqeqeq|nomen|undef|white|";
		}
		else if (preset == "goodparts") {
			options = "|bitwise|eqeqeq|nomen|onevar|plusplus|regexp|undef|white|";
		}
		return options;
	}

	private JPanel getOptionsPanel()
	{
		JPanel pnl = new JPanel();
		pnl.setBorder(new TitledBorder("Advance Options"));
		pnl.setLayout(new BoxLayout(pnl, BoxLayout.X_AXIS));
		JPanel pnlLeft = new JPanel();
		pnlLeft.setLayout(new BoxLayout(pnlLeft, BoxLayout.Y_AXIS));
		JPanel pnlRight = new JPanel();
		pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));
		pnl.add(pnlLeft);
		pnl.add(pnlRight);
		
		StringTokenizer strtok = new StringTokenizer(advOptions,"|\n");
		int count = 0;
		int max = advOptions.split("\n").length;
		while(strtok.hasMoreTokens())
		{
			String key = strtok.nextToken();
			String title = strtok.nextToken();
			String desc = strtok.nextToken();
			JCheckBox checkbox = new JCheckBox(title,false);
			checkbox.setToolTipText(desc);
			OptionCheckBoxes.put(key, checkbox);
			checkbox.setActionCommand("checkbox_"+key);
			checkbox.addActionListener(this);
			((count++<=max/2) ? pnlLeft : pnlRight).add(checkbox);
		}
		
		return pnl;
	}

	void loadSettings()
	{
		String options = jEdit.getProperty("jslint.options");
		System.out.println("loadSettings:: Options: " + options);
		if (options == null) {
			applyPreselect("recommended");
			saveSettings();
			return;
		}
		
		StringTokenizer strtok = new StringTokenizer(options,",:");
		while(strtok.hasMoreTokens())
		{
			String key = strtok.nextToken();
			String value = strtok.nextToken();
			if (OptionCheckBoxes.containsKey(key)) {
				((JCheckBox)OptionCheckBoxes.get(key))
					.setSelected(value.equalsIgnoreCase("TRUE"));
			}
		}
		checkPreselection();
	}


	void saveSettings()
	{
		StringBuffer strbuf = new StringBuffer();
		Enumeration enumeration = OptionCheckBoxes.keys();
		while(enumeration.hasMoreElements())
		{
			String key = (String)enumeration.nextElement();
			JCheckBox checkbox = (JCheckBox)OptionCheckBoxes.get(key);
			if (strbuf.length() > 0) strbuf.append(",");
			strbuf.append(key+":"+( (checkbox != null && !checkbox.isSelected()) ? "FALSE": "TRUE" ));
		}
		jEdit.setProperty("jslint.options", strbuf.toString());
	}
}
