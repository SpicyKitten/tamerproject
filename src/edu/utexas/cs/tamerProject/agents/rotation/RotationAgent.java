package edu.utexas.cs.tamerProject.agents.rotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.agents.GeneralAgent;
import edu.utexas.cs.tamerProject.agents.specialty.ExtActionAgentWrap;
import edu.utexas.cs.tamerProject.agents.specialty.ExtActionWrapper;
import edu.utexas.cs.tamerProject.agents.tamer.TamerAgent;
import edu.utexas.cs.tamerProject.experimentTools.RecordHandler;
import edu.utexas.cs.tamerProject.logger.Log;
import edu.utexas.cs.tamerProject.params.Params;
import rlVizLib.general.ParameterHolder;

/**
 * Multi-agent agent. Given a sequence of GeneralAgents, provided in
 * sequence or individually, this agent will sequentially request
 * action-state resolution by the GeneralAgents in turn. Doesn't
 * entirely behave according to the expectations of a GeneralAgent,
 * but ideally makes it easier to interact with multi-agent 
 * scenarios.
 * TODO:Properly logging events with multiple agents going on at the
 * same time may or may not work depending on how actions are logged 
 * @author ratha
 */
public class RotationAgent extends GeneralAgent implements AgentInterface, Iterable<GeneralAgent>
{
	private int agent_index = -1;
	private int steps_permitted = 3;
	private int steps_taken = steps_permitted;
	private BitSet activated_agents = null;
	private ArrayList<GeneralAgent> rotation = null;
	private boolean fixed_rotation = false;
	public double default_reward = 0.0;
	
	private static final char[] charCodes = {
		' '
	};
	private static final Log log = new Log(//edit these values as desired (class, Level, less trace information)
			RotationAgent.class, Level.FINE, Log.Simplicity.HIGH);//basic logging functionality
	
	public RotationAgent(ParameterHolder p)
	{
		rotation = new ArrayList<GeneralAgent>();
		activated_agents = new BitSet();
	}
	
	public RotationAgent()
	{
		this(getDefaultParameters());
	}
	
	/**
	 * Creates a RotationAgent with rotation equal
	 * to the given parameter, that has not yet been
	 * anchored.
	 * @param agents
	 */
	public RotationAgent(GeneralAgent... agents)
	{
		this(getDefaultParameters(), agents);
	}
	
	/**
	 * Creates a RotationAgent with rotation equal
	 * to the given parameter, that has not yet been
	 * anchored.
	 * @param p
	 * @param agents
	 */
	public RotationAgent(ParameterHolder p, GeneralAgent... agents)
	{
		rotation = new ArrayList<GeneralAgent>();
		for(GeneralAgent generalAgent : agents)
		{
			rotation.add(generalAgent);
		}
		activated_agents = new BitSet();
	}
	
	/**
	 * Sets the number of steps allocated to each agent
	 * before beginning to ask the next agent for actions
	 * @param n
	 */
	public void setStepsPermitted(int n)
	{
		steps_permitted = n;
	}
	
	/**
	 * @return The number of steps allocated to each agent
	 * before beginning to ask the next agent for actions
	 */
	public int getStepsPermitted()
	{
		return steps_permitted;
	}
	
	/**
	 * Adds an agent to the rotation
	 * @param generalAgent The agent to be added
	 * @return Whether the addition was successful
	 */
	public boolean add_agent(GeneralAgent generalAgent)
	{
		if(fixed_rotation || generalAgent == null)
			return false;
		rotation.add(generalAgent);
		return true;
	}
	
	/**
	 * Adds multiple agents to the rotation
	 * @param agents The agents to be added
	 * @return Whether the addition was successful
	 */
	public boolean add_agents(GeneralAgent... agents)
	{
		if(fixed_rotation)
			return false;
		for(GeneralAgent generalAgent : agents)
			if(generalAgent == null) return false;
		for(GeneralAgent generalAgent : agents)
		{
			rotation.add(generalAgent);
		}
		return true;
	}
	
	/**
	 * Returns access to an agent without providing
	 * direct access to the rotation itself. Allows
	 * for more customizable interaction with the
	 * rotation's agents; for example, one use could
	 * be to invoke processPreInitArgs when the 
	 * arguments in question vary by agent. However
	 * in such instances it is recommended that such
	 * methods be applied to agents before constructing
	 * the RotationAgent or adding them to an already
	 * constructed RotationAgent.
	 * @param index Which agent to get
	 * @return Copy by reference to the agent
	 * specified by index
	 */
	public GeneralAgent getAgent(int index)
	{
		return rotation.get(index);
	}
	
	/**
	 * Processes arguments to the agent by dispatching
	 * the same arguments to all the sub-agents in the 
	 * rotation. Particularly useful if all the agents
	 * are of same/similar types or respond to the particular
	 * parameters in the same way.
	 */
	public void processPreInitArgs(String[] args) 
	{
		//super.processPreInitArgs(args);//???
		for(GeneralAgent agent : rotation)
		{
			agent.processPreInitArgs(args);
		}
	}
	
	/**
	 * Dispatches post-init arguments to all sub-agents
	 */
	public void processPostInitArgs(String[] args) 
	{
		//super.processPostInitArgs(args);//???
		for(GeneralAgent agent : rotation)
		{
			agent.processPostInitArgs(args);
		}
	}
	
	/**
	 * Processes the task specifications by handing
	 * them over to all agents within the rotation
	 * @throws IllegalStateException If the rotation
	 * can still be modified.
	 */
	public void agent_init(String taskSpec) {
		if(!fixed_rotation)
			throw new IllegalStateException("Attempted to specify the task "
					+ "without calling anchor()");
		for(GeneralAgent agent : rotation)
		{
			log.log(Level.FINE,"Init-ing agent "+rotation.indexOf(agent));
			agent.agent_init(taskSpec);
		}
		//this helper method is allowed since it doesn't connect to the superclass abstract method
		super.agent_init(taskSpec, this);
		//if something else needs to be added, do so here:
		/** code that does something **/
	}

	/**
	 * Begins an episode.
	 */
	public Action agent_start(Observation o, double time, Action predeterminedAct) {
		if(!fixed_rotation)
			throw new IllegalStateException("Attempted to start the agent "
					+ "without calling anchor()");
		this.startHelper();
		this.lastStepStartTime = -10000000;//imitating TamerAgent
		//maybe call agent_step here after advancing the current agent properly??
		return agent_step(0, o, time, predeterminedAct);
	}

	public Action agent_step(double r, Observation o, double startTime, Action predeterminedAct) {
		//might be called from agent_start in which case just dispatch stuff here
		//otherwise might just have to advance and dispatch both here, and
		//just advance and call agent_start there
		if(!fixed_rotation)
			throw new IllegalStateException("Attempted to use the agent "
					+ "without calling anchor()");
		stepStartTime = startTime;//imitating TamerAgent
		this.stepStartHelper(r, o);
		steps_taken++;//how many steps on the current agent
		if(agent_index >= 0 && rotation.get(agent_index) instanceof ExtActionWrapper
				&& !((ExtActionWrapper)rotation.get(agent_index)).actionExhausted())
		{
			steps_taken--;
		}
		if(steps_taken >= steps_permitted)
		{
			steps_taken = 0;
			agent_index = (agent_index + 1) % rotation.size();
			log.log(Level.FINE,"Agent "+agent_index+"'s turn ("+getAgent(agent_index).getClass().getSimpleName()+"):");
		}
		Action requestedAction = null;
		if(!activated_agents.get(agent_index))
		{
			activated_agents.set(agent_index);
			log.log(Level.FINER,"Invoked agent"+agent_index+".agent_start("+r+")");
			requestedAction = getAgent(agent_index).agent_start(o, startTime, predeterminedAct);
		}
		else
		{
			log.log(Level.FINER,"Invoked agent"+agent_index+".agent_step("+r+")");
			requestedAction = getAgent(agent_index).agent_step(r, o, startTime, predeterminedAct);
		}
		currObsAndAct.setAct(predeterminedAct);
		if(currObsAndAct.actIsNull())
		{
			currObsAndAct.setAct(requestedAction);
		}
		lastStepStartTime = stepStartTime;//imitating TamerAgent
		this.stepEndHelper(r, o);
		return currObsAndAct.getAct();
	}

	/**
	 * Ends all agent episodes with reward default_reward, except for the
	 * current agent, which is given the reward r at termination. Might
	 * introduce a slight error in situations where agents get a per-agent
	 * reward update according to the MDP. Ends an episode.
	 */
	public void agent_end(double r, double time) {
		// not sure when this is called
		// dispatch to current agent, probably
		// sounds like it happens at the end of an episode or something
		if(!fixed_rotation)
			throw new IllegalStateException("A RotationAgent cannot be in an episode "
					+ "without calling anchor()");
		for(int i = activated_agents.nextSetBit(0); i >= 0; i = activated_agents.nextSetBit(i+1))
		{
			double reward = (i == agent_index) ? r : default_reward;
			log.log(Level.FINER,"Invoked agent"+i+".agent_end("+reward+")");
			getAgent(i).agent_end(reward, time);
		}
		activated_agents.clear();
		// resetting which agent is at hand
		agent_index = -1;
		steps_taken = steps_permitted;
		this.endHelper(r);
		return;
	}
	
	/**
	 * Sets the agents in the rotation permanently
	 * @return True, if the agents can rotate (>1 agent)
	 *         False, if the agents cannot rotate (1 agent)
	 * @throws IllegalStateException If no agents have been added
	 */
	public boolean anchor() throws IllegalStateException
	{
		if(rotation == null || rotation.size() == 0)
			throw new IllegalStateException(String.format("Attempted to anchor RotationAgent (@%x) "
					+ "with an invalid agent rotation", 
					System.identityHashCode(this)));
		if(fixed_rotation) 
			return false;
		else if(rotation.size() > 1 && (fixed_rotation = true))
			return true;
		else 
			return false;
	}
	
	/**
	 * Conveniently dispatches initParams to this agent and
	 * all sub-agents in the rotation as per environment envName
	 */
	public void initParams(String envName)
	{
    	params = Params.getParams(getClass().getName(), envName);
    	for(GeneralAgent generalAgent : rotation)
    	{
    		generalAgent.initParams(envName);
    	}
    }
	
	/**
	 * Dispatch agent cleanup tasks
	 */
	public void agent_cleanup() {
		for(GeneralAgent generalAgent : rotation)
		{
			generalAgent.agent_cleanup();
		}
		if(rotation != null)
			rotation.clear();
		rotation = null;
		agent_index = -1;
		fixed_rotation = true; //this agent is done
		if(activated_agents != null)
			activated_agents.clear();
		activated_agents = null;
		super.agent_cleanup();
	}
	
	/** May be verbose **/
	public void toggleInTrainSess()
	{
		//Keeping information accurate for this agent
		this.inTrainSess = !this.inTrainSess;
		//Keeping information accurate for sub-agents
		for(GeneralAgent generalAgent : rotation)
			generalAgent.toggleInTrainSess();
	}
	
	/** May be verbose **/
	public void togglePause()
	{
		//Keeping information accurate for this agent
		this.pause = !this.pause;
		//Keeping information accurate for sub-agents
		for(GeneralAgent generalAgent : rotation)
			generalAgent.togglePause();
	}
	
	//TODO: Add more methods that must have their events dispatched
	
	/**
	 * Dispatches key input to the currently activated agent
	 */
	public void receiveKeyInput(char c)
	{
		if(!fixed_rotation || agent_index < 0)
			return;
		boolean allAgentCharCode = false;
		for(char code : RotationAgent.charCodes)
			if(c == code)
				allAgentCharCode = true;
		if(!allAgentCharCode)
			rotation.get(agent_index).receiveKeyInput(c);
		else
			rotation.forEach(agent -> agent.receiveKeyInput(c));
	}
	
	/**
	 * Returns the RecordHandler associated with the currently
	 * activated agent
	 */
	public RecordHandler getRecHandler()
	{
		if(!fixed_rotation)
			return null;
		return rotation.get(agent_index).getRecHandler();
	}
	
	/**
	 * Returns all RecordHandlers associated with the current
	 * rotation of agents, in order.
	 * @return List of RecordHandlers
	 */
	public List<RecordHandler> getRecHandlers()
	{
		if(!fixed_rotation)
			return null;
		List<RecordHandler> handlers = new ArrayList<>();
		for(GeneralAgent generalAgent : rotation)
		{
			handlers.add(generalAgent.getRecHandler());
		}
		return handlers;
	}
	
	/**
	 * Dispatches calls of setRecordLog to all sub-agents
	 * and the current agent.
	 */
	public void setRecordLog(boolean recordLog)
	{
		super.setRecordLog(recordLog);
		for(GeneralAgent generalAgent : rotation)
		{
			generalAgent.setRecordLog(recordLog);
		}
	}
	
	/**
	 * Dispatches calls of setRecordRew to all sub-agents
	 * and the current agent.
	 */
	public void setRecordRew(boolean recordRew)
	{
		super.setRecordRew(recordRew);
		for(GeneralAgent generalAgent : rotation)
		{
			generalAgent.setRecordRew(recordRew);
		}
	}
	
	/**
	 * @return The acting agent's index
	 */
	public int currentAgent()
	{
		return agent_index;
	}
	
	/**
	 * @return The number of agents in the current rotation
	 */
	public int numAgents()
	{
		if(rotation == null)
			throw new IllegalStateException("Tried to get the number of agents in a nonexistent rotation");
		return rotation.size();
	}
	
	/**
	 * Sets unique identifiers for all agents, if you want
	 */
	public void setUniques(String format)
	{
		for(int i = 0; i < rotation.size(); ++i)
		{
			rotation.get(i).setUnique(String.format(format, i));
		}
	}
	
	/**
	 * @return Unmodifiable copy of the rotation
	 */
	public Collection<GeneralAgent> getRotation()
	{
		return Collections.unmodifiableList(rotation);
	}

	@Override
	public Iterator<GeneralAgent> iterator() {
		return (Collections.unmodifiableList(rotation).iterator());
	}
	
	public static void main(String[] args)
	{
		ExtActionAgentWrap e = new ExtActionAgentWrap();
		e.coreAgent = new TamerAgent();
		e.envName = "Tetris";
		e.coreAgent.envName = "Tetris";
		e.coreAgent.initParams("Tetris");
		RotationAgent r = new RotationAgent(e, new TamerAgent());
		r.initParams("Tetris");
		r.envName = "Tetris";
		boolean b = r.anchor();
		for(GeneralAgent g : r)
		{
			System.out.println("g.type: "+g.getClass().getSimpleName());
		}
		r.agent_init("VERSION RL-Glue-3.0 PROBLEMTYPE episodic DISCOUNTFACTOR 1.0 OBSERVATIONS INTS (200 0 1)  (0 1)  (0 7)  (0 4)  (0 9)  (0 19)  (20 20)  (10 10)  ACTIONS INTS (0 4)  REWARDS (0.0 8.0)  EXTRA EnvName:Tetris HEIGHT:20 WIDTH:10 Revision: null");
		System.out.println("envName: "+r.envName);
		Observation o = new Observation();
		o.intArray = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 4, 0, 3, -1, 10, 20};
		r.agent_start(o);
		//making 1+19 calls = 20 calls
		for(int i = 0; i < e.getExtendedActionAsIntArray().length; ++i)
		{
			System.out.println("Step "+i+": "+Arrays.toString(r.agent_step(0, o).intArray));
		}
		System.out.println("Step back to first agent: "+Arrays.toString(r.agent_step(0, o).intArray));
		r.agent_end(0);
		r.agent_cleanup();
		System.out.println(b);
	}

}
