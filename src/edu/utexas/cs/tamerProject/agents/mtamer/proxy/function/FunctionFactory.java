package edu.utexas.cs.tamerProject.agents.mtamer.proxy.function;

import java.util.function.BiFunction;

public interface FunctionFactory<A, B, C>
{
	/**
	 * Generates a BiFunction from parameters <code>objs</code>
	 */
	public abstract BiFunction<A, B, C> get(Object... objs);
}
