package edu.utexas.cs.tamerProject.modeling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;

import edu.utexas.cs.tamerProject.featGen.FeatGenerator;

public class IncBatchPerceptronModel extends PerceptronModel 
{
	/** The maximum number of tracked batches */
	private final int batchSize;
	/**
	 * The tracked samples for the incremental batches.
	 */
	@SuppressWarnings("serial")
	private Queue<Sample> samples = new ArrayDeque<Sample>() {
		public boolean offer(Sample s) 
		{
			if (this.size() >= batchSize)
				super.poll();
			return super.offer(s);
		}
	};
	
	/**
	 * <code>first</code> = 0.0, <code>second</code> = 1.0
	 * @see IncBatchPerceptronModel#IncBatchPerceptronModel(int, double, FeatGenerator, double, double)
	 */
	public IncBatchPerceptronModel(int batchSize, int numFeatures, double stepSize, FeatGenerator featGen) 
	{
		super(numFeatures, stepSize, featGen);
		this.batchSize = batchSize;
	}
	
	/**
	 * Creates a perceptron-based model for predicting two classes, <code>first</code>
	 * and <code>second</code>. The learning rate is <code>stepSize</code>, using
	 * inputs of length <code>numFeatures</code>.
	 * @param numFeatures The number of features in input to the model
	 * @param stepSize The learning rate for the model
	 * @param featGen The FeatGenerator for the model
	 * @param first The first prediction class for the model
	 * @param second The second prediction class for the model
	 */
	public IncBatchPerceptronModel(int batchSize, int numFeatures, double stepSize, FeatGenerator featGen,
			double first, double second)
	{
		super(numFeatures, stepSize, featGen, first, second);
		this.batchSize = batchSize;
	}
	
	@Override
	public void addInstance(Sample sample) {
		assert sample.label == firstLabel || sample.label == secondLabel : "Sample provided not instance of an expected class!";
		double[] feats = sample.feats;
		assert feats.length + 1 == weights.length : "Feats provided are of incorrect dimension! "+(weights.length-1)+" feats expected but "+feats.length+" feats provided!";
//		System.out.println("Provided sample");
//		System.out.println(sample);
		this.samples.offer(sample);
		for(Sample trackedSample : samples)
			this.updateWeights(trackedSample);
	}
	
	@Override
	public void clearSamplesAndReset() 
	{
		Arrays.fill(weights, 0);
		this.samples.clear();
	}

}
