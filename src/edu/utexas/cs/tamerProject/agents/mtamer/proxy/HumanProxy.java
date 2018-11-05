package edu.utexas.cs.tamerProject.agents.mtamer.proxy;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.agents.mtamer.MoralTamerAgent;

public interface HumanProxy 
{
	/**
	 * If moral, 
	 */
	double notify(MoralTamerAgent moralTamerAgent, Observation o, Action act);
}
