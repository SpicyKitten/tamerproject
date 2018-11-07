package edu.utexas.cs.tamerProject.agents.mtamer.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.rlcommunity.environments.tetris.TetrisState;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.agents.GeneralAgent;
import edu.utexas.cs.tamerProject.agents.mtamer.MoralTamerAgent;
import edu.utexas.cs.tamerProject.agents.mtamer.moral.MoralAgent;
import edu.utexas.cs.tamerProject.agents.mtamer.proxy.filter.MoralFilter;
import edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.tetris.TetrisMoralFunctionFactory;
import edu.utexas.cs.tamerProject.agents.rotation.RotationAgent;

public class MoralProxy implements HumanProxy
{
	//maybe useful for someone in the future
	private List<GeneralAgent> myAgents;
	private int agent_index;
	//maybe used for a running morality check
	private double[] evalAtCurrStep;
	private BiFunction<double[], Integer, Double> moralFilter = MoralFilter.getMoralFilter("global");
	private ArrayList<BiFunction<Observation, Observation, Double>> evalFunctions;
	private BiFunction<Observation, Action, Observation> transitionFunction;
	
	public MoralProxy(GeneralAgent generalAgent, Collection<BiFunction<Observation, Observation, Double>> evaluators,
			BiFunction<Observation, Action, Observation> mapping)
	{
		assert generalAgent instanceof MoralAgent : "Tried to set a proxy for a non-moral agent!";
		assert !(generalAgent instanceof RotationAgent) : "Tried to do single agent proxy for multi-agent!";
		evalAtCurrStep = new double[1];
		evalFunctions = new ArrayList<>();
		evaluators.forEach(evalFunctions::add);
		assert evalFunctions.size() == 1 : "Incorrect number of evaluation functions provided!";
		transitionFunction = mapping;
		((MoralAgent)generalAgent).setProxy(this, ProxyType.MORAL);
		myAgents = new ArrayList<>();
		myAgents.add(generalAgent);
		agent_index = 0;
	}
	
	public MoralProxy(GeneralAgent generalAgent, Collection<BiFunction<Observation, Observation, Double>> evaluators,
			BiFunction<Observation, Action, Observation> mapping, BiFunction<double[], Integer, Double> filter)
	{
		this(generalAgent, evaluators, mapping);
		assert filter != null : "If you were going to provide a filter yourself, at least don't give a null one!!!";
		this.moralFilter = filter;
	}
	
	public MoralProxy(Collection<GeneralAgent> agents, Collection<BiFunction<Observation, Observation, Double>> evaluators,
			BiFunction<Observation, Action, Observation> mapping)
	{
		for(GeneralAgent generalAgent : agents)
		{
			assert generalAgent instanceof MoralAgent : "Tried to set a proxy for non-moral agent!";
		}
		evalAtCurrStep = new double[agents.size()];
		evalFunctions = new ArrayList<>();
		evaluators.forEach(evalFunctions::add);
		assert evalFunctions.size() == agents.size() : "Incorrect number of evaluation functions provided!";
		transitionFunction = mapping;
		for(GeneralAgent generalAgent : agents)
			((MoralAgent)generalAgent).setProxy(this, ProxyType.MORAL);
		myAgents = new ArrayList<>(agents);
		agent_index = 0;
	}
	
	public MoralProxy(Collection<GeneralAgent> agents, Collection<BiFunction<Observation, Observation, Double>> evaluators,
			BiFunction<Observation, Action, Observation> mapping, BiFunction<double[], Integer, Double> filter)
	{
		this(agents, evaluators, mapping);
		assert filter != null : "If you were going to provide a filter yourself, at least don't give a null one!!!";
		this.moralFilter = filter;
	}

	/**
	 * 
	 * @param moralAgent An agent that needs moral feedback on episodes
	 * @param initial
	 * @param act
	 */
	public double notify(MoralAgent moralAgent, Observation initial, Action act) 
	{
		//Don't use RotationAgent to get the current moral agent because the
		//current agent in RotationAgent may be different by the time that
		//the agent_step function is called
		assert moralAgent instanceof GeneralAgent : "notify() sourced in a moral agent using an unusual superclass!";
		Observation terminal = transitionFunction.apply(initial, act);
//		RotationAgent r_agent = multiAgent ? (RotationAgent)this.agent : null;
		//feedbackVal is the eval function change sums
		double[] agent_evals = new double[myAgents.size()];
		for(int eval_index = 0; eval_index < myAgents.size(); ++eval_index)
		{
			//each evalFunction gives a change in evaluations between initial and terminal states of a step
			agent_evals[eval_index] = getEval(eval_index).apply(initial, terminal);;
		}
		int eval_index = this.getIndex(moralAgent);
		assert eval_index != -1 : "notify() sourced in a moral agent unknown to the current human proxy!";
		double feedbackVal = moralFilter.apply(agent_evals, eval_index);
		System.out.println("Proxy value: " + feedbackVal);
		//reverse notify!
		if(feedbackVal >= 0)
		{
			moralAgent.addMRew(moralAgent.moralFeedbackValue());
			return moralAgent.moralFeedbackValue();
		}
		else
		{
			moralAgent.addMRew(moralAgent.immoralFeedbackValue());
			return moralAgent.immoralFeedbackValue();
		}
	}
	
	private BiFunction<Observation, Observation, Double> getEval(int i)
	{
		assert evalFunctions != null : "No eval functions in getEval()!";
		assert i >= 0 && i < evalFunctions.size() : "Index "+i+" out of bounds in getEval()!";
		if(evalFunctions.get(i) == null)
			return (o, a) -> 0.0;
		return evalFunctions.get(i);
	}
	
	private int getIndex(MoralAgent agent)
	{
		for(int offset = 0; offset < myAgents.size(); ++offset) 
			if(myAgents.get((agent_index + offset) % myAgents.size()) == agent)
			{
				agent_index = (agent_index + offset) % myAgents.size();
				return agent_index;
			}
		return -1;
	}
	
	public void setMoralFilter(BiFunction<double[], Integer, Double> filter)
	{
		this.moralFilter = filter;
	}
	
	public void setMoralFilter(String filterName)
	{
		this.setMoralFilter(MoralFilter.getMoralFilter(filterName));
	}
	
	public static void main(String[] args)
	{
		TetrisMoralFunctionFactory factory = new TetrisMoralFunctionFactory();
		Collection<BiFunction<Observation, Observation, Double>> evals = new ArrayList<>();
		int[][][] evalParams = new int[][][] 
				{
				{{1,2},{2,3},{1,3},{-1,-1}},
				{{2,3},{1,3},{-1,-1},{1,2}}
				};
		int width = 10;
		int height = 20;
		int[] weights = new int[] {1, 2, -1, -2};
		for(int[][] evalParam : evalParams)
		{
			evals.add(factory.evaluationFunc(evalParam, weights, width, height));
		}
		
		TetrisState t = new TetrisState();
		RotationAgent g = new RotationAgent();
		g.add_agent(new MoralTamerAgent());
		g.add_agent(new MoralTamerAgent());
		MoralProxy h = new MoralProxy(g.getRotation(), evals, factory.transitionFunc());
		h.setMoralFilter(MoralFilter.getMoralFilter("relative"));
		Action a = new Action(1,0);
		a.intArray[0] = 5;
		//h.notify((MoralAgent)g.getAgent(0), t.get_observation(), a);
		Observation o = t.get_observation();
		for(int i = 0; i < 10; i++)
		{
			displayTetrisState(t);
			Observation prev = o.duplicate();
			o = h.transitionFunction.apply(t.get_observation(), a);
			int evalNo = 1;
			double[] agent_evals = new double[g.numAgents()];
			int eval_index = -1;
			for(BiFunction<Observation, Observation, Double> eval : evals)
			{
				System.out.println("Eval "+(evalNo++)+": "+eval.apply(prev, o));
				agent_evals[++eval_index] = eval.apply(prev, o);
			}
			double feedbackVal = h.moralFilter.apply(agent_evals, 0);
			System.out.println("Feedback: "+feedbackVal);
			t = setStateFromObs(o);
		}
		displayTetrisState(t);
	}
	
	private static TetrisState setStateFromObs(Observation obs) {
		edu.utexas.cs.tamerProject.featGen.tetris.TetrisState gameState = new edu.utexas.cs.tamerProject.featGen.tetris.TetrisState();
		for (int i = 0; i < gameState.worldState.length; i++)
			gameState.worldState[i] = obs.intArray[i];
        gameState.blockMobile = obs.intArray[gameState.worldState.length] == 1;
        gameState.currentBlockId = obs.intArray[gameState.worldState.length + 1];
	    gameState.currentRotation = obs.intArray[gameState.worldState.length + 2];
    	gameState.currentX = obs.intArray[gameState.worldState.length + 3];
    	gameState.currentY = obs.intArray[gameState.worldState.length + 4];
	    gameState.worldWidth = obs.intArray[gameState.worldState.length + 5];
        gameState.worldHeight = obs.intArray[gameState.worldState.length + 6];
        gameState.currentBlockColorId = obs.intArray[gameState.worldState.length + 7];
        TetrisState state = new TetrisState();
        state.blockMobile = gameState.blockMobile;
        state.currentBlockId = gameState.currentBlockId;
        state.currentBlockColorId = gameState.currentBlockColorId;
        state.currentRotation = gameState.currentRotation;
        state.currentX = gameState.currentX;
        state.currentY = gameState.currentY;
        state.score = gameState.score;
        state.is_game_over = gameState.is_game_over;
        state.worldWidth = gameState.worldWidth;
        state.worldHeight = gameState.worldHeight;

        state.worldState = new int[gameState.worldState.length];
        for (int i = 0; i < state.worldState.length; i++) {
            state.worldState[i] = gameState.worldState[i];
        }
        return state;
	}
	
	private static void displayTetrisState(TetrisState t)
	{
		for(int i = 0; i < t.worldHeight; i++)
		{
			for(int j = 0; j < t.worldWidth; j++)
			{
				System.out.print("[" + t.worldState[i*t.worldWidth+j] + "]");
			}
			System.out.println();
		}
		System.out.println();
	}
	
	private static void displayTetrisState(edu.utexas.cs.tamerProject.featGen.tetris.TetrisState t)
	{
		for(int i = 0; i < t.worldHeight; i++)
		{
			for(int j = 0; j < t.worldWidth; j++)
			{
				System.out.print("[" + t.worldState[i*t.worldWidth+j] + "]");
			}
			System.out.println();
		}
		System.out.println();
	}
}
