package searchclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

public class CentralPlanner {
	
	public LinkedList<Action[][]> individualplans;
	public State initialState;
	
	public CentralPlanner(State initialState) {
		this.individualplans = new LinkedList<Action[][]>();
		this.initialState = initialState;
		
	}
	
	public void addPlan(Action[][] plan) {
		this.individualplans.addLast(plan);
		System.err.println("PLANS: " + this.individualplans.size());
	}
	
	public Action[] getJointAction (int stepnumber) {
		
		Action[] jointAction = new Action[this.individualplans.size()];
		for (int i=0; i<this.individualplans.size(); i++) {
			if (stepnumber < this.individualplans.get(i).length) {
				jointAction[i] = this.individualplans.get(i)[stepnumber][0];
			}
		}
		return jointAction;
	}
	
	
	
	
}