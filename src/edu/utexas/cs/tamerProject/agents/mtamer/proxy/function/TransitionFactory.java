package edu.utexas.cs.tamerProject.agents.mtamer.proxy.function;

import java.util.function.BiFunction;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

public interface TransitionFactory extends FunctionFactory<Observation, Action, Observation>
{
	@Override
	public abstract BiFunction<Observation, Action, Observation> get(Object... objs);
}
