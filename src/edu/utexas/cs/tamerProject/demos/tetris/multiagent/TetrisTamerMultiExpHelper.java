package edu.utexas.cs.tamerProject.demos.tetris.multiagent;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

import org.rlcommunity.environments.tetris.Tetris;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.actSelect.ActionSelect;
import edu.utexas.cs.tamerProject.agents.GeneralAgent;
import edu.utexas.cs.tamerProject.agents.mtamer.MoralTamerAgent;
import edu.utexas.cs.tamerProject.agents.mtamer.proxy.HumanProxy;
import edu.utexas.cs.tamerProject.agents.mtamer.proxy.MoralProxy;
import edu.utexas.cs.tamerProject.agents.mtamer.proxy.ValueProxy;
import edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.tetris.TetrisEfficiencyFunctionFactory;
import edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.tetris.TetrisMoralFunctionFactory;
import edu.utexas.cs.tamerProject.agents.mtamer.trackable.TableTrackable;
import edu.utexas.cs.tamerProject.agents.rotation.RotationAgent;
import edu.utexas.cs.tamerProject.agents.specialty.ExtActionAgentWrap;
import edu.utexas.cs.tamerProject.agents.tamer.TamerAgent;
import edu.utexas.cs.tamerProject.applet.RunLocalExperiment;
import edu.utexas.cs.tamerProject.experiments.GeneralExperiment;
import edu.utexas.cs.tamerProject.logger.Log;

/**
 * (Needs to be adapted for this class.)
 * 
 * This class creates an experiment that tests learning from predictions of
 * human reward as if it is MDP reward, calculating and acting upon a return
 * with hyperbolic discounting at varying parameters. When the discount factor
 * is 0, the condition corresponds to TAMER's actions selection strategy. When 
 * the discount factor is very high, predictions of human reward are almost 
 * being treated equivalently to MDP reward (since the domain(s) tested here 
 * is/are episodic).
 * 
 * @author bradknox
 *
 */
public class TetrisTamerMultiExpHelper extends GeneralExperiment {

	static boolean debug = true;
	public final String expPrefix = "tetriszamsterdam";
	
	/*
	 * Hard-coding indices for args String (this might be removed eventually)
	 */
	static final int EXP_NAME_I = 2;

	private static final Log log = new Log(//edit these values as desired (class, Level, less trace information)
			TetrisTamerMultiExpHelper.class, Level.FINE, Log.Simplicity.HIGH);//basic logging functionality
		
	
	public void setRunLocalExpOptions() {
		RunLocalExperiment.numEpisodes = 1000;
		RunLocalExperiment.maxStepsPerEpisode= 10000000;
		RunLocalExperiment.stepDurInMilliSecs = 10;
	}
	
	
	public TetrisTamerMultiExpHelper(){
	}
	

	/**
	 * EnvironmentInterface object that is input here should not be the same as
	 * the object used by RLGlue through RunLocalExperiment (to avoid multi-threading
	 * issues).
	 */
	public GeneralAgent createAgent(String[] args, EnvironmentInterface env) {
		boolean meld = false;
		if(!getArgument(args, "-meld").equals(""))
			meld = Boolean.parseBoolean(getArgument(args, "-meld"));
		if(meld)
		{
			String[] meldArgs = getArguments(args, "-meld", 3);
			double[] moralVals = {Double.valueOf(meldArgs[1]), Double.valueOf(meldArgs[2])};
			ActionSelect.setMoralValues(moralVals);
		}
		boolean pipe = false;
		if(!getArgument(args, "-pipe").equals(""))
			pipe = Boolean.parseBoolean(getArgument(args, "-pipe"));
		if(pipe)
			ActionSelect.setMoralFilterEnabled(false);
		boolean moralTamer = false;
		if(!getArgument(args, "-moral").equals(""))
			moralTamer = Boolean.parseBoolean(getArgument(args, "-moral"));
		this.processArgs(args);
		RotationAgent r = createTetrisMultiagent(args,2);
		addTableTracer(r, args);
		if(moralTamer)
		{
			addHumanProxy(r, args);
			for(GeneralAgent g : r)
				addEfficiencyProxy((TamerAgent)((ExtActionAgentWrap)g).coreAgent);
		}
		return r;
//		return createTetrisAgent(args);
	}
	
	public static void main(String[] args)
	{
		TetrisTamerMultiExpHelper h = new TetrisTamerMultiExpHelper();
		h.createAgent(new String[] { "-moral", "true" }, null);
	}
	
	public void addTableTracer(RotationAgent r, String[] args)
	{
		boolean moralTamer = false;
		if(!getArgument(args, "-moral").equals(""))
			moralTamer = Boolean.parseBoolean(getArgument(args, "-moral"));
		boolean finalMoralTamer = moralTamer;
		Collection<TableTrackable> agents = r.getRotation().stream().map((e -> 
		(TableTrackable)((ExtActionAgentWrap)e).coreAgent)).collect(Collectors.toList());
		DefaultTableModel model = new DefaultTableModel();
		if(moralTamer)
			model.setColumnIdentifiers(new Object[] { "Ep. No.", "Name", "MPredict", "MReal", "Reward"});
		else
			model.setColumnIdentifiers(new Object[] { "Ep. No.", "Name", "MPredict", "MReal", "Reward"});
		JTable table = new JTable(model);
		JScrollPane pane = new JScrollPane(table);
		table.setFillsViewportHeight(false);
		JButton saveButton = new JButton("Save Table");
		JFrame frame = new JFrame("History");
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileFilter() {
			public String getDescription() {
				return "Comma-Separated Values (*.csv)";
			}
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
			}
		});
		fileChooser.setAcceptAllFileFilterUsed(false);
		saveButton.addActionListener((ae) -> {
			int returnVal = fileChooser.showSaveDialog(frame);
		    if (returnVal == JFileChooser.APPROVE_OPTION) {
		        try {
		            File file = fileChooser.getSelectedFile();
		            PrintWriter os = new PrintWriter(file);
		            for (int col = 0; col < table.getColumnCount(); col++) {
		                os.print(table.getColumnName(col) + ",");
		            }
		            os.println("\r");
		            StringBuilder s = new StringBuilder();
//		            int[] offsets = finalMoralTamer ? new int[] {0, 2, 2, 3, 2} : new int[] {0, 2, 2};
		            for (int i = 0; i < table.getRowCount(); i++) {
		            	s.setLength(0);
		                for (int j = 0; j < table.getColumnCount(); j++) {
//		                	for(int k = 0; k < offsets[j]; ++k)
//		                		s.append("\t");
		                	if(j != 0)
		                		s.append(",");
		                	s.append(table.getValueAt(i, j));
		                }
		                os.println(s.toString());
		            }
		            os.close();
		        } catch (IOException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		        }
		    }
		});
		frame.add(pane, BorderLayout.CENTER);
		frame.add(saveButton, BorderLayout.SOUTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		frame.toBack();
		agents.forEach((a) -> a.setHistoryTracker(table));
	}
	
	public void addEfficiencyProxy(TamerAgent t)
	{
		TetrisEfficiencyFunctionFactory factory = new TetrisEfficiencyFunctionFactory();
		double[] weights = {1.8647976524653612E-4, 4.7699817864523865E-4, 5.761317393684146E-4, 0.0012960048535114951, 0.001175888205670327, 5.604779944838809E-4, 4.585432630056264E-4, 3.3130744387274306E-4, -2.984269223413989E-4, -1.1662245468439961E-4, 0.001222582331859393, -2.769279666844568E-4, -6.184254202017794E-5, -8.959692239557676E-4, -1.4702876999442911E-4, -4.269928915542799E-4, -1.3722928538289147E-4, -1.1422510264924284E-4, -5.716152059649105E-4, -2.2125605735739628E-4, -9.583969996364789E-4, -3.3416610252028593E-4, -3.9481478842386895E-4, 1.8295156437545444E-5, 1.2428429595175925E-4, 1.4597629244831911E-4, 3.1796239523091487E-4, 2.650147179355084E-4, 1.3229965490506903E-4, 1.171540902831774E-4, 9.831050852152286E-5, -1.0744284010047433E-4, -6.4823910416021E-5, 2.4962784900877646E-4, -0.0025516097364202085, -0.0021632982353273164, -0.012960887058589412, -0.0041878515423978665, -0.0035993262726473634, -7.045685151198493E-4, -0.0012835947045794888, -0.003448266938856314, -0.0018512506084857315, -0.014974063311022518, -0.002818467430309881, -0.005070634639744364};
		int width = 10;
		int height = 20;
		BiFunction<Observation, Observation, Double> efficiency = factory.evaluationFunc(width, height, weights);
		new ValueProxy(t, efficiency, factory.transitionFunc());
	}
	
	public void addHumanProxy(RotationAgent r, String[] args)
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
		Collection<GeneralAgent> agents = r.getRotation().stream().map((e -> 
		((ExtActionAgentWrap)e).coreAgent)).collect(Collectors.toList());
		for(GeneralAgent g : r)
		{
			System.out.println(g.getClass().getSimpleName());
		}
		MoralProxy proxy = new MoralProxy(agents, evals, factory.transitionFunc());
		String filterName = "global";
		if(!getArgument(args, "-filter").equals(""))
			filterName = getArgument(args, "-filter");
		proxy.setMoralFilter(filterName);
	}
	
	public RotationAgent createTetrisMultiagent(String[] args, int numSubagents)
	{
		System.out.println("Created "+numSubagents+" agents");
		RotationAgent agent = new RotationAgent();
		agent.envName = "Tetris";
		agent.enableGUI = false;
		agent.initParams(agent.envName);
		System.out.println("Default enableGUI: "+agent.enableGUI);
		for(int i = 0; i < Math.max(1, numSubagents); ++i)
		{
			GeneralAgent subagent = createTetrisAgent(args);
			agent.add_agent(subagent);
		}
		agent.anchor();
		Collection<GeneralAgent> agents = agent.getRotation().stream().map((e -> 
		((ExtActionAgentWrap)e).coreAgent)).collect(Collectors.toList());
		int i = 0;
		boolean pipe = false;
		double[] moralVals = {-5, 5};
		if(!getArgument(args, "-pipe").equals(""))
		{
			String[] pipeArgs = getArguments(args, "-pipe", 3);
			pipe = Boolean.parseBoolean(pipeArgs[0]);
			moralVals[0] = Double.valueOf(pipeArgs[1]);
			moralVals[1] = Double.valueOf(pipeArgs[2]);
		}
		for(GeneralAgent g : agents)
		{
			g.setUnique(String.format("#%d", i));
			++i;
			if(pipe)
			{
				((MoralTamerAgent)g).alternativeMoralPipe = (Double d) ->
				{
					switch(d.intValue())
					{
					case 1://Immoral, lower value
						g.addHRew(moralVals[0]); return;
					case 0://Moral, higher value
						g.addHRew(moralVals[1]); return;
					default:
						throw new IllegalArgumentException("Input must be a moral or non-moral value");
					}
				};
			}
		}
		return agent;
	}
	
	

	public ExtActionAgentWrap createTetrisAgent(String[] args) {
		
		ExtActionAgentWrap agent;
		agent = new ExtActionAgentWrap();
		
		boolean moralTamer = false;
		if(!getArgument(args, "-moral").equals(""))
			moralTamer = Boolean.parseBoolean(getArgument(args, "-moral"));
		agent.coreAgent = moralTamer ? new MoralTamerAgent() : new TamerAgent();

		String unique = TetrisTamerMultiExpHelper.makeUnique(args);
		//agent.setRecordRew(true); // records predictions of human reward
				
		agent.envName = "Tetris";
		agent.enableGUI = false;
		agent.coreAgent.envName = "Tetris";
		agent.coreAgent.enableGUI = false;
		((TamerAgent)agent.coreAgent).EP_END_PAUSE = 0;
		
		agent.initParams(agent.envName);
		agent.coreAgent.initParams(agent.envName);
//		agent.params = Params.getParams(agent.getClass().getName(), agent.envName);
//		agent.coreAgent.params = Params.getParams(agent.coreAgent.getClass().getName(), agent.envName);
		log.log(Level.FINE,"Tetris Agent Parameters:" + agent.params.toOneLineStr());
		
		setTamerAgentParams((TamerAgent)agent.coreAgent); // should be done before processPreInitArgs(), which might intentionally override some assignments done by this call
		agent.processPreInitArgs(args);
		
		return agent;
	}
	
	
	
		
	public void runOneExp(String[] args) {
		RunLocalExperiment runLocal = new RunLocalExperiment();
		setRunLocalExpOptions();
		
		EnvironmentInterface env = createEnv();
		GeneralAgent agent = createAgent(args, null);
		runLocal.theAgent = agent;
		runLocal.theEnvironment = env;
		
		runLocal.init();
		runLocal.initExp(); // where agent_init() is called
		
		adjustAgentAfterItsInit(args, agent);
		
		System.out.println("About to start experiment");
		runLocal.startExp();
		while (!runLocal.expFinished) {
			GeneralAgent.sleep(100);
		}
	}
	
	@Override
	public EnvironmentInterface createEnv(){
		return new Tetris();
	}
	
	public void adjustAgentAfterItsInit(String[] args, GeneralAgent agent) {
		agent.processPostInitArgs(args);
		log.log(Level.FINE,"args in adjust: " + Arrays.toString(args));
		if (!agent.getInTrainSess())
			agent.toggleInTrainSess(); // toggle ensures that member agents are also toggled on
		log.log(Level.FINE,"in training in " + this.getClass().getSimpleName() + "? " + agent.getInTrainSess());
	}
	
	
	/**
	 * Create unique string for saving the log of this experiment.
	 * 
	 * @param args
	 * @return
	 */
	public static String makeUnique(String[] args) {
		return "testUnique";
//		String[] logFilePathParts = args[TRAIN_PATH_I].split("/");
//		String logFileName = logFilePathParts[logFilePathParts.length - 1].replace(".log", "").replace("recTraj-", "");
//		System.out.println("logFileName: " + logFileName);	
//		String epOrCont = args[TASK_CONT_I].equals("-makeTaskCont") ? "cont" : "epis" ;
//		return args[DISC_PARAM_I] + "%" + args[INIT_VALUE_I] + "%" + epOrCont + "%" + logFileName;
	}

	
	public void processArgs(String[] args) {
		log.log(Level.FINE,"\n[------process pre-init args------] " + Arrays.toString(args));
		for (int i = 0; i < args.length; i++) {
    		String argType = args[i];
    		// process any arguments here
		}
	}
	

	
		
	
	/*
	 * Set the parameters for the TAMER agent. This should be done before
	 * processPreInitArgs(), which may be used to overwrite these valuse.
	 */
	public static void setTamerAgentParams(TamerAgent agent) {		
		/*
		 * Set all TamerAgent params here, not in Params class. That way you have
		 * a single location that's specific to this experiment that contains
		 * all of your algorithmic information when it's time for writing.
		 */
		System.out.println(agent.params);
		agent.param("distClass","previousStep"); //// immediate, previousStep, or uniform
		agent.param("extrapolateFutureRew",false);
		agent.param("traceDecayFactor",0.0);
		agent.param("featClass","FeatGen_Tetris");
		agent.param("modelClass","IncGDLinearModel"); 
		agent.param("modelAddsBiasFeat",true);
		agent.param("stepSize",0.000005 / 47); // python code takes input value and divides by number of features // 0.02;
		
		agent.param("delayWtedIndivRew",false); 
		agent.param("noUpdateWhenNoRew",false); 
		agent.param("selectionMethod","greedy");
	
		agent.param("initModelWSamples",false);
		agent.param("numBiasingSamples",0);
		agent.param("biasSampleWt",0.1);
		agent.param("traceDecayFactor",0.0);
	}

	
	public static String[] getDebugArgsStrArray() {
		String[] args = new String[0];
		return args;
	}
	
	
	
	
//	public static void main(String[] args) {
//		if (TetrisTamerMultiExpHelper.debug) {
//			args = getDebugArgsStrArray();
//		}
//		
//		TetrisTamerMultiExpHelper exp = new TetrisTamerMultiExpHelper();
//		exp.runOneExp(args);
//	}


	public void processTrainerUnique(GeneralAgent agent, String trainerUnique) {
		// only one condition in this Tetris exp
	}
	
	private String getArgument(String[] args, String arg)
	{
		for(int i = 0; i < args.length; ++i)
			if(args[i].equals(arg))
				return args[i+1];
		return "";
	}
	
	private String[] getArguments(String[] args, String arg, int numArguments)
	{
		for(int i = 0; i < args.length; ++i)
			if(args[i].equals(arg))
			{
				String[] ret = new String[numArguments];
				for(int j = 0; j < numArguments; ++j)
					ret[j] = args[i+1+j];
				return ret;
			}
		return null;
	}
}
