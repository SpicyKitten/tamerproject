package edu.utexas.cs.tamerProject.agents.mtamer.moral;

import java.util.ArrayList;

import edu.utexas.cs.tamerProject.agents.mtamer.MRew;
import edu.utexas.cs.tamerProject.agents.mtamer.proxy.HumanProxy;
import edu.utexas.cs.tamerProject.agents.tamer.HRew;
import edu.utexas.cs.tamerProject.featGen.FeatGenerator;
import edu.utexas.cs.tamerProject.utils.Stopwatch;

public interface MoralAgent 
{
	public default void addMRew(double feedbackVal)
	{
		ArrayList<HRew> moralRewards = this.getNRewList("moralRew");
		//only the most recent moral reward is relevant
		if(moralRewards.size() > 0)
			moralRewards.set(0, (HRew)new MRew(feedbackVal, Stopwatch.getComparableTimeInSec()));
		else
			moralRewards.add((HRew)new MRew(feedbackVal, Stopwatch.getComparableTimeInSec()));
	}
	
	public default void setMoralFeatGen(String string)
	{
		//subclasses of MoralAgent may find it necessary to have a different
		//feature generation for morality versus performance
	}
	
	public abstract ArrayList<HRew> getNRewList(String name);
	public abstract double moralFeedbackValue();
	public abstract double immoralFeedbackValue();
	public abstract void setProxy(HumanProxy proxy);
}
