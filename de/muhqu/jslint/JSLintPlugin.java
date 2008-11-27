package de.muhqu.jslint;

import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;
import org.gjt.sp.jedit.gui.OptionsDialog;
import org.gjt.sp.jedit.msg.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import errorlist.*;
import java.util.*;
import console.*;
import org.mozilla.javascript.*;


public class JSLintPlugin extends EBPlugin
{
	public final static String NAME = "jslint";
	static boolean log = jEdit.getBooleanProperty("jslint.log",false);;
	DefaultErrorSource errsrc;
	private static JSLintPlugin me;
	
	public void start()
	{
		super.start();
		errsrc = new DefaultErrorSource("JSLint");
		ErrorSource.registerErrorSource(errsrc);
		me = this;
	}

	public void stop()
	{
		ErrorSource.unregisterErrorSource(errsrc);
		errsrc = null;
		me = null;
		super.stop();
	}

	public void handleMessage(EBMessage ebmess)
	{
		if (ebmess instanceof BufferUpdate) {
			BufferUpdate bu = (BufferUpdate)ebmess;
			if (bu.getWhat() == BufferUpdate.SAVED) {
				System.out.println("JSLint running on save : "+
				jEdit.getBooleanProperty("jslint.runonsave"));
				if(jEdit.getBooleanProperty("jslint.runonsave")) {
					run(bu.getView());
				}
			}
		}
		else if (ebmess instanceof EditPaneUpdate) {
			EditPaneUpdate epu = (EditPaneUpdate)ebmess;
			if (epu.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
				System.out.println("JSLint running on buffer switch : "+
				jEdit.getBooleanProperty("jslint.runonbufferswitch"));
				if(jEdit.getBooleanProperty("jslint.runonbufferswitch")) {
					run(epu.getEditPane().getView());
				}
			}
		}
	}
	
	private boolean needToRunOnBuffer(Buffer buffer) {
		String buffer_mode = buffer.getMode().getName();
		String buffer_modes_to_run_on = jEdit.getProperty("jslint.buffermodes", "javascript,html"); // "javascript,html,php";
		StringTokenizer strtok = new StringTokenizer(buffer_modes_to_run_on,",");
		while(strtok.hasMoreTokens())
		{
			String mode_to_test = (strtok.nextToken()).trim();
			if (buffer_mode.equals(mode_to_test)) return true;
		}
		return false;
	}
	
	public static String inputStreamAsString(InputStream stream)
		throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();
		return sb.toString();
	}

	public static void runJSLint(View view)
	{
		me.run(view);
	}

	public void run(View view)
	{
		try
		{
			Buffer buffer = view.getBuffer();
			boolean needtorun = needToRunOnBuffer(buffer);
			System.out.println("JSLINT: need to run: " + needtorun + "  (mode:"+buffer.getMode().getName()+")");
			if (needtorun)
			{
				String jslintsource = inputStreamAsString(this.getClass().getResourceAsStream("/jslint.js"));
				//System.out.println("Got JSLint Source: " + jslintsource);
				
				String sourcepath = buffer.getPath();
				String jssource = buffer.getText(0,buffer.getLength());
				//System.out.println("Got Clean Source: " + cleanjssource);
				
				errsrc.removeFileErrors(sourcepath);
				
				Context cx = ContextFactory.getGlobal().enterContext();
				try {
					Scriptable scope = cx.initStandardObjects();
					
					// Now evaluate JSLint Source, so we've setuped our JS env
					cx.evaluateString(scope, jslintsource, "<cmd>", 1, null);
					
					// Checking for JSLINT function
					Object fObj = scope.get("JSLINT", scope);
					if (!(fObj instanceof Function)) {
						System.out.println("JSLINT is not defined.");
					} else {
						System.out.println("JSLINT is defined, so call it.");
				
						String options = jEdit.getProperty("jslint.options");
						//System.out.println("JSLint Options: " + options);
						
						Scriptable jsOpt = cx.newObject(scope);
						try {
							StringTokenizer strtok = new StringTokenizer(options,",:");
							while(strtok.hasMoreTokens())
							{
								String key = strtok.nextToken();
								String value = strtok.nextToken();
								//System.out.println("key: "+key+"   value: "+value);
								if (value.equalsIgnoreCase("TRUE")) {
									jsOpt.put(key, jsOpt, true);
								}
								else if (value.equalsIgnoreCase("FALSE")) {
									jsOpt.put(key, jsOpt, false);
								}
								else {
									jsOpt.put(key, jsOpt, value);
								}
							}
						}
						catch (NoSuchElementException e) {
						}
						//System.out.println("jsOpt: "+Context.toString(jsOpt));
						//System.out.println("eqeqeq: "+Context.toString(jsOpt.get("eqeqeq", scope)));
						
						Object functionArgs[] = { jssource.replaceAll("\t", "    "), jsOpt }; // need to translate TABs to Spaces to get correct column references
						Function JSLINT = (Function)fObj;
						Object result = JSLINT.call(cx, scope, scope, functionArgs);
						Scriptable errArr = (Scriptable) JSLINT.get("errors", scope);
						int errLength = (int)Context.toNumber(ScriptableObject.getProperty(errArr, "length"));
						System.out.println("JSLINT.errors.length: "+errLength);
						System.out.println("JSLINT.errors: "+Context.toString(errArr));
						for (int i=0; i<errLength; i++){
							Scriptable err = (Scriptable) errArr.get(i, scope);
							if (Context.toString(err).equals("null")) continue;
							
							int errLine = (int) Context.toNumber( ScriptableObject.getProperty(err, "line") );
							int errCol = (int) Context.toNumber( ScriptableObject.getProperty(err, "character") );
							String errMsg = Context.toString( ScriptableObject.getProperty(err, "reason") );
							
							System.out.println("JSLINT.errors["+i+"]: on line "+errLine+", col "+errCol+", msg: "+errMsg+"");
							
							errsrc.addError(ErrorSource.ERROR,
								sourcepath,
								errLine,
								0,
								errCol,
								errMsg);
						}
					}
				} finally {
					Context.exit();
				}
			}
		}
		catch(IOException e)
		{
			Log.log(Log.ERROR,this.getClass(),"IOException when executing Process "+e);
			e.printStackTrace();
		}
	}

	private String processOptions(String props)
	{
		if (props == null)
		{
			return "";
		}

		StringBuffer strbuf = new StringBuffer();
		StringTokenizer strtok = new StringTokenizer(props,",");
		while(strtok.hasMoreTokens())
		{
			strbuf.append(" -"+strtok.nextToken());
		}
		return strbuf.toString();
	}

}