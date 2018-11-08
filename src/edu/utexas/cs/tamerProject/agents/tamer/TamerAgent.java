/*
Adapted by Brad Knox from RandomAgent.java by Brian Tanner

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package edu.utexas.cs.tamerProject.agents.tamer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

import edu.utexas.cs.tamerProject.actSelect.ActionSelect;
import edu.utexas.cs.tamerProject.agents.CreditAssignParamVec;
import edu.utexas.cs.tamerProject.agents.GeneralAgent;
import edu.utexas.cs.tamerProject.agents.HLearner;
import edu.utexas.cs.tamerProject.agents.mtamer.trackable.TableTrackable;
import edu.utexas.cs.tamerProject.logger.Log;
import edu.utexas.cs.tamerProject.modeling.SampleWithObsAct;
import edu.utexas.cs.tamerProject.trainInterface.TrainerListener;
import edu.utexas.cs.tamerProject.utils.Stopwatch;
import rlVizLib.general.ParameterHolder;
import rlVizLib.general.hasVersionDetails;


/**
 * This class flexibly implements a TAMER agent, which learns from human 
 * reward, input by keyboard.  
 * 
 * @author bradknox
 *
 */
public class TamerAgent extends GeneralAgent implements AgentInterface, TableTrackable {

	public HLearner hLearner;
	protected double lastStepStartTime;

	public TrainerListener trainerListener;
	public JTable histTable;
	
	/*
	 * Time of agent pause at end of episode in milliseconds, where
	 * agent simply waits to finish agent_end(), which TinyGlueExtended
	 * will wait for. This pause can be used to allow the trainer to 
	 * add reward or punishment at the ends of episodes. 
	 * 
	 *  *** When possible, create the pause in RunLocalExperiment
	 *  instead through its static variable PAUSE_DUR_AFTER_EP. ****
	 */
	public int EP_END_PAUSE = 0; //2000; /
	protected SampleWithObsAct[] lastLearningSamples;
	public static boolean verifyObsFitsEnvDesc = true;
	
	
	public SampleWithObsAct[] getLastLearningSamples(){return this.lastLearningSamples;}
    
    private static final Log log = new Log(//edit these values as desired (class, Level, less trace information)
			TamerAgent.class, Level.INFO, Log.Simplicity.HIGH);//basic logging functionality
	
	// Called when the environment is loaded (when "Load Experiment" is clicked in RLViz)
    public void agent_init(String taskSpec) {
    	GeneralAgent.agent_init(taskSpec, this);
		
		//// CREATE CreditAssignParamVec
		CreditAssignParamVec credAssignParams = new CreditAssignParamVec(this.params.distClass, 
														this.params.creditDelay, 
														this.params.windowSize,
														this.params.extrapolateFutureRew,
														this.params.delayWtedIndivRew,
														this.params.noUpdateWhenNoRew);
		
		//// INITIALIZE TAMER
		this.hLearner = new HLearner(this.model, credAssignParams);
		//System.out.println(this.params.selectionMethod +"\r" +this.params.selectionParams.toString() );
		this.actSelector = new ActionSelect(this.model, this.params.selectionMethod, 
											this.params.selectionParams, this.currObsAndAct.getAct().duplicate());


		
		//LogTrainer.trainOnLog("/Users/bradknox/rl-library/data/cartpole_tamer/recTraj-wbknox-tamerOnly-1295030420.488000.log", this);
		if (enableGUI) { // TODO reduce 3 lines below to a single line, putting the 3 inside TrainerListener
			//Schedule a job for event dispatch thread:
	        //creating and showing this application's GUI.
	        javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					trainerListener = TrainerListener.createAndShowGUI(TamerAgent.this);
				}
			});
		}
		if (this.actSelector.getRewModel() == null)
			this.actSelector.setRewModel(this.model);
		this.endInitHelper();
    }
    




	// Called at the beginning of each episode (in RLViz, it's first called when "Start" is first clicked)
    public Action agent_start(Observation o, double time, Action predeterminedAct) {
    	//System.out.println("---------------------------start TAMER ep " + this.currEpNum);
    	// System.out.println("\n\n------------new episode-----------");
		this.startHelper();
		
		//this.hLearner.newEpisode();	 //// CLEAR HISTORY and do any other set up
		this.lastStepStartTime = -10000000; // should cause a big problem if it's used during the first time step (which shouldn't happen)
		
        return agent_step(0.0, o, time, predeterminedAct);
    }
    
    
    

    public Action agent_step(double r, Observation o, double startTime, Action predeterminedAct) {
    	return agent_step(r, o, startTime, predeterminedAct, this.lastObsAndAct.getAct());
    }
    
    public Action agent_step(double r, Observation o, double startTime, Action predeterminedAct, Action tieBreakAction) {
    	log.log(Level.INFO,"\n-----------------Tamer step---------------");
    	log.log(Level.FINER,"\n");
    	//System.out.println("Training? " + this.inTrainSess);
    	log.log(Level.FINE,"TAMER obs: " + Arrays.toString(o.intArray));
    	if (verifyObsFitsEnvDesc)
    		this.checkObs(o);
    	//System.out.println("rew list in TAMER: " + this.hRewList.toString());
    	this.stepStartTime = startTime;
		this.stepStartHelper(r, o); // this.stepStartTime (set in stepStartHelper()) ends last step and starts new step
		log.log(Level.FINE,"TAMER this.stepStartTime: " + String.format("%f", this.stepStartTime));
    	this.hLearner.recordTimeStepEnd(startTime);
//    	if (this.stepsThisEp > 1)
//    		System.out.println("Tamer feats for last obs-act: " + Arrays.toString(this.featGen.getFeats(o, this.lastObsAndAct.getAct())));
    	
    	/*
    	 * PROCESS PREVIOUS TIME STEP
    	 */
//		if (this.stepsThisEp == 2)
			//log.log(Level.INFO,"Predicted human reward for last step in TAMER: " + this.getVal(this.lastObsAndAct));
		processPrevTimeStep(this.stepStartTime);
		this.lastLearningSamples = handlePrevSamples(startTime, inTrainSess);
//		this.lastLearningSamples = this.hLearner.processSamples(startTime, inTrainSess);
		
		try{
			for(SampleWithObsAct sample: this.lastLearningSamples){
				log.log(Level.INFO,"Sample Value: " + sample.label);
				log.log(Level.FINE,"Obs: " + Arrays.toString(sample.obs.intArray) + "Act: " + Arrays.toString(sample.act.intArray));
			}
		}
		catch(Exception e){e.printStackTrace();}
		
		/*
		 *  GET ACTION
		 */
		this.currObsAndAct.setAct(predeterminedAct);
		//System.out.print("tamerAgent ");
		if (this.currObsAndAct.actIsNull()) {
			this.currObsAndAct.setAct(this.actSelector.selectAction(o, tieBreakAction));
		}
    	
//		if (this.stepsThisEp == 399)
			log.log(Level.FINER,"TAMER act vals: " + Arrays.toString(this.model.getStateActOutputs(o, this.model.getPossActions(o))));
		
		this.lastStepStartTime = this.stepStartTime;
		//if (this.currObsAndAct.getAct().intArray.length > 0)
		//	System.out.println("TAMER action: " + this.currObsAndAct.getAct().intArray[0]);
//		if (this.lastAct != null)
//			System.out.println("TAMER last action: " + this.lastAct.intArray[0]);
		this.stepEndHelper(r, o);
		if (this.isTopLevelAgent) // If not top level, TamerAgent's chosen action might not be the actual action. This must be called by the primary class.
			this.hLearner.recordTimeStepStart(o, this.currObsAndAct.getAct(), this.featGen, startTime);
		Map<String, Object> params = new HashMap<>();
		params.put("episode reward", r);
		this.updateHistory(params);
		return this.currObsAndAct.getAct();
    }

    
    
    
    public void agent_end(double r, double time) {
    	this.stepStartTime = time;
    	this.endHelper(r);
		//// PROCESS PREVIOUS TIME STEP
		processPrevTimeStep(this.stepStartTime);
    	this.actSelector.anneal();
    	GeneralAgent.sleep(EP_END_PAUSE);
    }

    
    
    /**
     * Processes rewards from a previous time step
     */
	protected void processPrevTimeStep(double borderTime){
//		System.out.println("Efficiency: "+this.hRewThisStep);
		if (inTrainSess) //// UPDATE
			this.hLearner.processHRew(this.hRewThisStep);

		if (verbose)
			System.out.println("hRewThisStep: " + this.hRewThisStep.toString());
	}
    
	/**
	 * Handles the samples created by processing the rewards from a previous time step
	 */
	protected SampleWithObsAct[] handlePrevSamples(double currTime, boolean inTrainSess)
	{
		SampleWithObsAct[] ret = this.hLearner.processSamples(currTime, inTrainSess);
		//in subclasses, we can do other things here as well
		return ret;
	}

//    public void agent_cleanup() {
//        
//    }




	
	private void getHandCodedHRew(){
		if ((this.lastObsAndAct.getObs().doubleArray[1] > 0 && this.lastObsAndAct.getAct().intArray[0] == 2) ||
			(this.lastObsAndAct.getObs().doubleArray[1] <= 0 && this.lastObsAndAct.getAct().intArray[0] == 0))
			this.addHRew(1.0);
//			this.hRewThisStep = 1.0;
		else
			this.addHRew(-1.0);
//			this.hRewThisStep = -1.0;
		System.out.println("\thRewThisStep: " + hRewThisStep.toString());
	}

	   public static void main(String[] args){
	    	TamerAgent agent = new TamerAgent();
	    	agent.processPreInitArgs(args);
	    	if (agent.glue) {
	        	AgentLoader L=new AgentLoader(agent);
	        	L.run();
	    	}
	    	else {
	    		agent.runSelf();
	    	}
	    }  
	    
		public void processPreInitArgs(String[] args) {
			log.log(Level.FINE,"\n[------Tamer process pre-init args------] " + Arrays.toString(args));
			super.processPreInitArgs(args);
			for (int i = 0; i < args.length; i++) {
	    		String argType = args[i];
	    		if (argType.equals("-tamerModel") && (i+1) < args.length){
	    			if (args[i+1].equals("linear")) {
	    				System.out.println("Setting model to linear model");
	    				this.params.featClass = "FeatGen_RBFs";
	    				this.params.modelClass = "IncGDLinearModel";
	    				
	    				// These fit the RBF class that was tested to give identical output with that of the python code
	    				this.params.featGenParams.put("basisFcnsPerDim", "40");
	    				this.params.featGenParams.put("relWidth", "0.08");
	    				this.params.featGenParams.put("biasFeatVal", "0.1");
	    				this.params.featGenParams.put("normMin", "-1");
	    				this.params.featGenParams.put("normMax", "1");
	   					
	   					// Learning params
	    				this.params.initModelWSamples = false;
	    				this.params.initWtsValue = 0.0;
	    				this.params.stepSize = 0.001; // matches python code
	    			}
	    			else if (args[i+1].equals("kNN")) {
	    				this.params.modelClass = "WekaModelPerActionModel";
	    				this.params.featClass = "FeatGen_NoChange";
	    				this.params.initModelWSamples = false; //// no biasing in MC for ALIHT paper and ICML workshop paper
	    				this.params.numBiasingSamples = 100;
	    				this.params.biasSampleWt = 0.1;
	    				this.params.wekaModelName = "IBk"; //// IBk for ALIHT paper and ICML workshop paper
	    			}
	    			else {
	    				System.out.println("\nIllegal TamerAgent model type. Exiting.\n\n");
	    				System.exit(1);
	    			}
	    			
	    			System.out.println("agent model set to: " + args[i+1]);
	    		}
	    		else if (argType.equals("-credType") && (i+1) < args.length){
	    			if (args[i+1].equals("aggregate")) {
	    				this.params.delayWtedIndivRew = false;
	    				this.params.noUpdateWhenNoRew = false;
	    			}
	    			else if (args[i+1].equals("aggregRewOnly")) {
	    				this.params.delayWtedIndivRew = false;
	    				this.params.noUpdateWhenNoRew = true;    				
	    			}
	    			else if (args[i+1].equals("indivAlways")) {
	    				this.params.delayWtedIndivRew = true;
	    				this.params.noUpdateWhenNoRew = false;
	    			}
	    			else if (args[i+1].equals("indivRewOnly")) {
	    				this.params.delayWtedIndivRew = true;
	    				this.params.noUpdateWhenNoRew = true;    				
	    			}
	    			else{
	    				System.out.println("\nIllegal TamerAgent credit assignment type. Exiting.\n\n");
	    				System.exit(1);
	    			}
	    			System.out.println("agent.credType set to: " + args[i+1]);
	    		}
			}
		}

		public void receiveKeyInput(char c){
			super.receiveKeyInput(c);
			//System.out.println("TamerAgent receives key: " + c);
			if (c == '/') {
				this.addHRew(Feedback.GOOD);
			}
			else if (c == 'z') {
				this.addHRew(Feedback.BAD);
			}
			else if (c == '?') {
				this.addHRew(Feedback.EXCELLENT);
			}
			else if (c == 'Z') {
				this.addHRew(Feedback.AWFUL);
			}
			else if (c == ' ' && this.allowUserToggledTraining) {
				this.toggleInTrainSess();
				this.hLearner.credA.setInTrainSess(Stopwatch.getComparableTimeInSec(), this.inTrainSess);
			}
			else if (c == 'S') {
//				System.out.println(Arrays.toString(((IncGDLinearModel)this.model).getWeights()));
				this.model.saveDataAsArff(this.envName, (int)Stopwatch.getWallTimeInSec(), "");
			}
			
//			System.out.println("hRewList after key input: " + this.hRewList.toString());
		}
	    
		public void initRecords() {
			super.initRecords();
			if (this.hLearner != null)
				this.hLearner.clearHistory();
			this.lastStepStartTime = -10000000;
		}
	  

	    public static ParameterHolder getDefaultParameters() {
	        ParameterHolder p = new ParameterHolder();
	        rlVizLib.utilities.UtilityShop.setVersionDetails(p, new DetailsProvider());
	        return p;
	    }

		public void setHistoryTracker(JTable table) 
		{
			this.histTable = table;
		}

		public void updateHistory(Map<String, Object> params)
		{
			if(params == null) params = new HashMap<>();
			double r = params.containsKey("episode reward") ? (double) params.get("episode reward") : 0;
			if(histTable != null)
			{
				assert histTable.getModel() instanceof DefaultTableModel : "History Table created with incompatible model (needs support)!";
				DefaultTableModel model = (DefaultTableModel) histTable.getModel();
				model.addRow(new Object[] { this.currEpNum, this.unique, 0.0, 0.0, r });
			}
		}
}

class Feedback
{
	public static final int
		BAD = -1,
		GOOD = 1,
		AWFUL = -10,
		EXCELLENT = 10;
}



/**
 * This is a little helper class that fills in the details about this environment
 * for the fancy print outs in the visualizer application.
 */
class DetailsProvider implements hasVersionDetails {

    public String getName() {
        return "General TAMER Agent";
    }

    public String getShortName() {
        return "Tamer Agent";
    }

    public String getAuthors() {
        return "Brad Knox";
    }

    public String getInfoUrl() {
        return "http://www.cs.utexas.edu/~bradknox";
    }

    public String getDescription() {
        return "RL-Library Java Version of a general Tamer agent.";
	}
}

