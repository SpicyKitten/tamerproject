package edu.utexas.cs.tamerProject.agents.mtamer.proxy.function;

import java.util.function.Function;
import java.util.function.Supplier;

public class FactoryConstructor<T> 
{
	public Function<Object[], T> myFunction;
	public Supplier<T> mySupplier;
	public FactoryConstructor(Supplier<T> s)
	{
		assert s != null : "A constructor must be constructable!";
		myFunction = null;
		mySupplier = s;
	}
	public FactoryConstructor(Function<Object[], T> f)
	{
		assert f != null : "A constructor must be constructable!";
		myFunction = f;
		mySupplier = null;
	}
	public T construct(Object[] args)
	{
		if(myFunction != null)
			return myFunction.apply(args);
		if(mySupplier != null)
			return mySupplier.get();
		throw new IllegalStateException("A constructor must take 0 or 1+ arguments!!!");
	}
}
