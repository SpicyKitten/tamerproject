package edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.tetris;

import java.util.function.BiFunction;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import edu.utexas.cs.tamerProject.agents.mtamer.proxy.function.TransitionFactory;

class TetrisTransitionFactory implements TransitionFactory
{
	public TetrisTransitionFactory() {}
	
	@Override
	public BiFunction<Observation, Action, Observation> get(Object... objs) {
		return (o, a) -> 
		{
			edu.utexas.cs.tamerProject.featGen.tetris.TetrisState gameState = new edu.utexas.cs.tamerProject.featGen.tetris.TetrisState();
			for (int i = 0; i < gameState.worldState.length; i++)
				gameState.worldState[i] = o.intArray[i];
	        gameState.blockMobile = o.intArray[gameState.worldState.length] == 1;
	        gameState.currentBlockId = o.intArray[gameState.worldState.length + 1];
		    gameState.currentRotation = o.intArray[gameState.worldState.length + 2];
	    	gameState.currentX = o.intArray[gameState.worldState.length + 3];
	    	gameState.currentY = o.intArray[gameState.worldState.length + 4];
		    gameState.worldWidth = o.intArray[gameState.worldState.length + 5];
	        gameState.worldHeight = o.intArray[gameState.worldState.length + 6];
	        gameState.currentBlockColorId = o.intArray[gameState.worldState.length + 7];
	        for (int actI = 0; actI < a.intArray.length; actI++){ //// take actions
	        	gameState.take_action(a.intArray[actI]);
	            gameState.update();
			}
	        gameState.writeCurrentBlock(gameState.worldState, true);
	        gameState.checkIfRowAndScore();
	        if(!gameState.blockMobile)
	        	gameState.spawn_block();
	        Observation terminal = new Observation(gameState.worldState.length + 8, 0);
            for (int i = 0; i < gameState.worldState.length; i++) {
                terminal.intArray[i] = gameState.worldState[i];
            }
			terminal.intArray[gameState.worldState.length] = gameState.blockMobile?1:0;
			terminal.intArray[gameState.worldState.length + 1] = gameState.currentBlockId;
			terminal.intArray[gameState.worldState.length + 2] = gameState.currentRotation;
			terminal.intArray[gameState.worldState.length + 3] = gameState.currentX;
			terminal.intArray[gameState.worldState.length + 4] = gameState.currentY;
			terminal.intArray[gameState.worldState.length + 5] = gameState.worldWidth;
			terminal.intArray[gameState.worldState.length + 6] = gameState.worldHeight;
			terminal.intArray[gameState.worldState.length + 7] = gameState.currentBlockColorId;
			return terminal;
		};
	}
}
