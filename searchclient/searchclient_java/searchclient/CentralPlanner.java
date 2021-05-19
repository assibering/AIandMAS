package searchclient;

import java.security.InvalidParameterException;
import java.util.*;

public class CentralPlanner {
	
	public LinkedList<Integer> subgoal_order;
    public LinkedList<Action[][]>[] allplans;
	public State initialState;

	public CentralPlanner(State initialState) {
		this.allplans = null;
		this.subgoal_order =null;
		this.initialState = initialState;
	}

	public void addAllPlans(LinkedList<Action[][]>[] allplans) {
		this.allplans = allplans;
	}

	public void addPlanOrder(LinkedList<Integer> subgoal_order) {
		this.subgoal_order = subgoal_order;
	}

//	public Action[][] getSequencePlan() {
//		int total_agents = this.allplans.length;
//		for (int turn : this.subgoal_order) {
//			Action[][] individualaction = this.allplans[turn].poll();
//		}
//	}

//	public Action[] getJointAction (int stepnumber) {
//
//		Action[] jointAction = new Action[this.individualplans.size()];
//		for (int i=0; i<this.individualplans.size(); i++) {
//			if (stepnumber < this.individualplans.get(i).length) {
//				jointAction[i] = this.individualplans.get(i)[stepnumber][0];
//			}
//		}
//		return jointAction;
//	}
	
	
	
	
}