package edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.tetris;

import java.util.function.BiFunction;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.EnvironmentFunctionFactory;

public class TetrisFunctionFactory extends EnvironmentFunctionFactory
{
	public TetrisFunctionFactory()
	{
		this.addConstructor("transition", TetrisTransitionFactory::new);
		this.addConstructor("evaluation", varArgs(TetrisEvaluationFactory::new));
		//this.addConstructor("empty_evaluation", noArgs(TetrisEvaluationFactory::new));
	}
	
	/**
	 * Provides an evaluation function for the Tetris environment generated with parameters 
	 * <code>args</code>
	 */
	@Override
	public BiFunction<Observation, Observation, Double> evaluationFunc(Object... args) {
		return this.get("evaluation", args);
	}

	/**
	 * Provides a transition function for the Tetris environment generated with parameters 
	 * <code>args</code>
	 */
	@Override
	public BiFunction<Observation, Action, Observation> transitionFunc(Object... args) {
		return this.get("transition", args);
	}

}