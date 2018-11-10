package edu.utexas.cs.tamerProject.demos.tetris.multiagent;

import java.io.File;
import java.util.Observable;

import edu.utexas.cs.tamerProject.applet.TamerApplet;
import edu.utexas.cs.tamerProject.experiments.GeneralExperiment;

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
		/* 0*/settings.put("efficiencyMoralParallel fixed", new Settings("false",0,"false"));
		/* 1*/settings.put("efficiencyMoralParallel global", new Settings("false",0,"false"));
		/* 2*/settings.put("efficiencyMoralParallel relative", new Settings("false",0,"false"));
		/* 3*/settings.put("efficiencyMoralParallel simple", new Settings("false",0,"false"));
		/* 4*/settings.put("efficiencyMoralPiped fixed", new Settings("false",5,"true"));
		/* 5*/settings.put("efficiencyMoralPiped global", new Settings("false",5,"true"));
		/* 6*/settings.put("efficiencyMoralPiped relative", new Settings("false",5,"true"));
		/* 7*/settings.put("efficiencyMoralPiped simple", new Settings("false",5,"true"));
		/* 8*/settings.put("efficiencyOnly fixed", new Settings("true",0,"false"));
		/* 9*/settings.put("efficiencyOnly global", new Settings("true",0,"false"));
		/*10*/settings.put("efficiencyOnly relative", new Settings("true",0,"false"));
		/*11*/settings.put("efficiencyOnly simple", new Settings("true",0,"false"));
		/*12*/settings.put("moralityOnly fixed", new Settings("false",0,"false"));
		/*13*/settings.put("moralityOnly global", new Settings("false",0,"false"));
		/*14*/settings.put("moralityOnly relative", new Settings("false",0,"false"));
		/*15*/settings.put("moralityOnly simple", new Settings("false",0,"false"));
		Object[] dirs = settings.keySet().toArray();
		boolean isGeneral = true;
		String s = (String)dirs[15];
		Settings mySettings = settings.get(s);
		System.out.println("Setting: " + s);
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
		params.setIntegerParam("seed", 32983294);
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
		//path to target directory (example is C:\Users\user\git\tamerproject\records\)
		file = Paths.get("/Users","user","git","tamerproject","records", s.replaceAll(" .*", ""), generalString, s.replace(" ","_")+"_"+generalString+".csv").toFile();
//		String meld = new String[]{"false","true"}[1];//-0.08516973786720164
		String meld = mySettings.meld();
		String negativeValue = mySettings.negativeValue();
		String positiveValue = mySettings.positiveValue();
		String pipe = mySettings.pipe();
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
		RunLocalExperiment.stepDurInMilliSecs = 1;
		RunLocalExperiment.numEpisodes = 10;
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