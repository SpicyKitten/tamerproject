package edu.utexas.cs.tamerProject.demos.tetris.multiagent;

import java.io.File;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
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
		{
			((TetrisTamerMultiExpHelper)exp).write(file);
			System.exit(0);
		}
	}
	
	File file;
	GeneralExperiment exp;
	
	private class Settings
	{
		private String meld;
		private double alpha;
		private String pipe;
		public String positiveValue()
		{
			return Double.valueOf(alpha).toString();
		}
		public String negativeValue()
		{
			return Double.valueOf(-alpha).toString();
		}
		public String meld() 
		{
			return meld;
		}
		public String pipe()
		{
			return pipe;
		}
		public Settings(String m, double a, String p)
		{
			meld = m;
			alpha = Math.abs(a);
			pipe = p;
		}
	}
	
	public void initPanel() {
		Map<String, Settings> settings = new LinkedHashMap<>();
		double alpha = 0.006;//global 0.06 relative 0.06 fixed 0.006 simple 0.06
		/*short 0*/settings.put("efficiencyMoralParallel fixed", new Settings("false",0,"false"));
		/*y 1*/settings.put("efficiencyMoralParallel global", new Settings("false",0,"false"));
		/*y 2*/settings.put("efficiencyMoralParallel relative", new Settings("false",0,"false"));
		/*y 3*/settings.put("efficiencyMoralParallel simple", new Settings("false",0,"false"));
		/*y 4*/settings.put("efficiencyMoralPiped fixed", new Settings("false",alpha,"true"));
		/*y 5*/settings.put("efficiencyMoralPiped global", new Settings("false",alpha,"true"));
		/*y 6*/settings.put("efficiencyMoralPiped relative", new Settings("false",alpha,"true"));
		/*y 7*/settings.put("efficiencyMoralPiped simple", new Settings("false",alpha,"true"));
		/*short 8*/settings.put("efficiencyOnly fixed", new Settings("true",0,"false"));
		/*short 9*/settings.put("efficiencyOnly global", new Settings("true",0,"false"));
		/*short10*/settings.put("efficiencyOnly relative", new Settings("true",0,"false"));
		/*short11*/settings.put("efficiencyOnly simple", new Settings("true",0,"false"));
		/*y12*/settings.put("moralityOnly fixed", new Settings("false",0,"false"));
		/*y13*/settings.put("moralityOnly global", new Settings("false",0,"false"));
		/*y14*/settings.put("moralityOnly relative", new Settings("false",0,"false"));
		/*y15*/settings.put("moralityOnly simple", new Settings("false",0,"false"));
		Object[] dirs = settings.keySet().toArray();
		int seedIndex = 2;
		boolean isLearning = true;//given using moral, true = no baseline, false = baseline
		boolean humanTraining = false;
		boolean isGeneral = true;
		String s = (String)dirs[9];//set to null unless it's a setting you're going to use 100%
		Settings mySettings = settings.get(s);
		System.out.println("Setting: " + s + (isLearning?" learning":" baseline"));
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
		int[] seeds = {32983294, -6027919, 5098428, 7658480, -8329415, -4632304, -6859866, 6802044, -1929450, -901885, -5259575};
		params.setIntegerParam("seed", seeds[seedIndex]);
//		params.setIntegerParam("seed", seeds[seedIndex]);
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
//		String filter = new String[]{"fixed","global","relative","simple"}[0];
		String filter = s.replaceAll(".* ", "");
		String generalString = isGeneral ? "general" : "specialized";
		String baselineString = isLearning ? "learning" : "baseline";
		//path to target directory (example is C:\Users\\user\\git\\tamerproject\\records\)
		file = Paths.get("/Users","user","git","tamerproject","records", s.replaceAll(" .*", ""), baselineString, s.replace(" ","_")+"_"+generalString+"_"+baselineString+".csv").toFile();
//		String meld = new String[]{"false","true"}[1];//-0.08516973786720164
		String meld = mySettings.meld();
		String negativeValue = mySettings.negativeValue();
		String positiveValue = mySettings.positiveValue();
		String pipe = mySettings.pipe();
		String humanTrainingEnabled = Boolean.toString(humanTraining);
		String learningEnabled = Boolean.toString(isLearning);
		//DONE: implement piping moral rewards into efficiency channel (yikes)
		String[] args = new String[] { "-moral", moral,
				"-filter", filter, "-meld", meld, negativeValue, positiveValue,
				"-pipe", pipe, negativeValue, positiveValue, "-human", humanTrainingEnabled,
				"-learning", learningEnabled};
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
		RunLocalExperiment.stepDurInMilliSecs = 10;
		RunLocalExperiment.numEpisodes = humanTraining ? 50 : 10;
		RLPanel.DISPLAY_SECONDS_FOR_TIME = true;
		RLPanel.DISPLAY_REW_THIS_EP = true;
		RLPanel.PRINT_REW_AS_INT = true;
		RLPanel.nameForRew = "Lines cleared";
		RLPanel.enableSpeedControls = true;
		RLPanel.enableSingleStepControl = false;
		
		//prevent unnecessary crashing
		this.trainerUnique = "unique Tetris: 1";
		
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