package searchclient;

import java.util.*;

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

	public Action[] getJointAction(int stepnumber) {
		Action[] jointAction = new Action[this.individualplans.size()];
		for (int i = 0; i < this.individualplans.size(); i++) {
			if (stepnumber < this.individualplans.get(i).length) {
				jointAction[i] = this.individualplans.get(i)[stepnumber][0];
			} else {
				jointAction[i] = Action.NoOp;
			}
		}
		return jointAction;
	}

	public Action[][] getFullPlan() {
		ArrayList<Action[]> result = new ArrayList<>();
		int step = 0;
		while (true) {
			Action[] jointAction = getJointAction(step);
			// If there are no actions to be made, we found the full plan
			if (Arrays.stream(jointAction).allMatch(action -> action == Action.NoOp))
				break;
			result.add(jointAction);
			step++;
		}
		return result.toArray(new Action[0][]);
	}

	public PlanningResult test(State initialState, Action[] jointAction, int step) {
		// If there are no actions to be made, we found the full plan
		if (Arrays.stream(jointAction).allMatch(action -> action == Action.NoOp)) {
			System.err.printf("Found full solution at step %d\n", step);
			return new PlanningResult();
		}
		// If there are actions to be made, we check them for applicability and conflict
		for (int agent = 0; agent < initialState.agentRows.length; agent++) {
			ApplicabilityResult applicabilityResult = initialState.isApplicable(agent, jointAction[agent]);
			if (!applicabilityResult.isApplicable()) {
				System.err.printf("Found applicability issue with action %s by agent %d at step %d due to %s: %c\n",
						jointAction[agent],
						agent, step, applicabilityResult.type, applicabilityResult.getCause());
				PlanningResult result = new PlanningResult();
				result.agent = agent;
				result.step = step;
				result.type = PlanningResult.PlanningResultType.WITH_CONFLICT;
				result.cause = applicabilityResult.getCause();
				return result;
			}
		}
		// Check if joint action is conflicting at this point
		ConflictResult conflictResult = initialState.isConflicting(jointAction);
		if (conflictResult.isConflict()) {
			System.err.printf("Found conflict issue with agent %d at step %d\n", conflictResult.agent1, step);
			PlanningResult result = new PlanningResult();
			result.agent = conflictResult.agent1;
			result.step = step;
			result.type = PlanningResult.PlanningResultType.WITH_CONFLICT;
			result.cause = (char) (conflictResult.agent2 + '0');
			return result;
		}

		return null;
	}

	public PlanningResult delve(State initialState, int step) {
		Action[] jointAction = getJointAction(step);
		PlanningResult result = test(initialState, jointAction, step);
		if (result != null) {
			return result;
		}
		return delve(new State(copyState(initialState), jointAction), step + 1);
	}

	//TODO: how to handle cases where you just need to wait rather than backtrack (like with 0?)
	public PlanningResult plan(State initialState, int step) {
		System.err.printf("Start planning at step %d\n", step);
		Action[] jointAction = getJointAction(step);
		PlanningResult result = test(initialState, jointAction, step);
		if (result != null) {
			return result;
		}
		//If no issue now, plan ahead
		PlanningResult nextAction = plan(new State(copyState(initialState), jointAction), step + 1);

		// Issue resolution phase
		// If we found some conflict, we try to check if delay works at this step
		PlanningResult resolveAttempt;
		Action[][] temporaryPlan;
		Action[][] originalPlan;
		if (nextAction.type == PlanningResult.PlanningResultType.WITH_CONFLICT) {
			// TODO create temporary list of actions for delaying
			originalPlan = Arrays.copyOf(this.individualplans.get(nextAction.agent),
					this.individualplans.get(nextAction.agent).length);
			temporaryPlan = Arrays.copyOf(originalPlan,
					originalPlan.length);
			ArrayList<Action[]> temporaryList = new ArrayList<>();
			Collections.addAll(temporaryList, temporaryPlan);
			// TODO how many NoOps should be added there - maybe difference between current step and error step?
			for (int noOps = step; noOps < nextAction.step; noOps++) {
				temporaryList.add(step, new Action[]{Action.NoOp});
			}
			temporaryPlan = temporaryList.toArray(new Action[0][]);
			this.individualplans.set(nextAction.agent, temporaryPlan);
			// TODO check if this works
			resolveAttempt = delve(initialState, step);
		}
		// If there is no conflict, we propagate that back
		else {
			System.err.printf("Found full solution at step %d\n", step);
			return nextAction;
		}
		// If the conflict occurs at the same point or before, this means we need to revert further
		// We return who is conflicting and revert changes
		// TODO: how to detect if this given conflict is fully resolved and we don't try to solve another one
		if (resolveAttempt.type == PlanningResult.PlanningResultType.WITH_CONFLICT
				&& resolveAttempt.agent == nextAction.agent
				&& resolveAttempt.cause == nextAction.cause) {
			// TODO revert to previous plan
			if (step != 0)
				this.individualplans.set(resolveAttempt.agent, originalPlan);
			return resolveAttempt;
		}
		// If the conflict occurs not at all or with other agent, this means we delayed enough to avoid this problem
		// And we just call next step on this action
		else {
			return plan(new State(copyState(initialState), getJointAction(step)), step + 1);
		}
	}

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
}