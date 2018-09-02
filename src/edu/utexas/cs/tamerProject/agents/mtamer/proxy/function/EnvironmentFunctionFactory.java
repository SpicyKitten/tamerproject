package edu.utexas.cs.tamerProject.agents.mtamer.proxy.function;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

public abstract class EnvironmentFunctionFactory
{
	/**
	 * Provides an evaluation function for the Environment generated with parameters 
	 * <code>args</code>
	 */
	public abstract BiFunction<Observation, Observation, Double> evaluationFunc(Object... args);
	/**
	 * Provides a transition function for the Environment generated with parameters 
	 * <code>args</code>
	 */
	public abstract BiFunction<Observation, Action, Observation> transitionFunc(Object... args);
	/**
	 * Constructors for factory production of types
	 */
	protected final Map<String, FactoryConstructor<?>> funcs = new HashMap<String, FactoryConstructor<?>>();
	/**
	 * @return An object denoting a no-argument parameter list for a constructor
	 */
	protected <T> Supplier<T> noArgs(Supplier<T> s)
	{
		return s;
	}
	/**
	 * @return An object denoting a variable-argument parameter list for a constructor
	 */
	protected <T> Function<Object[],T> varArgs(Function<Object[], T> f)
	{
		return f;
	}
	/**
	 * Allows for the creation of instances of classes with either 
	 * variable argument or empty parameter lists. Parse arguments in the class constructors
	 * and get() methods. Intended use is through the inherited get() method for this class.
	 * Any class instantiated through this method should have defined a constructor that
	 * takes the expected types of parameter lists (Object... or Void).
	 * @param s The given name of the function's intention
	 * @param f The constructor of the actual function factory
	 * @return 
	 */
	protected <T> void addConstructor(String s, Function<Object[], T> f)
	{
		funcs.put(s, new FactoryConstructor<T>(f));
	}
	/**
	 * Allows for the creation of instances of classes with either 
	 * variable argument or empty parameter lists. Parse arguments in the class constructors
	 * and get() methods. Intended use is through the inherited get() method for this class.
	 * Any class instantiated through this method should have defined a constructor that
	 * takes the expected types of parameter lists (Object... or Void).
	 * @param s The given name of the function's intention
	 * @param f The constructor of the actual function factory
	 * @return 
	 */
	protected <T> void addConstructor(String s, Supplier<T> f)
	{
		funcs.put(s, new FactoryConstructor<T>(f));
	}
	/**
	 * Intended for use with function factories. Gets the function generated
	 * by the factory with parameters <code>objs</code>. Keep in mind that the
	 * parameters are passed as a whole to the factory constructor and factory
	 * function request. One solution is to pass in two Object[]s, and then use
	 * the first array as the parameters for the constructor of the FunctionFactory
	 * and the second array as the parameters for the {@link FunctionFactory#get(Object...) FunctionFactory::get} request
	 * @param target The given name of the intention of the factory's produced function
	 * @param objs All parameters that are passed to the factory constructor and function request
	 * @param <A> Type parameter of result parameters
	 * @param <B> Type parameter of result parameters
	 * @param <C> Type parameter of result return type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <A,B,C> BiFunction<A,B,C> get(String target, Object... objs) {
		if(!funcs.containsKey(target))
			throw new NoSuchElementException("No such function factory "+target+"!");
		Object o = funcs.get(target).construct(objs);
		return (BiFunction<A,B,C>)((FunctionFactory<A, B, C>)o).get(objs);
	}
}