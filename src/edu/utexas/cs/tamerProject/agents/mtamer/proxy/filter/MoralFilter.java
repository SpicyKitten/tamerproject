package edu.utexas.cs.tamerProject.agents.mtamer.proxy.filter;

import java.util.function.BiFunction;

public class MoralFilter 
{
	/**
	 * Gets the moral filter associated with the provided String parameter 
	 * @param filterName fixed, global, simple, or relative
	 * @return The associated moral filter if morality is in the domain, else <code>null</code>
	 */
	public static BiFunction<double[], Integer, Double> getMoralFilter(String filterName)
	{
		switch(filterName)
		{
		case "fixed":
			return MoralFilter::fixed;
		case "global":
			return MoralFilter::global;
		case "simple":
			return MoralFilter::simple;
		case "relative":
			return MoralFilter::relative;
		default:
			throw new IllegalArgumentException("Unknown definition of morality("+filterName+")!");
		}
	}
	public static double fixed(double[] evals, int agent_index)
	{
		return 0;
	}
	public static double global(double[] evals, int agent_index)
	{
		double sum = 0;
		for(int i = 0; i < evals.length; ++i)
			sum += evals[i];
		return sum;
	}
	public static double simple(double[] evals, int agent_index)
	{
		for(int i = 0; i < evals.length; ++i)
		{
			if(i == agent_index)
				continue;
			if(evals[i] < 0)
				return -1;
		}
		return 1;
	}
	public static double relative(double[] evals, int agent_index)
	{
		for(int i = 0; i < evals.length; ++i)
		{
			if(i == agent_index)
				continue;
			if(evals[i] < 0 && Math.abs(evals[agent_index]) + evals[i] < 0)
				return -1;
		}
		return 1;
	}
}
