package edu.utexas.cs.tamerProject.agents.mtamer.proxy;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.agents.mtamer.moral.MoralAgent;

public interface HumanProxy 
{
	/**
	 * If moral, give moral feedback and return the feedback
	 * If value, give efficiency feedback and return the feedback
	 * @param o The observation corresponding to the initial state of the problem
	 * @param act The action that is going to be applied to the initial state
	 * @return The feedback calculated by the proxy
	 */
	double notify(MoralAgent moralAgent, Observation o, Action act);
}
