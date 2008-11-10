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
{
	JTextField txtjslintlocation;
	JCheckBox chkRunOnSave;
	private boolean allOn=false;
	private Hashtable hashOptions = new Hashtable();

	public JSLintOptionPane()
	{
		super(JSLintPlugin.NAME);
	}

	protected void _init()
	{
		String msg_options = jEdit.getProperty("jslint.disabled_options");
		if (msg_options == null)
		{
			allOn=true;
		}
		setUp();
		txtjslintlocation.setText(jEdit.getProperty("jslint.path"));
	}

	public void _save()
	{
		if(txtjslintlocation != null)
		{
			jEdit.setProperty("jslint.path",txtjslintlocation.getText());
			jEdit.setBooleanProperty("jslint.runonsave",chkRunOnSave.isSelected());
			saveSettings();
		}
	}

	public void setUp()
	{
		JPanel pnlMain = new JPanel(new BorderLayout());

		//General Options setup
		JPanel pnlGeneral = new JPanel(new GridLayout(3,1));
		JPanel pnljslint = new JPanel();
		JPanel pnlrunOnSave = new JPanel();

		txtjslintlocation = new JTextField(40);
		chkRunOnSave = new JCheckBox("Run JSLint on Buffer save",jEdit.getBooleanProperty("jslint.runonsave"));

		pnljslint.add(new JLabel("Path to jslint.js (Rhino Version)"));
		pnljslint.add(txtjslintlocation);

		pnlrunOnSave.add(chkRunOnSave);

		pnlGeneral.add(pnljslint);
		pnlGeneral.add(pnlrunOnSave);

		pnlMain.add(BorderLayout.NORTH,pnlGeneral);

		pnlMain.add(BorderLayout.CENTER,new JScrollPane(getOptions()));

		addComponent(pnlMain);
		loadSettings();
	}


	private JPanel getOptions()
	{
		GridLayout gLay = new GridLayout();
		gLay.setColumns(1);
		gLay.setRows(0);

		JPanel pnl = new JPanel(gLay);

		pnl.setBorder(new TitledBorder("Advance Options"));

		JCheckBox chkdeadlock = new JCheckBox("Check for Deadlocks",true);
		hashOptions.put("deadlock", chkdeadlock);
		pnl.add(chkdeadlock);
        
		JCheckBox chkraceCondition = new JCheckBox("Check for Race Conditions",true);
		hashOptions.put("raceCondition", chkraceCondition);
		pnl.add(chkraceCondition);

		return pnl;
	}

	void loadSettings()
	{
		//We store disabled options becoz since by default ALL chkboxes are enabled, now we can just uncheck those chkboxes whose name is found, the rest are checked as it is. Now if we were to kee chkboxes checked as false then we would have to write code for specially auto checking them all for the first time when no codelint.msg_option is found. if prefer that the former way is the best.

		String disabledOptions = jEdit.getProperty("jslint.disabled_options");
		if (JSLintPlugin.log)
		{
			Log.log(Log.DEBUG,this.getClass(),"See disabledOptions in loadSettings "+ disabledOptions);
		}
		if (disabledOptions == null)
		{
			return;
		}

		StringTokenizer strtok = new StringTokenizer(disabledOptions,",");
		while(strtok.hasMoreTokens())
		{
			String key = strtok.nextToken();
			if (hashOptions.containsKey(key))
			{
				//Good this key matches on component. So set its value
				if (JSLintPlugin.log)
				{
					Log.log(Log.DEBUG,this.getClass(),"Seeting to false for key "+ key);
				}
				((JCheckBox)hashOptions.get(key)).setSelected(false);
			}
		}
	}


	void saveSettings()
	{
		StringBuffer strbuf = new StringBuffer();
		Enumeration enumeration = hashOptions.keys();
		while(enumeration.hasMoreElements())
		{
			String key = (String)enumeration.nextElement();
			JCheckBox checkbox = (JCheckBox)hashOptions.get(key);
			if (checkbox != null && !checkbox.isSelected())
			{
				if (JSLintPlugin.log)
				{
					Log.log(Log.DEBUG,this.getClass(),"Setting disabled key "+ key +" checkbox state "+ checkbox.isSelected());
				}
				strbuf.append(key+",");
			}
		}
		jEdit.setProperty("jslint.disabled_options",strbuf.toString());
	}
}
