package edu.utexas.cs.tamerProject.modeling;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;

import edu.utexas.cs.tamerProject.featGen.FeatGenerator;
import edu.utexas.cs.tamerProject.modeling.templates.RegressionModel;

/**
 * Simple perceptron model for labeling samples
 * Replacement (temporary models/samples) not implemented yet
 * TODO: Implement traces/temp. models/partial samples/whatever
 * TODO: Implement optional disabling of bias weight
 * @author ratha
 *
 */
public class PerceptronModel extends RegressionModel implements Cloneable
{
	/**
	 * The value of the first class label in classification.
	 * Guaranteed to be lower than the value of the second class label.
	 */
	protected double firstLabel;
	/** The value of the second class label in classification */
	protected double secondLabel;
	/** The learning rate of the perceptron algorithm */
	protected double rate;
	/** The weights of the perceptron model
	 *  Format [Bias, w1, w2, ..., wn] for inputs as [x1, x2, ..., xn] 
	 */
	protected double[] weights;
	
	/**
	 * <code>first</code> = 0.0, <code>second</code> = 1.0
	 * @see PerceptronModel#PerceptronModel(int, double, FeatGenerator, double, double)
	 */
	public PerceptronModel(int numFeatures, double stepSize, FeatGenerator featGen)
	{
		this(numFeatures, stepSize, featGen, 0, 1);
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
	public PerceptronModel(int numFeatures, double stepSize, FeatGenerator featGen,
			double first, double second)
	{
//		assert second > first : "Perceptron expects the labels in ascending order!";
		if(first == second)
			throw new IllegalArgumentException("PerceptronModel expects two different class labels!");
		this.featGen = featGen;
		this.rate = stepSize;
		this.firstLabel = Math.min(first, second);
		this.secondLabel = Math.max(first, second);
		this.weights = new double[numFeatures + 1];
	}

	@Override
	public void addInstance(Sample sample) {
		assert sample.label == firstLabel || sample.label == secondLabel : "Sample provided not instance of an expected class!";
		double[] feats = sample.feats;
		assert feats.length + 1 == weights.length : "Feats provided are of incorrect dimension! "+(weights.length-1)+" feats expected but "+feats.length+" feats provided!";
		this.updateWeights(sample);
	}
	
	public void updateWeights(Sample sample)
	{
		double[] feats = sample.feats;
		double guess = predictLabel(feats);
		double error = sample.label - guess;
		double weightedError = error * sample.weight;
		double adjustment = rate * weightedError;
		weights[0] += adjustment * 1;//bias adjustment
		for(int i = 0; i < feats.length; ++i)
		{
			weights[i + 1] += adjustment * feats[i];
		}
		if(verbose && error != 0)
//		if(true)
		{
			System.out.println("Old weights: "+Arrays.toString(weights));
			System.out.println("Feats: "+Arrays.toString(feats));
			System.out.println("Prediction: "+guess);
			System.out.println("Reality: "+sample.label);
			System.out.println("Rate("+rate+") * Error: "+adjustment);
			System.out.println("New weights: "+Arrays.toString(weights));
		}
	}

	/**
	 * Uses <code>samples</code> to train the perceptron model
	 * @param samples Provided samples for training
	 */
	@Override
	public void addInstances(Sample[] samples) 
	{
		for(Sample s : samples)
			this.addInstance(s);
	}

	/**
	 * TODO: Temp samples not supported yet. 
	 * At the moment redirects to {@link PerceptronModel#addInstances}
	 */
	@Override
	public void addInstancesWReplacement(Sample[] samples) 
	{
		this.addInstances(samples);
	}

	@Override
	public void buildModel() 
	{
		//Nothing doing
	}
	
	@Override
	public double predictLabel(double[] feats) 
	{
		double summation = weights[0] * 1;//bias weight
		for(int i = 0; i < feats.length; ++i)
		{
			summation += feats[i] * weights[i + 1]; 
		}
		return (summation <= 0) ? firstLabel : secondLabel;
	}

	@Override
	public void clearSamplesAndReset() 
	{
		Arrays.fill(weights, 0);
	}
	
	@Override
	public PerceptronModel makeFullCopy() 
	{
		PerceptronModel cloneModel = null;
		try {
			cloneModel = (PerceptronModel) (this.clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			System.exit(1);
		}
		cloneModel.weights = Arrays.copyOf(this.weights, this.weights.length);
		return cloneModel;
	}
	
	public static void main(String[] args)
	{
		int runs = 10000;
		int correct = 0;
		for(int i = 0; i < runs; ++i)
		{
//			System.out.println(i);
			if(test())correct++;
			if(i % 100 == 0)
				System.out.println(String.format("%d/%d runs correct", correct, i+1));
		}
		System.out.println(String.format("%d/%d runs correct", correct, runs));
	}
	
	public static boolean test()
	{
		PerceptronModel pm = new PerceptronModel(7, 0.001, null, 0, 1);
		ArrayList<Sample> samples = new ArrayList<>();
		for(int i = 0; i < 400; i++)
		{
			double x1 = (int)(Math.random()*12 - 6);//from -5 to 5
			double x2 = (int)(Math.random()*12 - 6);//from -5 to 5
			double x3 = (int)(Math.random()*12 - 6);//from -5 to 5
			double x4 = (int)(Math.random()*12 - 6);//from -5 to 5
			double x5 = (int)(Math.random()*12 - 6);//from -5 to 5
			double x6 = (int)(Math.random()*12 - 6);//from -5 to 5
			double x7 = (int)(Math.random()*12 - 6);//from -5 to 5
			double y = 0 + 0 * x1 - 1 * x2 + 1 * x3 + 0 * x4 + 3*x5 + 0*x6 +25 * x7;
			//if above the line, 1. if below the line, 0.
//			double yprime = y + Math.random()*5 - 2.5;
			Sample s = new Sample(new double[] {x1, x2, x3, x4, x5, x6, x7}, y >= 0 ? 0 : 1, 1.0);
			samples.add(s);
			if(samples.size() > 500)
				samples.remove(0);
//			for(int j = 0; j < 5; ++j)
			for(Sample sample : samples)
			{
				pm.addInstance(sample);
			}
//			for(int j = 0; j < 100; ++j)
//				pm.addInstance(s);
		}
		double x1 = (int)(Math.random()*12 - 6);//from -5 to 5
		double x2 = (int)(Math.random()*12 - 6);//from -5 to 5
		double x3 = (int)(Math.random()*12 - 6);//from -5 to 5
		double x4 = (int)(Math.random()*12 - 6);//from -5 to 5
		double x5 = (int)(Math.random()*12 - 6);//from -5 to 5
		double x6 = (int)(Math.random()*12 - 6);//from -5 to 5
		double x7 = (int)(Math.random()*12 - 6);//from -5 to 5
		double y = 0 + 0 * x1 - 1 * x2 + 1 * x3 + 0 * x4 + 3*x5 + 0*x6 +25 * x7;
//		System.out.println((y >= 0 ? "0" : "1") + " expected");
//		System.out.println(Arrays.toString(pm.weights));
//		System.out.print('[');
//		for(int i = 0; i < pm.weights.length; ++i)
//		{
//			NumberFormat f = new DecimalFormat("#.#####");
//			double d = Double.parseDouble(f.format(pm.weights[i]));
//			System.out.print(d);
//			if(i != pm.weights.length - 1)
//				System.out.print(", ");
//		}
//		System.out.println(']');
		double predicted = pm.predictLabel(new double[] { x1, x2, x3, x4, x5, x6, x7});
//		System.out.println("Predicted label: "+predicted);
		return y >= 0 ? predicted == 0.0 : predicted == 1.0;
	}
}
