package searchclient;

import java.awt.Desktop.Action;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

public class CentralPlanner {
	
	public LinkedList<Integer> subgoal_order;
    public LinkedList<Action[][]>[] allplans;
	public State initialState;
	public int sequenceLength;
	
	public CentralPlanner(State initialState) {
		this.allplans = null;
		this.subgoal_order = new LinkedList<Integer>();
		this.initialState = initialState;
		this.sequenceLength = 0;
	}
	
	public void addSubPlan(Action[][] subplan, int agent) {
		this.allplans[agent].add(subplan);
		this.sequenceLength += subplan.length;
	}
	
	public void addPlanOrder(LinkedList<Integer> subgoal_order) {
		this.subgoal_order = subgoal_order;
	}
	
//	public Action[][] createPlan() {
//		
//		Action[][] totalplan = new Action[this.allplans.length][this.sequenceLength];
//		For (int i=0; i<this.allplans.length; i++) {
//			Arrays.fill(totalplan[i], Action.NoOp);
//		}
//		
//		int[] agent_step = new int[this.allplans.length];
//		Arrays.fill(agent_step, 0);
//		
//		State[] agent_state = new State[this.allplans.length];
//		Arrays.fill(agent_state, this.initialState);
//		
//		
//		for (int agent : this.subgoal_order) {
//			Action[][] agent_plan = this.allplans[agent].poll();
//			
//			for (int action=0; action<agent_plan.length; action++) {
//				Action currentAction = agent_plan[action][0];
//				
//				boolean legalmove = false;
//				int NoOp_count = 0;
//				while (!legalmove) {
//					
//					if (agent_state[agent].isApplicable(agent, currentAction)) {
//						
//						Action[] jointAction = new Action[this.allplans.length];
//						for (int i=0; i<jointAction.length; i++) {
//							if (i != agent) {
//								jointAction[i] = totalplan[i][agent_step[agent]];
//							} else {
//								jointAction[i] = currentAction;
//							}
//						}
//						
//						if (!agent_state[agent].isConflicting(jointAction)) {
//							legalmove = true;
//						} else {
//							NoOp_count += 1;
//						}
//						
//					} else {
//						NoOp_count += 1;
//					}
//					
//				}
//				
//				
//				
//				
//				
//				agent_step[agent] = agent_step[agent] + NoOp_count + 1;
//				totalplan[agent][agent_step[agent]] = currentAction;
//				
//				
//				agent_plan[agent] = new State(agent_plan[agent], jointAction[i]);
//				
//				
//			}
//			
//		}
//
//	}
	
	
	
	static State copyState(State initialState) {
		char[][] initialStateBoxes = new char[initialState.boxes.length][];
		for (int boxRow = 0; boxRow < initialStateBoxes.length; boxRow++) {
			initialStateBoxes[boxRow] =
					Arrays.copyOf(initialState.boxes[boxRow], initialState.boxes[boxRow].length);
		}
		char[][] initialStateGoals = new char[initialState.goals.length][];
		for (int boxRow = 0; boxRow < initialStateGoals.length; boxRow++) {
			initialStateGoals[boxRow] =
					Arrays.copyOf(initialState.goals[boxRow], initialState.goals[boxRow].length);
		}
		boolean[][] initialStateWalls = new boolean[initialState.walls.length][];
		for (int boxRow = 0; boxRow < initialStateWalls.length; boxRow++) {
			initialStateWalls[boxRow] =
					Arrays.copyOf(initialState.walls[boxRow], initialState.walls[boxRow].length);
		}
		int[][] initialStateDistances = new int[initialState.distancegrid.length][];
		for (int boxRow = 0; boxRow < initialStateDistances.length; boxRow++) {
			initialStateDistances[boxRow] =
					Arrays.copyOf(initialState.distancegrid[boxRow], initialState.distancegrid[boxRow].length);
		}
		return new State(Arrays.copyOf(initialState.agentRows, initialState.agentRows.length),
				Arrays.copyOf(initialState.agentCols, initialState.agentCols.length),
				Arrays.copyOf(initialState.agentColors, initialState.agentColors.length),
				initialStateWalls,
				initialStateBoxes,
				Arrays.copyOf(initialState.boxColors, initialState.boxColors.length),
				initialStateGoals,
				initialStateDistances
		);
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