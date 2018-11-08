package edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.tetris;

import java.util.Arrays;
import java.util.function.BiFunction;

import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.EvaluationFactory;

class TetrisEfficiencyFactory implements EvaluationFactory
{
	private int gameWidth;
	private int gameHeight;
	private double[] weights;
	public TetrisEfficiencyFactory(Object... objs) 
	{
		this((int) objs[0], (int) objs[1], (double[])objs[2]);
	}
	
	public TetrisEfficiencyFactory(int width, int height, double[] w)
	{
		this.gameWidth = width;
		this.gameHeight = height;
		this.weights = Arrays.copyOf(w, w.length);
	}

	@Override
	public BiFunction<Observation, Observation, Double> get(Object... objs) {
		return (o, o2) -> {
			int[] initialState = Arrays.copyOfRange(o.intArray, 0, gameWidth * gameHeight);
			int[] finalState = Arrays.copyOfRange(o2.intArray, 0, gameWidth * gameHeight);
			double[] initFeats = new double[NUM_FEATS];
			double[] finalFeats = new double[NUM_FEATS];
			putSFeatsInArray(initialState, initFeats);
			putSFeatsInArray(finalState, finalFeats);
			double accumulator = 0;
			for (int i = 0; i < NUM_FEATS; ++i) {
				accumulator += (finalFeats[i] - initFeats[i]) * weights[i];
			}
			return accumulator;
		};
	}
	
	private static final int NUM_FEATS = 46;
	private static final int COL_HT_START_I = 0;
	private static final int MAX_COL_HT_I = 10;
	private static final int COL_DIFF_START_I = 11;
	private static final int NUM_HOLES_I = 20;
	private static final int MAX_WELL_I = 21;
	private static final int SUM_WELL_I = 22;
	private static final int SQUARED_FEATS_START_I = 23;

	public static double HT_SQ_SCALE = 100.0; // 40 in python-based code and experiments before 2013-02-21

	/**
	 *  python-based code used integer division and chopped off the decimal.
	 *  Set to true to get identical behavior to the python agent when learning
	 *  from python-trained logs. 
	 */
	public static boolean REMOVE_DECIMAL = false;
	public static boolean COUNT_LAST_COL_FOR_WELL_SUM = true;
	public static boolean SCALE_ALL_SQUARED_FEATS = false;
	
	private void putSFeatsInArray(int[] intStateVars, double[] featsArray) {

		for (int i = 0; i < featsArray.length; i++) {
			featsArray[i] = -1;
		}

		featsArray[NUM_HOLES_I] = 0;
		featsArray[SUM_WELL_I] = 0;

		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				int i = getIndex(row, col);
				if (intStateVars[i] > 0) { // filled cell
					if (featsArray[COL_HT_START_I + col] == -1)
						featsArray[COL_HT_START_I + col] = row;
					if (featsArray[MAX_COL_HT_I] == -1)
						featsArray[MAX_COL_HT_I] = row;
				} else { // empty cell
					if (featsArray[COL_HT_START_I + col] != -1)
						featsArray[NUM_HOLES_I] += 1;
				}
			}
		}
		for (int col = 0; col < gameWidth; col++) {
			if (featsArray[COL_HT_START_I + col] == -1)
				featsArray[COL_HT_START_I + col] = gameHeight;
		}
		if (featsArray[MAX_COL_HT_I] == -1)
			featsArray[MAX_COL_HT_I] = gameHeight;
		for (int col = 0; col < gameWidth - 1; col++) {
			featsArray[COL_DIFF_START_I + col] = Math
					.abs(featsArray[COL_HT_START_I + col] - featsArray[COL_HT_START_I + col + 1]);
		}
		for (int col = 0; col < gameWidth; col++) {
			int wellDepth = getWellDepth(col, intStateVars);
			if (wellDepth > 0 && (col < (gameWidth - 1) || COUNT_LAST_COL_FOR_WELL_SUM)) {
				featsArray[SUM_WELL_I] += wellDepth;
			}
			if (wellDepth > featsArray[MAX_WELL_I])
				featsArray[MAX_WELL_I] = wellDepth;
		}
		for (int i = 0; i < SQUARED_FEATS_START_I; i++) {
			featsArray[SQUARED_FEATS_START_I + i] = Math.pow(featsArray[i], 2.0);
			if (i <= MAX_COL_HT_I || SCALE_ALL_SQUARED_FEATS) {
				featsArray[SQUARED_FEATS_START_I + i] /= HT_SQ_SCALE;

			}
			if (REMOVE_DECIMAL)
				featsArray[SQUARED_FEATS_START_I + i] = (int) featsArray[SQUARED_FEATS_START_I + i];

		}
		// System.out.println("featsArray: " + Arrays.toString(featsArray));
		// System.out.println("NUM_HOLES:" + featsArray[NUM_HOLES_I]);
		// System.out.println("MAX_WELL: " + featsArray[MAX_WELL_I]);
		// System.out.println("SUM_WELL: " + featsArray[SUM_WELL_I]);
	}
	
	private int getIndex(int row, int col) {
		return (row * gameWidth) + col;
	}
	
	private int getWellDepth(int col, int[] intStateVars) {
		int depth = 0;
		for (int row = 0; row < gameHeight; row++) {
			if (intStateVars[getIndex(row, col)] > 0) // encounter a filled space, stop counting
				break;
			else {
				if (depth > 0) // if well-depth count has begun, don't require left and right to be filled
					depth += 1;
				else if ((col == 0 || intStateVars[getIndex(row, col - 1)] > 0) // leftmost column or cell to the left is full
						&& (col == gameWidth - 1 || intStateVars[getIndex(row, col + 1)] > 0) ){ // rightmost column or cell to the right is full
					depth += 1; // start count
				}
			}
		}
		//System.out.println("col " + col + " well depth: " + depth);
		return depth;
	}
}
