package edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.tetris;

import java.util.Arrays;
import java.util.function.BiFunction;

import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.EvaluationFactory;

public class TetrisEvaluationFactory implements EvaluationFactory
{
	private int[][] evals;
	private int[] weights;
	private int worldWidth;
	private int worldHeight;
	public TetrisEvaluationFactory(Object... objs) 
	{
		this((int[][])objs[0], (int[])objs[1], (int)objs[2], (int)objs[3]);
		System.out.println("Made TetrisEvaluationFactory with params:");
		System.out.println(Arrays.toString(objs));
	}
	public TetrisEvaluationFactory(int[][] details, int[] wts, int ww, int wh)
	{
		evals = details;
		worldWidth = ww;
		worldHeight = wh;
		weights = wts;
	}
	@Override
	public BiFunction<Observation, Observation, Double> get(Object... objs) {
		return (o, o2)->
		{
			int[] initialState = Arrays.copyOfRange(o.intArray, 0, worldHeight*worldWidth);
			int[] finalState = Arrays.copyOfRange(o2.intArray, 0, worldHeight*worldWidth);
			Eval initialEval = eval(initialState);
			Eval finalEval = eval(finalState);
			//anything was cleared 
			//(can't calculate exactly but we have a pretty good estimator for normal tetris games)
//			boolean line = initialEval.count > finalEval.count;
//			boolean empty = finalEval.count == 0;
//			return (empty?50d:0d)+(line?25d:0d)+(finalEval.score-initialEval.score);
			return 0d + finalEval.score - initialEval.score;
		};
	}
	
	private Eval eval(int[] state)
	{
		int eval = 0;
		int count = 0;
//		int[] feats = new int[6];
//		int featIndex = 0;
//		for(int color1 = 1; color1 <= 3; ++color1)
//		{
//			for(int color2 = color1; color2 <= 3; ++color2)
//			{
//				for (int row = 0; row < worldHeight; row++) 
//				{
//					for (int col = 0; col < worldWidth; col++) 
//					{
//						int here = state[row*worldWidth+col];
//						int bottom = -1;
//						int right = -1;
//						if(row < worldHeight - 1)
//							bottom = state[(row+1)*worldWidth+col];
//						if(col < worldWidth - 1)
//							right = state[(row)*worldWidth+col+1];
//						if(here == color1 && bottom == color2
//							|| here == color2 && bottom == color1)
//							++feats[featIndex];
//						if(here == color1 && right == color2
//							|| here == color2 && right == color1)
//							++feats[featIndex];
//					}
//				}
//				++featIndex;
//			}
//		}
//		System.out.println("Eval feats: "+Arrays.toString(feats));
		for(int i = 0; i < worldHeight; i++)
		{
			for(int j = 0; j < worldWidth; j++)
			{
				int here = state[i*worldWidth+j];
				if(here != 0)
					count++;
				int bottom = -1;
				int right = -1;
				if(i < worldHeight - 1)
					bottom = state[(i+1)*worldWidth+j];
				if(j < worldWidth - 1)
					right = state[i*worldWidth+(j+1)];
				for(int k = 0; k < evals.length; k++)
				{
					if(here == evals[k][0] && bottom == evals[k][1]
						|| here == evals[k][1] && bottom == evals[k][0])
					{
						eval += weights[k];
					}
					if(here == evals[k][0] && right == evals[k][1]
						|| here == evals[k][1] && right == evals[k][0])
					{
						eval += weights[k];
					}
				}
			}
		}
		return new Eval(eval, count);
	}

	private class Eval
	{
		public int score;
//		public int count;
		public Eval(int s, int c)
		{
			score = s;
//			count = c;
		}
	}
}
