package edu.utexas.cs.tamerProject.demos.tetris.multiagent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.utexas.cs.tamerProject.applet.RLPanel;
import edu.utexas.cs.tamerProject.applet.RunLocalExperiment;
import edu.utexas.cs.tamerProject.applet.TamerApplet;
import edu.utexas.cs.tamerProject.experiments.GeneralExperiment;


public class TetrisTamerMultiExp extends TamerApplet{

	GeneralExperiment exp;
	
	public void initPanel() {
		/*
		 * Init experiment class
		 */
		exp = new TetrisTamerMultiExpHelper();
		
		/*
		 * Init environment
		 */
		env = exp.createEnv();
		
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
		String meld = new String[]{"false","true"}[0];
		String negativeValue = (Double.valueOf(-5)).toString();
		String positiveValue = (Double.valueOf(5)).toString();
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

		/*
		 * Set experimental parameters
		 */
		RunLocalExperiment.stepDurInMilliSecs = 150;
		RLPanel.DISPLAY_SECONDS_FOR_TIME = true;
		RLPanel.DISPLAY_REW_THIS_EP = true;
		RLPanel.PRINT_REW_AS_INT = true;
		RLPanel.nameForRew = "Lines cleared";
		RLPanel.enableSpeedControls = true;
		RLPanel.enableSingleStepControl = false;
		
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