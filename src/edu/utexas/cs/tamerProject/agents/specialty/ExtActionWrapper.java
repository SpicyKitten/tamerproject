package edu.utexas.cs.tamerProject.agents.specialty;

/**
 * Simple interface for classes to determine if a wrapper
 * class has exhausted its supply of moves for the current
 * extended action. Method actionsExhausted will return 
 * true if the supply of moves has run out. Separate from
 * class ExtActionAgentWrap because such wrappers are not
 * guaranteed to be a subclass of ExtActionAgentWrap.
 * @author ratha
 */
public interface ExtActionWrapper {
	public abstract boolean actionExhausted();
}
