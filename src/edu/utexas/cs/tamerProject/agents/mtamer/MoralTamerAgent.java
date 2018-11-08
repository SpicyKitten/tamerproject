/**
 * 
 */
package edu.utexas.cs.tamerProject.agents.mtamer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;

import javax.swing.table.DefaultTableModel;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.actSelect.ActionSelect;
import edu.utexas.cs.tamerProject.agents.CreditAssignParamVec;
import edu.utexas.cs.tamerProject.agents.GeneralAgent;
import edu.utexas.cs.tamerProject.agents.HLearner;
import edu.utexas.cs.tamerProject.agents.mtamer.moral.MoralAgent;
import edu.utexas.cs.tamerProject.agents.mtamer.proxy.HumanProxy;
import edu.utexas.cs.tamerProject.agents.mtamer.proxy.ProxyType;
import edu.utexas.cs.tamerProject.agents.mtamer.trackable.TableTrackable;
import edu.utexas.cs.tamerProject.agents.tamer.HRew;
import edu.utexas.cs.tamerProject.agents.tamer.TamerAgent;
import edu.utexas.cs.tamerProject.featGen.FeatGenerator;
import edu.utexas.cs.tamerProject.featGen.moral.MoralFeatGen_Tetris;
import edu.utexas.cs.tamerProject.logger.Log;
import edu.utexas.cs.tamerProject.modeling.IncBatchPerceptronModel;
import edu.utexas.cs.tamerProject.modeling.SampleWithObsAct;
import edu.utexas.cs.tamerProject.modeling.templates.RegressionModel;
import edu.utexas.cs.tamerProject.trainInterface.TrainerListener;
import edu.utexas.cs.tamerProject.utils.Stopwatch;
import rlVizLib.general.ParameterHolder;
import rlVizLib.general.hasVersionDetails;

/**
 * TAMER agent, but with morality prediction???? :O
 */
public class MoralTamerAgent extends TamerAgent implements MoralAgent, TableTrackable{
	/**
	 * Learning human input, we will try to predict 
	 * whether move is moral (0) or immoral(1)
	 */
	public HLearner mLearner;
	public ArrayList<HRew> mRewThisStep;
	public HumanProxy m_proxy;
	public HumanProxy v_proxy;
	public FeatGenerator moralFeatGen;
	public Consumer<Double> alternativeMoralPipe = null;
	
	private static final Log log = new Log(//edit these values as desired (class, Level, less trace information)
			MoralTamerAgent.class, Level.OFF, Log.Simplicity.HIGH);//basic logging functionality
	
	public void agent_init(String taskSpec)
	{
		GeneralAgent.agent_init(taskSpec, this);
		this.setMoralFeatGen(param("moralFeatClass"));
		//// CREATE CreditAssignParamVec
		CreditAssignParamVec credAssignParams = new CreditAssignParamVec("immediate", this.params.creditDelay,
				this.params.windowSize, this.params.extrapolateFutureRew, this.params.delayWtedIndivRew,
				this.params.noUpdateWhenNoRew);
		//// INITIALIZE TAMER
		this.hLearner = new HLearner(this.model, credAssignParams);
		credAssignParams = new CreditAssignParamVec("immediate", this.params.creditDelay,
				this.params.windowSize, this.params.extrapolateFutureRew, this.params.delayWtedIndivRew,
				this.params.noUpdateWhenNoRew);
		this.mLearner = new HLearner(this.makeMoralModel(), credAssignParams);
		//assuming feedback works in the same way for morality and score under the TAMER model
		//and that credAssignParams aren't changed in the HLearner class (which they aren't for now?)
		
		//System.out.println(this.params.selectionMethod +"\r" +this.params.selectionParams.toString() );
		this.actSelector = new ActionSelect(this.model, this.params.selectionMethod, 
											this.params.selectionParams, this.currObsAndAct.getAct().duplicate());
		//ActionSelect actSelector's model should somehow incorporate both elements of mLearner
		//and hLearner feedback. This is pretty difficult, not sure how to make that happen

		if (enableGUI) {
	        javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					trainerListener = TrainerListener.createAndShowGUI(MoralTamerAgent.this);
				}
			});
		}
		if (this.actSelector.getRewModel() == null)
			this.actSelector.setRewModel(this.model);
		this.endInitHelper();
	}
	
	public void setMoralFeatGen(String featGen)
	{
		switch (featGen) {
		case "MoralFeatGen_Tetris":
			this.moralFeatGen = new MoralFeatGen_Tetris(this.theObsIntRanges, this.theObsDoubleRanges,
					this.theActIntRanges, this.theActDoubleRanges);
			return;
		default:
			log.log(Level.WARNING,"The current code doesn't support class " + params.featClass
					+ " for feature generation. Adding support might be trivial. " +
							"Adjust MoralTamerAgent.log for more.",Log.Simplicity.NONE);
		}
	}
	
	public RegressionModel makeMoralModel()
	{
		log.log(Level.FINE, "Step size: "+param("stepSize"));
		//Can't agent_init without setting the value of moralFeatGen!!!
		RegressionModel model = null;
		log.log(Level.FINER,""+this.moralFeatGen.getNumFeatures());
		model = new IncBatchPerceptronModel(1000, this.moralFeatGen.getNumFeatures(), 
		0.5 / 14.0, this.moralFeatGen, 1, 0);
//		model = new PerceptronModel(this.moralFeatGen.getNumFeatures(), 
//				0.5 / 14.0, this.moralFeatGen, 1, 0);
//		model = new IncGDLinearModel(this.moralFeatGen.getNumFeatures(), 
//				/*param("stepSize")*/0.0005 / 14.0, this.moralFeatGen, 
//				param("initWtsValue")/*0.4999*/, 
//				param("modelAddsBiasFeat"));
//		System.out.println("trace decay factor: "+param("traceDecayFactor"));
//		System.out.println("discount factor for learning: "+this.discountFactorForLearning);
//		System.out.println("trace type: "+param("traceType"));
//		((IncGDLinearModel)model).setEligTraceParams(this.params.traceDecayFactor, 
//				this.discountFactorForLearning, this.params.traceType);
		return model;
	}
	
	public static ParameterHolder getDefaultParameters() {
        ParameterHolder p = new ParameterHolder();
        rlVizLib.utilities.UtilityShop.setVersionDetails(p, new DetailsProvider());
        return p;
    }
	
	protected SampleWithObsAct[] handlePrevSamples(double currTime, boolean inTrainSess)
	{
		SampleWithObsAct[] ret = this.hLearner.processSamples(currTime, inTrainSess);
		this.mLearner.processSamples(currTime, inTrainSess);
		return ret;
	}
	
	protected void processPrevTimeStep(double borderTime)
	{
		super.processPrevTimeStep(borderTime);
		if(this.inTrainSess)
			this.mLearner.processHRew(mRewThisStep);
		if(this.verbose)
			System.out.println("mRewThisStep: " + mRewThisStep);
	}
	
	public void updateHistory(Map<String, Object> params)
	{
		if(params == null) params = new HashMap<>();
		double r = params.containsKey("episode reward") ? (double) params.get("episode reward") : 0;
		@SuppressWarnings("unchecked")
		Map<Action, Double> moralRewards = (Map<Action, Double>) params.get("moral rewards");
		if(histTable != null)
		{
			assert histTable.getModel() instanceof DefaultTableModel : "History Table created with incompatible model (needs support)!";
			DefaultTableModel model = (DefaultTableModel) histTable.getModel();
			double mPredict = 0.0d;
			if(moralRewards != null)
				mPredict = moralRewards.get(this.currObsAndAct.getAct()) >= 0.5 ? 1.0 : 0.0;
			double mReal = 0.0d;
			if(params.containsKey("moral feedback"))
				mReal = (double) params.get("moral feedback");
//				mReal = this.getNRewList("moralRew").get(0).val;
			model.addRow(new Object[] { this.currEpNum, this.unique, mPredict, mReal, r });
		}
	}
	
	public Action agent_step(double r, Observation o, double startTime, Action predeterminedAct, Action tieBreakAction) 
	{
		log.log(Level.INFO,"\n----------------MTamer step---------------");
    	log.log(Level.FINER,"\n");
//    	log.log(Level.FINE,"MTAMER obs: " + Arrays.toString(o.intArray));
    	if (verifyObsFitsEnvDesc)
    		this.checkObs(o);
    	this.stepStartTime = startTime;
    	this.mRewThisStep = getMoralFeedbackThisStep();
		this.stepStartHelper(r, o); // this.stepStartTime (set in stepStartHelper()) ends last step and starts new step
//		log.log(Level.FINE,"MTAMER this.stepStartTime: " + String.format("%f", this.stepStartTime));
    	this.hLearner.recordTimeStepEnd(startTime);
    	this.mLearner.recordTimeStepEnd(startTime);
    	processPrevTimeStep(this.stepStartTime);
		this.lastLearningSamples = handlePrevSamples(startTime, inTrainSess);
		try{
			for(SampleWithObsAct sample: this.lastLearningSamples){
				log.log(Level.INFO,"Sample Value: " + sample.label);
//				log.log(Level.FINE,"Obs: " + Arrays.toString(sample.obs.intArray) + "Act: " + Arrays.toString(sample.act.intArray));
			}
		}
		catch(Exception e){e.printStackTrace();}
		this.currObsAndAct.setAct(predeterminedAct);
		double[] tamerVals = (this.model.getStateActOutputs(o, this.model.getPossActions(o)));
//		log.log(Level.FINE,"TAMER act values("+tamerVals.length+"): " + Arrays.toString(tamerVals));
		double[] moralVals = (this.mLearner.getModel().getStateActOutputs(o, this.mLearner.getModel().getPossActions(o)));
		log.log(Level.FINE,"TAMER moral vals("+moralVals.length+"): " + Arrays.toString(moralVals));
		Map<Action, Double> moralRewards = null;
		if (this.currObsAndAct.actIsNull()) {
			moralRewards = this.getActionRewards(this.mLearner.getModel(), o);
			Map<Action, Boolean> moralPredictions = this.getActionMoralities(o, 0.5);
			this.currObsAndAct.setAct(this.actSelector.selectAction(o, tieBreakAction, Optional.<Map<Action,Boolean>>of(moralPredictions)));
		}
		this.lastStepStartTime = this.stepStartTime;
		//if (this.currObsAndAct.getAct().intArray.length > 0)
		//	System.out.println("TAMER action: " + this.currObsAndAct.getAct().intArray[0]);
//		if (this.lastAct != null)
//			System.out.println("TAMER last action: " + this.lastAct.intArray[0]);
		this.stepEndHelper(r, o);
		if (this.isTopLevelAgent) // If not top level, TamerAgent's chosen action might not be the actual action. This must be called by the primary class.
		{
			this.hLearner.recordTimeStepStart(o, this.currObsAndAct.getAct(), this.featGen, startTime);
			this.mLearner.recordTimeStepStart(o, this.currObsAndAct.getAct(), this.moralFeatGen, startTime);
//			System.out.println("Change in feats: "+Arrays.toString(this.moralFeatGen.getFeats(o, this.currObsAndAct.getAct())));
		}
		//log a moral reward for the done action
		if(moralRewards != null) log.log(Level.FINE,String.format("MPredict: [%f]", moralRewards.get(this.currObsAndAct.getAct())));
		double feedback = this.m_proxy.notify(this, o, this.currObsAndAct.getAct());
		double efficiency = 0;
//		if(this.v_proxy != null)
			efficiency = this.v_proxy.notify(this, o, this.currObsAndAct.getAct());
		Map<String, Object> params = new HashMap<>();
		params.put("episode reward", r);
		params.put("episode feedback", efficiency);
		params.put("moral rewards", moralRewards);
		params.put("moral feedback", feedback);
		this.updateHistory(params);
		return this.currObsAndAct.getAct();
	}
	
	public void agent_end(double r, double time) 
	{
		this.mRewThisStep = getMoralFeedbackThisStep();
		super.agent_end(r, time);
	}
	
	/**
	 * Gets the moral feedback for the step, clearing the named reward before returning.
	 */
	public ArrayList<HRew> getMoralFeedbackThisStep()
	{
		ArrayList<HRew> moralRewards = this.getNRewList("moralRew", true);
		this.getNRewList("moralRew").clear();
		return moralRewards;
	}
	
	//technically inherited, now, but leaving it here for tracing purposes in future
	public void addMRew(double feedbackVal)
	{
		if(alternativeMoralPipe != null)
		{
			alternativeMoralPipe.accept(feedbackVal);
		}
		else
		{
			ArrayList<HRew> moralRewards = this.getNRewList("moralRew");
			//only the most recent moral reward is relevant
			if(moralRewards.size() > 0)
				moralRewards.set(0, (HRew)new MRew(feedbackVal, Stopwatch.getComparableTimeInSec()));
			else
				moralRewards.add((HRew)new MRew(feedbackVal, Stopwatch.getComparableTimeInSec()));
		}
	}
	
	public void initRecords() {
		super.initRecords();
		if (this.mLearner != null)
			this.mLearner.clearHistory();
	}
	
	public void receiveKeyInput(char c)
	{
		if((c == 'z' || c == 'Z' || c == '/' || c == '?') && this.v_proxy != null)
			return;
		if((c == 'x' || c == 'X' || c == '.' || c == '>') && this.m_proxy != null)
			return;
		super.receiveKeyInput(c);
		if(c == 'x' || c == 'X')
			this.addMRew(Feedback.IMMORAL);//x for immoral
		else if (c == '.' || c == '>')
			this.addMRew(Feedback.MORAL);
		else if (c == ' ' && this.allowUserToggledTraining) {
			this.mLearner.credA.setInTrainSess(Stopwatch.getComparableTimeInSec(), this.inTrainSess);
		}
	}
	
	protected Map<Action, Double> getActionRewards(RegressionModel m, Observation o)
	{
		Map<Action, Double> actionRewards = new HashMap<Action, Double>();
		ArrayList<Action> actions = this.moralFeatGen.getPossActions(o);
		double[] vals = m.getStateActOutputs(o, actions);
		for(int i = 0; i < actions.size(); ++i)
		{
			actionRewards.put(actions.get(i), vals[i]);
		}
		return actionRewards;
	}
	
	/**
	 * {@literal<Action, Value> maps to <Action, True> if Value falls on the side of threshold
	 * that denotes morality}
	 */
	protected Map<Action, Boolean> getActionMoralities(Observation o, double threshold)
	{
		assert threshold > 0.0 && threshold < 1.0 : "Threshold of action moralities isn't within (0,1)!";
		Map<Action, Boolean> actionMoralities = new HashMap<Action, Boolean>();
		Map<Action, Double> moralRewards = this.getActionRewards(this.mLearner.getModel(), o);
		double immoral = Feedback.IMMORAL;
		double moral = Feedback.MORAL;
		double divider = Math.min(immoral, moral) + threshold*(Math.abs(immoral-moral));
		assert immoral != divider && moral != divider : "Math error: not able to distinguish immoral and moral actions!";
		boolean topIsMoral = (immoral < divider);
		for(Map.Entry<Action, Double> e : moralRewards.entrySet())
		{
			//we round values up if they are exactly equal to the divider
			if((topIsMoral && e.getValue() >= divider)
					|| (!topIsMoral && e.getValue() < divider))
				actionMoralities.put(e.getKey(), true);
			else
				actionMoralities.put(e.getKey(), false);
		}
		return actionMoralities;
	}
	
	public double moralFeedbackValue()
	{
		return Feedback.MORAL;
	}
	
	public double immoralFeedbackValue()
	{
		return Feedback.IMMORAL;
	}

	@Override
	public void setProxy(HumanProxy proxy, ProxyType type)
	{
		switch(type)
		{
		case MORAL:
			this.m_proxy = proxy; break;
		case VALUE:
			this.v_proxy = proxy; break;
		}
	}
}

class Feedback 
{
	public static final int 
		MORAL = 0, 
		IMMORAL = 1;
}

class DetailsProvider implements hasVersionDetails {

    public String getName() {
        return "Moral TAMER Agent";
    }

    public String getShortName() {
        return "M-Tamer Agent";
    }

    public String getAuthors() {
        return "Avilash Rath";
    }

    public String getInfoUrl() {
        return "";
    }

    public String getDescription() {
        return "RL-Library Java Version of a moral Tamer agent.";
	}
}
