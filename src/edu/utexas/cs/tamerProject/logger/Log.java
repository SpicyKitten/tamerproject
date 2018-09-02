package edu.utexas.cs.tamerProject.logger;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * Provides functionality for a simple per-class logger that
 * writes to the System.out stream, flushes properly, and
 * can set simplicity levels of output both over the logger
 * and a single logged message.
 * TODO: Inherited but not overridden methods are unlikely
 * to work as intended. For example the entering() method
 * should almost always be a LOW/NONE-Simplicity message.
 * @author ratha
 *
 */
public class Log extends Logger{
	//this variable can enable a global level over all Loggers, not recommended
	private static final Level absoluteLevel = null;
	private Simplicity simplicity;
    private final Formatter myFormatter = new Formatter() {
        public String format(LogRecord r) {
        	String shortName = r.getSourceClassName();
        	if(getSimplicity() != Simplicity.LOW)
			try { 
				shortName = Class.forName(shortName).getSimpleName(); }
			catch (Exception e)//bad practice but hard to avoid if you feed it something bad
			{ shortName = r.getSourceClassName(); }
            String ret = MessageFormat.format("{0} {1} {2} {3} {4}{5}", 
            		new Date(),shortName,r.getSourceMethodName(),
            		r.getLevel(),r.getMessage(),System.getProperty("line.separator"));
            if(getSimplicity() == Simplicity.HIGH)
            	ret = r.getMessage() + System.getProperty("line.separator");
            return ret;
        }
    };
    /**
     * Creates logger for class <code>c</code> with general
     * log level <code>l</code> and simplicity level <code>s</code>. 
     * This logger will be activated under the conditions of 
     * LogManager's {@linkplain java.util.logging.LogManager#addLogger(Logger) addLogger} 
     * with logger name <code>c.getName()</code>.
     */
	public Log(Class<?> c, Level l, Simplicity s)
	{
		super(c.getName(), null);
		if(absoluteLevel != null)
			l = absoluteLevel;
		//have to do a bit of stuff here to make sure that the
		//output is flushed to System.out stream pro-actively
		Handler h = new StreamHandler(System.out, myFormatter)
				{
					public void publish(LogRecord record)
					{
						if(record == null)
							return;
						super.publish(record);
						super.flush();
					}
				};
		h.setLevel(l);
		addHandler(h);
//		h = new StreamHandler(System.err, myFormatter);
//		h.setLevel(l);
//		addHandler(h);
		this.setSimplicity(s);
		this.setUseParentHandlers(false);
		this.setLevel(l);
		LogManager.getLogManager().addLogger(this);
	}
	public void setLevel(Level l)
	{
		super.setLevel(l);
		for(Handler h : this.getHandlers())
		{
			h.setLevel(l);
		}
	}
	
	public enum Simplicity
	{
		HIGH,MEDIUM,LOW,NONE;
	}
	public void log(Level level, String msg, Object param1, Simplicity s) 
	{
		StackTraceElement caller = this.getCallerStackFrame();
		if(!isLoggable(level)) return;
		Map<String,String> info = this.getCallerInfo(caller);
		Simplicity t = this.getSimplicity();
		this.setSimplicity(s);
		super.logp(level, info.get("class"), info.get("method"), msg, param1);
		this.setSimplicity(t);
	}
	public void log(Level level, String msg, Simplicity s)
	{
		StackTraceElement caller = this.getCallerStackFrame();
		if(!isLoggable(level)) return;
		Map<String,String> info = this.getCallerInfo(caller);
		Simplicity t = this.getSimplicity();
		this.setSimplicity(s);
		super.logp(level, info.get("class"), info.get("method"), msg, (Object[])null);
		this.setSimplicity(t);
	}
	public void fine(String msg, Simplicity s)
	{
		StackTraceElement caller = this.getCallerStackFrame();
		if(!isLoggable(Level.FINE)) return;
		Map<String,String> info = this.getCallerInfo(caller);
		Simplicity t = this.getSimplicity();
		this.setSimplicity(s);
		super.logp(Level.FINE, info.get("class"), info.get("method"), msg, (Object[])null);
		this.setSimplicity(t);
	}
	public void severe(String msg, Simplicity s)
	{
		StackTraceElement caller = this.getCallerStackFrame();
		if(!isLoggable(Level.SEVERE)) return;
		Map<String,String> info = this.getCallerInfo(caller);
		Simplicity t = this.getSimplicity();
		this.setSimplicity(s);
		super.logp(Level.SEVERE, info.get("class"), info.get("method"), msg, (Object[])null);
		this.setSimplicity(t);
	}
	public void info(String msg, Simplicity s)
	{
		StackTraceElement caller = this.getCallerStackFrame();
		if(!isLoggable(Level.INFO)) return;
		Map<String,String> info = this.getCallerInfo(caller);
		Simplicity t = this.getSimplicity();
		this.setSimplicity(s);
		super.logp(Level.INFO, info.get("class"), info.get("method"), msg, (Object[])null);
		this.setSimplicity(t);
	}
	public void finer(String msg, Simplicity s)
	{
		StackTraceElement caller = this.getCallerStackFrame();
		if(!isLoggable(Level.FINER)) return;
		Map<String,String> info = this.getCallerInfo(caller);
		Simplicity t = this.getSimplicity();
		this.setSimplicity(s);
		super.logp(Level.FINER, info.get("class"), info.get("method"), msg, (Object[])null);
		this.setSimplicity(t);
	}
	public void finest(String msg, Simplicity s)
	{
		StackTraceElement caller = this.getCallerStackFrame();
		if(!isLoggable(Level.FINEST)) return;
		Map<String,String> info = this.getCallerInfo(caller);
		Simplicity t = this.getSimplicity();
		this.setSimplicity(s);
		super.logp(Level.FINEST, info.get("class"), info.get("method"), msg, (Object[])null);
		this.setSimplicity(t);
	}
	public void warning(String msg, Simplicity s)
	{
		StackTraceElement caller = this.getCallerStackFrame();
		if(!isLoggable(Level.WARNING)) return;
		Map<String,String> info = this.getCallerInfo(caller);
		Simplicity t = this.getSimplicity();
		this.setSimplicity(s);
		super.logp(Level.WARNING, info.get("class"), info.get("method"), msg, (Object[])null);
		this.setSimplicity(t);
	}
	public void config(String msg, Simplicity s)
	{
		StackTraceElement caller = this.getCallerStackFrame();
		if(!isLoggable(Level.CONFIG)) return;
		Map<String,String> info = this.getCallerInfo(caller);
		Simplicity t = this.getSimplicity();
		this.setSimplicity(s);
		super.logp(Level.CONFIG, info.get("class"), info.get("method"), msg, (Object[])null);
		this.setSimplicity(t);
	}
	private Simplicity getSimplicity() {
		return simplicity;
	}
	private void setSimplicity(Simplicity simplicity) {
		this.simplicity = simplicity;
		if(this.simplicity == Simplicity.NONE)
			for(Handler h : this.getHandlers())
				h.setFormatter(new SimpleFormatter());
		else
			for(Handler h : this.getHandlers())
				h.setFormatter(myFormatter);
	}
	/**
	* Gets the StackTraceElement of the first class that is not this class.
	* That should be the initial caller of a logging method.
	* @return caller of the initial logging method or null if unknown.
	* @see <a href="http://developer.classpath.org/doc/java/util/logging/Logger-source.html">Logger source code</a>
	*/
	private StackTraceElement getCallerStackFrame()
	{
	Throwable t = new Throwable();
	StackTraceElement[] stackTrace = t.getStackTrace();
	int index = 0;
	
	// skip to stackentries until this class
	while(index < stackTrace.length
	&& !stackTrace[index].getClassName().equals(getClass().getName()))
	index++;
	
	// skip the stackentries of this class
	while(index < stackTrace.length
	&& stackTrace[index].getClassName().equals(getClass().getName()))
	index++;
	
	return index < stackTrace.length ? stackTrace[index] : null;
	}
	private Map<String,String> getCallerInfo(StackTraceElement caller)
	{
		HashMap<String,String> h = new HashMap<>();
		h.put("class", getCallerClass(caller));
		h.put("method", getCallerMethod(caller));
		return h;
	}
	private String getCallerClass(StackTraceElement caller)
	{
		return caller == null ? "<unknown>" : caller.getClassName();
	}
	private String getCallerMethod(StackTraceElement caller)
	{
		return caller == null ? "<unknown>" : caller.getMethodName();
	}
}