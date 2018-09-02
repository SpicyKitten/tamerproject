package edu.utexas.cs.tamerProject.agents.mtamer.trackable;

import java.util.Map;

import javax.swing.JTable;

public interface TableTrackable 
{
	public abstract void setHistoryTracker(JTable table);
	public abstract void updateHistory(Map<String, Object> params);
}
