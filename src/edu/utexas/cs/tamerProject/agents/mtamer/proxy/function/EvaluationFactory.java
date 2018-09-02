package edu.utexas.cs.tamerProject.agents.mtamer.proxy.function;

import java.util.function.BiFunction;

import org.rlcommunity.rlglue.codec.types.Observation;

public interface EvaluationFactory extends FunctionFactory<Observation, Observation, Double>
{
	@Override
	public abstract BiFunction<Observation, Observation, Double> get(Object... objs);
}
