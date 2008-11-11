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
	JCheckBox chk_runOnSave;
	private boolean allOn=false;
	private Hashtable<String, JCheckBox> OptionCheckBoxes = new Hashtable<String, JCheckBox>();
	private Hashtable<String, JRadioButton> PresetRadioButtons = new Hashtable<String, JRadioButton>();
	private ButtonGroup preSelectBtnGroup = new ButtonGroup();

	public JSLintOptionPane()
	{
		super(JSLintPlugin.NAME);
	}

	protected void _init()
	{
		String msg_options = jEdit.getProperty("jslint.options");
		if (msg_options == null)
		{
			allOn=true;
		}
		setUp();
	}

	public void _save()
	{
		jEdit.setBooleanProperty("jslint.runonsave",chk_runOnSave.isSelected());
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
		
		String preSelect = this.getPreselectString();

		JRadioButton pre_recommended = new JRadioButton(
				"Recommended Options",
				(preSelect == "recommended")
			);
		PresetRadioButtons.put("recommended",pre_recommended);
		pre_recommended.addActionListener(this);
		preSelectBtnGroup.add(pre_recommended);
		pnlGeneral.add(pre_recommended);
		
		JRadioButton pre_goodparts = new JRadioButton(
				"Good Parts",
				(preSelect == "goodparts")
			);
		PresetRadioButtons.put("goodparts",pre_goodparts);
		pre_goodparts.addActionListener(this);
		preSelectBtnGroup.add(pre_goodparts);
		pnlGeneral.add(pre_goodparts);
		
		JRadioButton pre_clearall = new JRadioButton(
				"Clear all",
				(preSelect == "clearall")
			);
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
		GridLayout gLay = new GridLayout();
		gLay.setColumns(2);
		gLay.setRows(0);
		
		JPanel pnl = new JPanel(gLay);
		
		pnl.setBorder(new TitledBorder("Advance Options"));
		
		// adsafe	true if ADsafe.org  rules widget pattern should be enforced.
		OptionCheckBoxes.put("adsafe", new JCheckBox("ADsafe.org  rules widget pattern should be enforced.",false));
		
		// bitwise	true if bitwise operators should not be allowed
		OptionCheckBoxes.put("bitwise", new JCheckBox("bitwise operators should not be allowed",false));
		
		// browser	true if the standard browser globals should be predefined
		OptionCheckBoxes.put("browser", new JCheckBox("standard browser globals should be predefined",false));
		
		// cap	true if upper case HTML should be allowed
		OptionCheckBoxes.put("cap", new JCheckBox("upper case HTML should be allowed",false));
		
		// css	true if CSS workarounds should be tolerated
		OptionCheckBoxes.put("css", new JCheckBox("CSS workarounds should be tolerated",false));
		
		// debug	true if debugger statements should be allowed
		OptionCheckBoxes.put("debug", new JCheckBox("debugger statements should be allowed",false));
		
		// eqeqeq	true if === should be required
		OptionCheckBoxes.put("eqeqeq", new JCheckBox("=== should be required",false));
		
		// evil	true if eval should be allowed
		OptionCheckBoxes.put("evil", new JCheckBox("eval should be allowed",false));
		
		// forin	true if unfiltered for in statements should be allowed
		OptionCheckBoxes.put("forin", new JCheckBox("unfiltered for in statements should be allowed",false));
		
		// fragment	true if HTML fragments should be allowed
		OptionCheckBoxes.put("bitwise", new JCheckBox("HTML fragments should be allowed",false));
		
		// // indent	the number of spaces used for indentation (default is 4)
		
		// laxbreak	true if statement breaks should not be checked
		OptionCheckBoxes.put("laxbreak", new JCheckBox("statement breaks should not be checked",false));
		
		// nomen	true if names should be checked for initial underbars
		OptionCheckBoxes.put("nomen", new JCheckBox("names should be checked for initial underbars",false));
		
		// on	true if HTML event handlers should be allowed
		OptionCheckBoxes.put("on", new JCheckBox("HTML event handlers should be allowed",false));
		
		// onevar	true if only one var statement per function should be allowed
		OptionCheckBoxes.put("onevar", new JCheckBox("only one var statement per function should be allowed",false));
		
		// passfail	true if the scan should stop on first error
		OptionCheckBoxes.put("passfail", new JCheckBox("the scan should stop on first error",false));
		
		// plusplus	true if ++ and -- should not be allowed
		OptionCheckBoxes.put("plusplus", new JCheckBox("++ and -- should not be allowed",false));
		
		// // predef	an array of strings, the names of predefined global variables
		
		// regexp	true if . should not be allowed in RegExp literals
		OptionCheckBoxes.put("regexp", new JCheckBox(". should not be allowed in RegExp literals",false));
		
		// rhino	true if the Rhino environment globals should be predefined
		OptionCheckBoxes.put("rhino", new JCheckBox("the Rhino environment globals should be predefined",false));
		
		// safe	true if the safe subset rules are enforced. These rules are used by ADsafe.
		OptionCheckBoxes.put("safe",  new JCheckBox("the safe subset rules are enforced. These rules are used by ADsafe.",false));
		
		// sidebar	true if the Windows Sidebar Gadgets globals should be predefined
		OptionCheckBoxes.put("sidebar",  new JCheckBox("the Windows Sidebar Gadgets globals should be predefined",false));
		
		// strict	true if the ES3.1 "use strict"; pragma is required.
		OptionCheckBoxes.put("strict", new JCheckBox("the ES3.1 \"use strict\"; pragma is required.",false));
		
		// sub	true if subscript notation may be used for expressions better expressed in dot notation
		OptionCheckBoxes.put("sub", new JCheckBox("subscript notation may be used for expressions better expressed in dot notation",false));
		
		// undef	true if undefined global variables are errors
		OptionCheckBoxes.put("undef", new JCheckBox("undefined global variables are errors",false));
		
		// white	true if strict whitespace rules apply
		OptionCheckBoxes.put("white", new JCheckBox("strict whitespace rules apply",false));
		
		// widget	true if the Yahoo Widgets globals should be predefined
		OptionCheckBoxes.put("widget", new JCheckBox("the Yahoo Widgets globals should be predefined",false));
		
		
		Enumeration enumeration = OptionCheckBoxes.keys();
		while(enumeration.hasMoreElements())
		{
			String key = (String)enumeration.nextElement();
			JCheckBox checkbox = (JCheckBox)OptionCheckBoxes.get(key);
			checkbox.setActionCommand("checkbox_"+key);
			checkbox.addActionListener(this);
			pnl.add(checkbox);
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
