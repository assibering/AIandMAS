package searchclient;

import java.util.*;

public class GraphSearch {

	public static Action[][] search(State initialState, HeuristicFactory factory) {
		int iterations = 0;

		LinkedList<char[][]> subgoals;

		ArrayList<LinkedList<char[][]>> MAsubgoals = new ArrayList<>();

		//Generate list of subgoals per agent
		for (int agent = 0; agent < initialState.agentRows.length; agent++) {
			MAsubgoals.add(initialState.getAgentSubGoals(agent));
		}

		// Individual actions, used as temporary value per agent
		LinkedList<Action[]> actions;

		LinkedList<LinkedList<Action[]>> individualplans = new LinkedList<>();

		CentralPlanner planner = new CentralPlanner(initialState);

		for (int agent = 0; agent < initialState.agentRows.length; agent++) {
			individualplans.add(new LinkedList<>());
			subgoals = MAsubgoals.get(agent);
			System.err.println("AGENT COLORS");
			System.err.println(Arrays.toString(State.agentColors));

			System.err.println("BOX COLORS");
			System.err.println(Arrays.toString(State.boxColors));

			State s = copyState(initialState);

			// Filtering out agent
			int[] aRows = s.getSingleAgentRow(agent);
			int[] aCols = s.getSingleAgentCol(agent);
			s.agentRows = aRows;
			s.agentCols = aCols;

			System.err.println(Arrays.toString(State.boxColors));
			System.err.println(Arrays.toString(State.agentColors));

			actions = new LinkedList<>();

			for (char[][] subgoal : subgoals) {

				System.err.println("NEW STATE - NEW SUBGOAL");

				// Build temporary state
				char[][] aBoxes = initialState.getSingleAgentBoxes(agent, subgoal);
				s = new State(s.agentRows, s.agentCols, State.agentColors,
						s.walls, aBoxes, State.boxColors, subgoal, s.distancegrid);

				// Print information about currently considered state
				System.err.println("SUBGOAL");
				for (int i = 0; i < subgoal.length; i++) {
					System.err.println(Arrays.toString(s.goals[i]));
				}
				System.err.println("BOXES");
				for (int i = 0; i < aBoxes.length; i++) {
					System.err.println(Arrays.toString(s.boxes[i]));
				}
				System.err.println("WALLS");
				for (int i = 0; i < s.walls.length; i++) {
					for (int j = 0; j < s.walls[i].length; j++) {
						if (s.walls[i][j]) {
							System.err.print("X,");
						} else if (s.agentRows[0] == i && s.agentCols[0] == j) {
							System.err.print("A,");
						} else {
							System.err.print(" ,");
						}
					}
					System.err.println();
				}
				System.err.println("AGENT POSITION");
				System.err.println("Row: " + Arrays.toString(s.agentRows) + ", Col: " + Arrays.toString(s.agentCols));
				System.err.println("AGENT COLORS");
				System.err.println(Arrays.toString(State.agentColors));

				System.err.println("BOX COLORS");
				System.err.println(Arrays.toString(State.boxColors));

				// Build a frontier based on temporary state
				Frontier frontier = new FrontierBestFirst(factory.makeHeuristic(s));

				frontier.add(s);
				HashSet<State> explored = new HashSet<>();

				while (true) {

					//Print a status message every 10000 iteration
					if (++iterations % 10000 == 0) {
						printSearchStatus(explored, frontier);
					}

					if (frontier.isEmpty()) {
						printSearchStatus(explored, frontier);
						return null;
					}

					s = frontier.pop();
					if (s.isGoalState()) {
						//Print state of found subgoal
						System.err.println("SUBGOAL FOUND");
						for (int i = 0; i < subgoal.length; i++) {
							System.err.println(Arrays.toString(s.boxes[i]));
						}
						for (int row = 1; row < s.goals.length - 1; row++) {
							for (int col = 1; col < s.goals[row].length - 1; col++) {
								char goal = s.goals[row][col];
								if ('A' <= goal && goal <= 'Z') {
									s.boxes[row][col] = 0;
									s.walls[row][col] = true;
								}
							}
						}

						Action[][] subactions = s.extractPlan();
						for (Action[] subaction : subactions) {
							System.err.println(Arrays.toString(subaction));
							actions.add(subaction);
						}

						//first_subgoal = false;
						System.err.println();
						break;
					}

					explored.add(s);

					ArrayList<State> States = s.getExpandedStates();
					for (int i = 0; i < States.size(); ++i) {
						State expanded = States.get(i);
						if (!explored.contains(expanded) && !frontier.contains(expanded)) {
							frontier.add(expanded);
						}
					}
				}
			}

			//Add action plan for the agent to planner
			planner.addPlan(actions.toArray(new Action[0][0]));
			individualplans.get(agent).addAll(actions);
		}

		System.err.println("DONE");

		//TODO: replace all this with a central planner
		int agentPlans = individualplans.size();

		// Check applicability issues
		boolean keepCheckingApplicability = true;
		while (keepCheckingApplicability) {
			keepCheckingApplicability = false;
			State testState = copyState(initialState);
			int maximumLength = individualplans.stream()
					.map(List::size)
					.max(Integer::compareTo)
					.orElse(0);
			for (int i = 0; i < maximumLength; i++) {
				for (int j = 0; j < agentPlans; j++) {
					Action[] currentStep = getCurrentStep(individualplans, i);
					if (!initialState.isApplicable(i, currentStep[j])) {
						// Create a copy state of current initial state
						State temporary = copyState(testState);

						ArrayList<Action[]> backtrackedSteps = new ArrayList<>();
						for (int stepBack = i; stepBack >= 0; stepBack--) {
							// TODO: take note of current step
							Action[] backtrackedStep = getCurrentStep(individualplans, stepBack);
							backtrackedSteps.add(backtrackedStep);
							// TODO: backtrack current step *for all agents*
							Action[] backtracking = new Action[backtrackedStep.length];
							for (int k = 0; k < backtracking.length; k++) {
								backtracking[k] = backtrackedStep[k].opposite();
							}
							temporary = new State(temporary, backtracking);
							// TODO: reenact current step with unapplicable agent NoOping at this point
							for (int k = backtrackedSteps.size() - 1; k >= 0; k--) {
								Action[] returnStep = Arrays.copyOf(backtrackedSteps.get(k), backtrackedSteps.get(k).length);
								returnStep[j] = Action.NoOp;
								temporary = new State(temporary, returnStep);
							}
							// TODO: reenact NoOped steps (with no applicability checks) except for last one
							for (int k = backtrackedSteps.size() - 1; k >= 1; k--) {
								Action[] stepToDo = backtrackedSteps.get(k);
								Action[] delayedStep = new Action[backtrackedStep.length];
								Arrays.fill(delayedStep, Action.NoOp);
								delayedStep[j] = stepToDo[j];
								temporary = new State(temporary, delayedStep);
							}
							// TODO: if now applicable, add enough NoOps at that point to delay
							if (temporary.isApplicable(j, backtrackedSteps.get(0)[j])) {
								for (int k = 0; k < backtrackedSteps.size(); k++) {
									Action[] noOpAction = new Action[]{Action.NoOp};
									individualplans.get(i).add(i - k, noOpAction);
								}
								break;
							}
							// TODO: if not, repeat this step with next actions
							else {
								continue;
							}
						}
						// TODO: what if every action is inapplicable? Replan?
						// TODO: conflict check
						// Force recheck in the future
						keepCheckingApplicability = true;
					}
					testState = new State(testState, getCurrentStep(individualplans, i));
				}
			}
		}

		int agentPlanLength = individualplans.stream()
				.map(List::size)
				.max(Integer::compareTo)
				.orElse(0);

		Action[][] finalActions = new Action[agentPlanLength][agentPlans];

		for (int i = 0; i < agentPlanLength; i++) {
			for (int j = 0; j < agentPlans; j++) {
				if (i < individualplans.get(j).size()) {
					finalActions[i][j] = individualplans.get(j).get(i)[0];
				} else {
					finalActions[i][j] = Action.NoOp;
				}
			}
		}
		return finalActions;
	}

	private static State copyState(State initialState) {
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
				State.agentColors,
				initialStateWalls,
				initialStateBoxes,
				State.boxColors,
				initialStateGoals,
				initialStateDistances
		);
	}


	private static Action[] getCurrentStep(LinkedList<LinkedList<Action[]>> actions, int step) {
		Action[] currentStep = new Action[actions.size()];
		for (int agent = 0; agent < actions.size(); agent++) {
			if (actions.get(agent).size() < step) {
				currentStep[agent] = actions.get(agent).get(step)[0];
			} else {
				currentStep[agent] = Action.NoOp;
			}
		}

		return currentStep;
	}

	private static final long startTime = System.nanoTime();

	private static void printSearchStatus(HashSet<State> explored, Frontier frontier) {
		String statusTemplate = "#Expanded: %,8d, #Frontier: %,8d, #Generated: %,8d, Time: %3.3f s\n%s\n";
		double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000d;
		System.err.format(statusTemplate, explored.size(), frontier.size(), explored.size() + frontier.size(),
				elapsedTime, Memory.stringRep());
	}
}
