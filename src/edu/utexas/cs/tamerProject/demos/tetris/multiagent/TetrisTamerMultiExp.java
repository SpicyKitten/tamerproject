package edu.utexas.cs.tamerProject.demos.tetris.multiagent;

import java.util.Observable;

import org.rlcommunity.environments.tetris.Tetris;

import edu.utexas.cs.tamerProject.applet.RLPanel;
import edu.utexas.cs.tamerProject.applet.RunLocalExperiment;
import edu.utexas.cs.tamerProject.applet.TamerApplet;
import edu.utexas.cs.tamerProject.experiments.GeneralExperiment;
import edu.utexas.cs.tamerProject.featGen.FeatGenerator;
import rlVizLib.general.ParameterHolder;

public class TetrisTamerMultiExp extends TamerApplet{
	
	public void update(Observable observable, Object obj) {
		if(this.rlPanel != null)
			super.update(observable, obj);
		else
			System.exit(0);
	}

	GeneralExperiment exp;
	
	public void initPanel() {
		/*
		 * Init experiment class
		 */
		exp = new TetrisTamerMultiExpHelper();
		
		/*
		 * Init environment
		 */
//		env = exp.createEnv();
		ParameterHolder params = Tetris.getDefaultParameters();
//		System.out.println("my seed parameter is "+getParameter("seed"));
		params.addIntegerParam("seed");
		params.setIntegerParam("seed", 329834294);
		env = new Tetris(params);
		
		/*
		 * Init agent
		 */
//		String[] args = TetrisTamerMultiExpHelper.getDebugArgsStrArray();
		//efficiencyNoMoral = {moral:false}
		//moralEfficiencyComposed = {moral:true},{meld:true,-5,5}
		//moralEfficiencyMeld = {moral:true},{pipe:true,-5,5}
		//moralEfficiencyParallel = {moral:true}
		//moralNoEfficiency = {moral:true}, @Human: don't give efficiency feedback
		String moral = new String[]{"false","true"}[1];
		String filter = new String[]{"fixed","global","relative","simple"}[2];
		String meld = new String[]{"false","true"}[1];
		String negativeValue = (Double.valueOf(0)).toString();
		String positiveValue = (Double.valueOf(0)).toString();
		String pipe = new String[]{"false","true"}[0];
		//DONE: implement piping moral rewards into efficiency channel (yikes)
		String[] args = new String[] { "-moral", moral,
				"-filter", filter, "-meld", meld, negativeValue, positiveValue,
				"-pipe", pipe, negativeValue, positiveValue};
		agent = exp.createAgent(args, env);

		/*
		 * Set agent parameters
		 */
		agent.setAllowUserToggledTraining(false);
		agent.setRecordLog(true);
		agent.setRecordRew(true);
		
		FeatGenerator.setStaticSeed(5000);
		/*
		 * Set experimental parameters
		 */
		RunLocalExperiment.stepDurInMilliSecs = 0;
		RunLocalExperiment.numEpisodes = 1;
		RLPanel.DISPLAY_SECONDS_FOR_TIME = true;
		RLPanel.DISPLAY_REW_THIS_EP = true;
		RLPanel.PRINT_REW_AS_INT = true;
		RLPanel.nameForRew = "Lines cleared";
		RLPanel.enableSpeedControls = true;
		RLPanel.enableSingleStepControl = false;
		
		this.trainerUnique = "my little 5";
		
		super.initPanel();
	}
	
	
	protected void prepForStartTask(){
		prepPanelsForStartTask();
		rlPanel.runLocal.initExp();
		// Experiment-specific code below.
		if (exp != null) {
			exp.adjustAgentAfterItsInit(TetrisTamerMultiExpHelper.getDebugArgsStrArray(), agent);
			exp.processTrainerUnique(agent, TetrisTamerMultiExp.this.trainerUnique);
		}
	}
	
}