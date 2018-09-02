package edu.utexas.cs.tamerProject.agents.mtamer;

import edu.utexas.cs.tamerProject.agents.tamer.HRew;

public class MRew extends HRew
{
	public MRew()
	{
		super();
	}
	
	public MRew(double feedbackVal, double comparableTimeInSec) 
	{
		super(feedbackVal, comparableTimeInSec);
	}
	
	public String morality(double val)
	{
		return val == 0 ? "Moral" : "Immoral";
	}
	
	public String toString()
	{
		return "{" + morality(val) + " @ " + String.format("%f", this.time) + "}";
	}
}