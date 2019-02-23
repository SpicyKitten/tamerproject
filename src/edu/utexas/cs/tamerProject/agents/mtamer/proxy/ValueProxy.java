package edu.utexas.cs.tamerProject.agents.mtamer.proxy;

import java.util.function.BiFunction;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.agents.GeneralAgent;
import edu.utexas.cs.tamerProject.agents.mtamer.moral.MoralAgent;
import edu.utexas.cs.tamerProject.agents.rotation.RotationAgent;
import edu.utexas.cs.tamerProject.agents.tamer.TamerAgent;

public class ValueProxy implements HumanProxy
{
	//maybe useful for someone in the future
	private GeneralAgent myAgent;
	//maybe used for a running morality check
	private BiFunction<Observation, Observation, Double> evalFunction;
	private BiFunction<Observation, Action, Observation> transitionFunction;
	
	//ValueProxy has only a single-agent constructor. This is because the multiple-agent constructor
	//implies knowledge of the other agents in a team. While this is consistent for the framework
	//in making decisions about morality, an agent does not need to know about the presence of other
	//agents for the purposes of getting closer to its own optimal value function.
	public ValueProxy(GeneralAgent generalAgent, BiFunction<Observation, Observation, Double> evaluator,
			BiFunction<Observation, Action, Observation> mapping)
	{
		assert generalAgent instanceof MoralAgent : "Tried to set a proxy for a non-moral agent!";
		assert !(generalAgent instanceof RotationAgent) : "Tried to do single agent proxy for multi-agent!";
		evalFunction = evaluator;
		transitionFunction = mapping;
		((MoralAgent)generalAgent).setProxy(this);
		myAgent = generalAgent;
	}

	/**
	 * 
	 * @param moralAgent An agent that needs value feedback on episodes
	 * @param initial
	 * @param act
	 */
	public double notify(MoralAgent moralAgent, Observation initial, Action act) 
	{
		//Only TAMER agents will need to be notified of their efficiency feedback - non-TAMER agents don't use this data
		assert moralAgent instanceof TamerAgent : "notify() sourced in a moral agent using an unusual superclass!";
		assert myAgent == moralAgent : "notify() executed on the wrong agent!";
		Observation terminal = transitionFunction.apply(initial, act);
		//feedbackVal is the optimal eval function change
		double feedbackVal = evalFunction.apply(initial, terminal);
//		System.out.println("Proxy value: " + feedbackVal);
		//reverse notify!
		((TamerAgent)moralAgent).addHRew(feedbackVal);
		return feedbackVal;
	}

	@Override
	public ProxyType getProxyType() {
		return ProxyType.VALUE;
	}
	
}
