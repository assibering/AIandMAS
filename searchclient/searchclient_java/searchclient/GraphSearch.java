package searchclient;


import java.util.*;

import static searchclient.CentralPlanner.copyState;

//import java.awt.Desktop.Action;
//import java.awt.Desktop.Action;

public class GraphSearch {

    public static Action[][] search(State initialState, Frontier frontier) {
		boolean outputFixedSolution = false;

		if (outputFixedSolution) {
			//Part 1:
			//The agents will perform the sequence of actions returned by this method.
			//Try to solve a few levels by hand, enter the found solutions below, and run them:

			return new Action[][]{
					{Action.PushEE}
			};

		} else {

			int iterations = 0;
			CentralPlanner planner = new CentralPlanner(copyState(initialState));

			State originalState = copyState(initialState);

			boolean greedy = false;
			boolean wastar = false;
			boolean astar = false;

			if (frontier.getName().contains("greedy")) {
				greedy = true;
			}

			if (frontier.getName().contains("WA*")) {
				wastar = true;
			}

			if (frontier.getName().contains("A*")) {
				astar = true;
			}

			FrontierBestFirst centralPlannerFrontier = null;
			if (greedy) {
				centralPlannerFrontier = new FrontierBestFirst(new HeuristicGreedy(initialState));
			} else if (astar) {
				centralPlannerFrontier = new FrontierBestFirst(new HeuristicAStar(initialState));
			} else if (wastar) {
				centralPlannerFrontier = new FrontierBestFirst(new HeuristicWeightedAStar(initialState, 5));
			}
			planner.setFrontier(centralPlannerFrontier);

			LinkedList<char[][]> subgoals = new LinkedList<char[][]>();
			LinkedList<char[][]>[] MAsubgoals = new LinkedList[initialState.agentRows.length];


			for (int agent = 0; agent < initialState.agentRows.length; agent++) {
				subgoals = initialState.getAgentSubGoals(agent);
				MAsubgoals[agent] = subgoals;
			}

			//Lists to store individual plans, and order to execute plans.
			LinkedList<Integer> subgoal_actions_order = new LinkedList<Integer>();
			LinkedList<Action[][]>[] all_plans = new LinkedList[initialState.agentRows.length];
			for (int i = 0; i < all_plans.length; i++) {
				all_plans[i] = new LinkedList<Action[][]>();
			}

			//Maintaining State
			State s = copyState(initialState);

			//State from an agent's perspective
			State agent_s = copyState(s);

			searchclient.Color[] initcoloragent = s.agentColors;
			searchclient.Color[] initcolorbox = s.boxColors;


			//Integers to store
			int agents = s.agentRows.length;

			int subgoal_total = 0;
			for (int i = 0; i < s.goals.length; i++) {
				for (int j = 0; j < s.goals[i].length; j++) {
					if (s.goals[i][j] != 0) {
						subgoal_total += 1;
					}
				}
			}
			int subgoal_count = 0;
			int sequence_solution_length = 0;
			int unreachable_count = 0;


			// Mark agents as initially blocked by other agents
			boolean[] blockedAgents = new boolean[agents];
			for (int agent = 0; agent < agents; agent++) {
				List<Character> blocksByAgent = AgentWiggleSearch.checkAgentBlocks(originalState, agent);
				for (char blockedAgent : blocksByAgent) {
					blockedAgents[blockedAgent - '0'] = true;
				}
			}

			for (int agent = 0; agent < agents; agent++) {
				if (blockedAgents[agent]) {
					Action[][] wigglePlan = AgentWiggleSearch.search(copyState(originalState), centralPlannerFrontier, agent);
					if (wigglePlan != null) {
						for (int involvedAgents = 0; involvedAgents < wigglePlan[0].length; involvedAgents++) {
							Action[][] agentWigglePlan = new Action[wigglePlan.length][];
							for (int i = 0; i < wigglePlan.length; i++) {
								agentWigglePlan[i] = new Action[]{wigglePlan[i][involvedAgents]};
								all_plans[involvedAgents].add(agentWigglePlan);
								sequence_solution_length += wigglePlan.length;
								subgoal_actions_order.addLast(agent);
							}
						}
					}
				}
			}

			while (subgoal_count < subgoal_total) {

				//	while (!NB_and_R_subgoals.isEmpty()) {

				boolean good_subgoal = false;
				char[][] currentsubgoal = new char[s.boxes.length][s.boxes[0].length];
				int agentX = 0;
				LinkedList<char[][]> subgoals1 = new LinkedList<char[][]>();

				int subgoal_index = 0;
				detectsubgoal:
				for (int agent = 0; agent < MAsubgoals.length; agent++) {
					subgoals1 = s.getAgentSubGoals(agent);
					subgoal_index = 0;
					for (char[][] subgoal : subgoals1) {
						if (nonBlocking(s, subgoal)) {

							State reachability_test_state = copyState(s);
							reachability_test_state.goals = subgoal;
							boolean is_reachable = isreachable(reachability_test_state);

//                			Coordinates reachable = reachability_test_state.conflictRecognition(agent);

							if (is_reachable) {
								good_subgoal = true;
								currentsubgoal = subgoal;
								agentX = agent;
								break detectsubgoal;
							}
						}
					}
					subgoal_index += 1;
				}

				if (!good_subgoal) {
//            		System.err.println("ALL SUBGOALS ARE UNREACHABLE");
					subgoal_count -= 1;

					int agentToMove = 0;
					detectblocker:
					for (int i = 0; i < s.agentRows.length; i++) {
						for (int agent = 0; agent < MAsubgoals.length; agent++) {
							subgoals1 = s.getAgentSubGoals(agent);
							subgoal_index = 0;
							for (char[][] subgoal : subgoals1) {
								if (nonBlocking(s, subgoal)) {

									State reachability_test_state = copyState(s);
									reachability_test_state.goals = subgoal;
									boolean is_reachable = isreachable_removeAgent(reachability_test_state, i);


//                        			Coordinates reachable = reachability_test_state.conflictRecognition(agent);

									if (is_reachable) {
										agentToMove = i;
										good_subgoal = true;
										currentsubgoal = subgoal;
										agentX = agentToMove;
										break detectblocker;
									}
								}
							}
							subgoal_index += 1;
						}
					}

					int agent_row = s.agentRows[agentToMove];
					int agent_col = s.agentRows[agentToMove];

					int[] goal_dest = newAgentPosition(s, agentToMove);
					currentsubgoal = new char[currentsubgoal.length][currentsubgoal[0].length];
					currentsubgoal[goal_dest[0]][goal_dest[1]] = String.valueOf(0).charAt(0);

				}
				s.agentColors = initcoloragent;
				s.boxColors = initcolorbox;


				searchclient.Color currentColor = s.agentColors[agentX];


				char[][] unreachedSubgoals = new char[s.goals.length][s.goals[0].length];

				for (int i = 0; i < currentsubgoal.length; i++) {
					for (int j = 0; j < currentsubgoal[i].length; j++) {
						if (currentsubgoal[i][j] == 0) {
							unreachedSubgoals[i][j] = s.goals[i][j];
						}
					}
				}

				LinkedList<Integer> same_color_agents = new LinkedList<Integer>();
				for (int i = 0; i < agents; i++) {
					if (currentColor == s.agentColors[i]) {
						same_color_agents.add(i);
					}
				}

				LinkedList<char[][]> subgoal_split = new LinkedList<char[][]>();

				char[][] currentlevel = new char[s.boxes.length][s.boxes[0].length];

				for (int i = 0; i < currentlevel.length; i++) {
					for (int j = 0; j < currentlevel[i].length; j++) {
						currentlevel[i][j] = ' ';
					}
				}


				for (int i = 0; i < s.agentRows.length; i++) {
					char b = (char) (i + '0');
					currentlevel[s.agentRows[i]][s.agentCols[i]] = b;
				}

				for (int i = 0; i < s.boxes.length; i++) {
					for (int j = 0; j < s.boxes[i].length; j++) {
						char box = s.boxes[i][j];
						if ('A' <= box && box <= 'Z') {
							currentlevel[i][j] = box;
						}
					}
				}

				for (int i = 0; i < s.walls.length; i++) {
					for (int j = 0; j < s.walls[i].length; j++) {
						if (s.walls[i][j]) {
							currentlevel[i][j] = 'W';
						}
					}
				}

				boolean agent_found = false;
				find_solvable_agent:
				for (int agentNr : same_color_agents) {
					agent_found = reachableBy(s, agentNr);

					if (agent_found) {
						subgoal_count += 1;
						int[] aRows = s.getSingleAgentRow(agentNr);
						int[] aCols = s.getSingleAgentCol(agentNr);
						char[][] aBoxes = s.getSingleAgentBoxes(agentNr);
						boolean[][] aWalls = s.otherEntities(agentNr);

						searchclient.Color[] agentColor = new searchclient.Color[initialState.agentColors.length];
						searchclient.Color[] boxColor = new searchclient.Color[initialState.boxColors.length];
						agentColor[0] = currentColor;
						for (int i = 0; i < boxColor.length; i++) {
							if (initialState.boxColors[i] == currentColor) {
								boxColor[i] = currentColor;
							}
						}

						agent_s = new State(aRows, aCols, agentColor,
								aWalls, aBoxes, boxColor, currentsubgoal, s.distancegrid);


						subgoal_split = agent_s.splitSubgoal(agent_s.goals, agentNr);

						for (char[][] subsubgoal : subgoal_split) {

							int[] goal_coord = new int[]{0, 0};

							char subgoal_char = 0;
							outer1:
							for (int i = 0; i < subsubgoal.length; i++) {
								for (int j = 0; j < subsubgoal[i].length; j++) {
									if (subsubgoal[i][j] != 0) {
										subgoal_char = subsubgoal[i][j];
										goal_coord = new int[]{i, j};
										break outer1;
									}
								}
							}

							int[][] dist_grid = agent_s.getdistance(goal_coord[0], goal_coord[1]);


							agent_s = new State(aRows, aCols, agentColor,
									aWalls, aBoxes, boxColor, subsubgoal, dist_grid);

							if (greedy) {
								frontier = new FrontierBestFirst(new HeuristicGreedy(agent_s));
							} else if (astar) {
								frontier = new FrontierBestFirst(new HeuristicAStar(agent_s));
							} else if (wastar) {
								frontier = new FrontierBestFirst(new HeuristicWeightedAStar(agent_s, 0));
							}


							frontier.add(agent_s);
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


								agent_s = frontier.pop();

								if (agent_s.isGoalState()) {

									//Update State
									s.agentRows[agentNr] = agent_s.getSingleAgentRow(0)[0];
									s.agentCols[agentNr] = agent_s.getSingleAgentCol(0)[0];

									//Update boxes
									aBoxes = agent_s.getSingleAgentBoxes(0);
									for (int i = 0; i < aBoxes.length; i++) {
										for (int j = 0; j < aBoxes[i].length; j++) {

											char agent_box = aBoxes[i][j];
											char s_box = s.boxes[i][j];
											boolean agent_wall = agent_s.walls[i][j];


											if ('A' <= agent_box && agent_box <= 'Z') {
												if (agent_box != s_box) {
													s.boxes[i][j] = agent_box;
												}
											} else if ('A' <= s_box && s_box <= 'Z') {
												if (agent_box != s_box) {
													if (!agent_wall) {
														s.boxes[i][j] = agent_box;
													}
												}
											}
										}
									}

									//Turn achieved box-subgoal into a wall
									for (int row = 1; row < agent_s.goals.length - 1; row++) {
										for (int col = 1; col < agent_s.goals[row].length - 1; col++) {
											char goal = agent_s.goals[row][col];
											if ('A' <= goal && goal <= 'Z') {
												s.boxes[row][col] = 0;
												s.walls[row][col] = true;
											}
										}
									}

									//Update all goals in S
									s.goals = unreachedSubgoals;


									aRows = agent_s.getSingleAgentRow(0);
									aCols = agent_s.getSingleAgentCol(0);
									aBoxes = agent_s.getSingleAgentBoxes(0);
									aWalls = agent_s.otherEntities(0);

									Action[][] subactions = agent_s.extractPlan();
									sequence_solution_length += subactions.length;
									all_plans[agentNr].addLast(subactions);
									subgoal_actions_order.addLast(agentNr);

									while (!frontier.isEmpty()) {
										frontier.pop();
									}

//									System.err.println();
									break;
								}


								explored.add(agent_s);


								ArrayList<State> States = agent_s.getExpandedStates();
								for (int i = 0; i < States.size(); ++i) {
									State expanded = States.get(i);
									if (!explored.contains(expanded) && !frontier.contains(expanded)) {
										frontier.add(expanded);
									}
								}
							}

						}
						break find_solvable_agent;
					}

				}

				if (!agent_found) {

				}


				s.agentColors = initcoloragent;
				s.boxColors = initcolorbox;

			}

			Action[][] finalactions = new Action[sequence_solution_length][agents];
			for (Action[] row : finalactions) {
				Arrays.fill(row, Action.NoOp);
			}

			int minimumLength = Integer.MIN_VALUE;
			for (LinkedList<Action[][]> agent : all_plans) {
				LinkedList<Action[]> resolvedActions = new LinkedList<>();
				for (Action[][] steps : agent) {
					resolvedActions.addAll(Arrays.asList(steps));
				}
				if (resolvedActions.size() > minimumLength) {
					minimumLength = resolvedActions.size();
				}
			}

			// Plan padding
			// If we know when the box is picked up (we get it from list of plans)
			// Then we know how long our plan must be in minimum to work with this
			// We can then pad with NoOps until this length is met
			// So that in worst case we don't run out of steps to NoOp on
			// But what would be the length? Since we have global order, we
			// know that for us to work, one of these agents will have to move
			int[] countPlans = new int[originalState.agentRows.length];
			int[] subactionLengths = new int[originalState.agentRows.length];
			for (int i = 0; i < subgoal_actions_order.size(); i++) {
				int turn = subgoal_actions_order.get(i);
				Action[][] subplan = all_plans[turn].get(countPlans[turn]++);
				int currentMaximumLength = Arrays.stream(subactionLengths).max().orElse(0);
				if (subplan.length + subactionLengths[turn] < currentMaximumLength) {
					ArrayList<Action[]> subplanList = new ArrayList<>();
					Collections.addAll(subplanList, subplan);
					for (int j = subplan.length + subactionLengths[turn]; j < currentMaximumLength; j++) {
						subplanList.add(0, new Action[]{Action.NoOp});
					}
					subplan = subplanList.toArray(new Action[0][]);
				}
				planner.addSubplan(subplan, turn);
				subactionLengths[turn] += subplan.length;
			}
			try {
				planner.plan(originalState, 0);
			} catch (StackOverflowError ignored) {
			}

			// If we fail to find a solution, then at least go sequential - will suck, but at least
			// it may work

			PlanningResult delveResult = planner.delve(originalState, 0);

			if (delveResult.type == PlanningResult.PlanningResultType.WITH_CONFLICT
					|| delveResult.step < Arrays.stream(subactionLengths).sum()) {
				int index = 0;
				for (int turn : subgoal_actions_order) {
					Action[][] currentAction = all_plans[turn].poll();
					if (currentAction == null) {
						break;
					}
					for (Action[] actions : currentAction) {
						finalactions[index][turn] = actions[0];
						index += 1;
					}
				}
				return finalactions;
			}

			return planner.getFullPlan();
		}
	}

    public static boolean nonBlocking(State currentState, char[][] subgoal) {

		int goal_row = -1;
		int goal_col = -1;

		outerloop:
		for (int i = 0; i < subgoal.length; i++) {
			for (int j = 0; j < subgoal[i].length; j++) {
				if (subgoal[i][j] != 0) {
					goal_row = i;
					goal_col = j;
					break outerloop;
				}
			}
		}

		State test_state = copyState(currentState);

		test_state.walls[goal_row][goal_col] = true;

		boolean[] wallneighboors = new boolean[4];
		wallneighboors[0] = test_state.walls[goal_row - 1][goal_col];
		wallneighboors[1] = test_state.walls[goal_row + 1][goal_col];
		wallneighboors[2] = test_state.walls[goal_row][goal_col - 1];
		wallneighboors[3] = test_state.walls[goal_row][goal_col + 1];

		LinkedList<int[]> goal_neighboors = new LinkedList<int[]>();
		for (int i = 0; i < wallneighboors.length; i++) {
			if (!wallneighboors[i]) {
				if (i == 0) {
					goal_neighboors.add(new int[]{goal_row - 1, goal_col});
				} else if (i == 1) {
					goal_neighboors.add(new int[]{goal_row + 1, goal_col});
				} else if (i == 2) {
					goal_neighboors.add(new int[]{goal_row, goal_col - 1});
				} else if (i == 3) {
					goal_neighboors.add(new int[]{goal_row, goal_col + 1});
				}
			}
		}

		int[] dst = goal_neighboors.poll();
		if (dst == null) {
			return true;
		}
		int[][] region = test_state.getdistance(dst[0], dst[1]);
		for (int i = 0; i < region.length; i++) {
			for (int j = 0; j < region[i].length; j++) {
				if (region[i][j] != 0) {
					region[i][j] = 1;
				}
			}
		}
		int currRegion = 2;
		for (int i = 0; i < goal_neighboors.size(); i++) {
			dst = goal_neighboors.get(i);
			if (region[dst[0]][dst[1]] == 0) {
				int[][] region_temp = test_state.getdistance(dst[0], dst[1]);
				for (int j = 0; j < region_temp.length; j++) {
					for (int k = 0; k < region_temp[j].length; k++) {
						if (region_temp[j][k] != 0) {
							region[j][k] = currRegion;
						}
					}
				}
			}
			currRegion += 1;
		}

		currRegion -= 1;

		LinkedList<Character>[] agent_chars = new LinkedList[currRegion];
		for (int i = 0; i < agent_chars.length; i++) {
			agent_chars[i] = new LinkedList<Character>();
		}

		LinkedList<Character>[] box_chars = new LinkedList[currRegion];
		for (int i = 0; i < box_chars.length; i++) {
			box_chars[i] = new LinkedList<Character>();
		}

		LinkedList<Character>[] goal_chars = new LinkedList[currRegion];
		for (int i = 0; i < goal_chars.length; i++) {
			goal_chars[i] = new LinkedList<Character>();
		}

		//Adding boxes and goals in region
		int thisregion;
		for (int i = 0; i < region.length; i++) {
			for (int j = 0; j < region[i].length; j++) {
				thisregion = region[i][j];
				if (thisregion != 0) {

					if (test_state.boxes[i][j] != 0) {
						box_chars[thisregion - 1].add(test_state.boxes[i][j]);
					} else if (test_state.goals[i][j] != 0) {
						goal_chars[thisregion - 1].add(test_state.goals[i][j]);
					}

				}
			}
		}

		//Adding agents in region
		for (int i = 0; i < test_state.agentRows.length; i++) {
			int agent_row = test_state.agentRows[i];
			int agent_col = test_state.agentCols[i];

			int agentregion = region[agent_row][agent_col];

			if (agentregion != 0) {
				char agent_char = (char) (i + '0');
				agent_chars[agentregion - 1].add(agent_char);
			}
			if (agent_row == goal_row && agent_row == goal_row) {
				char agent_char = (char) (i + '0');
				for (int regions_count = 0; regions_count < agent_chars.length; regions_count++) {
					agent_chars[regions_count].add(agent_char);
				}
			}
		}


		for (int i = 0; i < agent_chars.length; i++) {

			for (char goal : goal_chars[i]) {
				// Box check
				boolean boxFound = false;
				Color boxColor = null;
				for (int j = 0; j < box_chars[i].size(); j++) {
					if (box_chars[i].get(j).equals(goal)) {
						boxColor = test_state.boxColors[box_chars[i].get(j) - 'A'];
						box_chars[i].remove(j);
						boxFound = true;
						break;
					}
				}
				if (!boxFound) {
					return false;
				}

				// Agent check
				boolean agentFound = false;
				for (int j = 0; j < agent_chars[i].size(); j++) {
					if (test_state.agentColors[agent_chars[i].get(j) - '0'].equals(boxColor)) {
						agentFound = true;
						break;
					}
				}
				if (!agentFound) {
					return false;
				}
			}
		}

		return true;

	}

	public static boolean reachableBy(State s, int agent) {

		State test = copyState(s);

		int subgoal_row = 0;
		int subgoal_col = 0;
		char subgoal_char = 0;

		outer:
		for (int i = 0; i < test.goals.length; i++) {
			for (int j = 0; j < test.goals[i].length; j++) {
				if (test.goals[i][j] != 0) {
					subgoal_row = i;
					subgoal_col = j;
					subgoal_char = test.goals[i][j];
					break outer;
				}
			}
		}

		if ('A' <= subgoal_char && subgoal_char <= 'Z') {

			int color_index = subgoal_char - 'A';
			Color currentColor = test.boxColors[color_index];

			for (int i = 0; i < test.boxes.length; i++) {
				for (int j = 0; j < test.boxes[i].length; j++) {
					if (test.boxes[i][j] != 0) {
						if (test.boxColors[test.boxes[i][j] - 'A'] != currentColor) {
							test.walls[i][j] = true;
						}
					}
				}
			}

			for (int i = 0; i < test.agentRows.length; i++) {
				if (i != agent) {
					test.walls[test.agentRows[i]][test.agentCols[i]] = true;
				}
			}

			int[][] reachables = test.getdistance(subgoal_row, subgoal_col);

			boolean reachable = false;

			if (reachables[test.agentRows[agent]][test.agentCols[agent]] > 0) {
				reachable = true;
			}

			return reachable;

		} else {

			int agentNr = Integer.parseInt(String.valueOf(subgoal_char));
			int[][] reachables = test.getdistance(subgoal_row, subgoal_col);

			boolean reachable = false;
			if (reachables[test.agentRows[agentNr]][test.agentCols[agentNr]] > 0) {
				reachable = true;
			}

			return reachable;
		}


    }


    public static boolean isreachable(State s) {

		State test = copyState(s);

		int subgoal_row = 0;
		int subgoal_col = 0;
		char subgoal_char = 0;

		outer:
		for (int i = 0; i < test.goals.length; i++) {
			for (int j = 0; j < test.goals[i].length; j++) {
				if (test.goals[i][j] != 0) {
					subgoal_row = i;
					subgoal_col = j;
					subgoal_char = test.goals[i][j];
					break outer;
				}
			}
		}


		if ('A' <= subgoal_char && subgoal_char <= 'Z') {

			int color_index = subgoal_char - 'A';
			Color currentColor = test.boxColors[color_index];

			for (int i = 0; i < test.boxes.length; i++) {
				for (int j = 0; j < test.boxes[i].length; j++) {
					if (test.boxes[i][j] != 0) {
						if (test.boxColors[test.boxes[i][j] - 'A'] != currentColor) {
							test.walls[i][j] = true;
						}
					}
				}
			}
//System.err.println("boxcolor: " + currentColor);
			LinkedList<Integer> same_color_agents = new LinkedList<Integer>();
			for (int i = 0; i < test.agentRows.length; i++) {
				if (currentColor == s.agentColors[i]) {
//        			System.err.println("agentcolor: " + s.agentColors[i]);
					same_color_agents.add(i);
				} else {

					test.walls[test.agentRows[i]][test.agentCols[i]] = true;
				}
			}

			if (test.walls[subgoal_row][subgoal_col]) {
				for (int i = 0; i < test.agentRows.length; i++) {
					if (currentColor != s.agentColors[i]) {
						if (test.agentRows[i] == subgoal_row) {
							if (test.agentCols[i] == subgoal_col) {
								return false;
							}
						}
					}
				}
			}

			int[][] reachables = test.getdistance(subgoal_row, subgoal_col);

			boolean reachable = false;
			for (int agent : same_color_agents) {
				if (reachables[test.agentRows[agent]][test.agentCols[agent]] > 0) {
					reachable = true;
					break;
				}
			}

			reachable = false;
			for (int i = 0; i < test.boxes.length; i++) {
				for (int j = 0; j < test.boxes[i].length; j++) {
					if (test.boxes[i][j] == subgoal_char) {
						if (reachables[i][j] > 0) {
							reachable = true;
						}
					}
				}
			}

			return reachable;

		} else {

			int agentNr = Integer.parseInt(String.valueOf(subgoal_char));
			int[][] reachables = test.getdistance(subgoal_row, subgoal_col);

			boolean reachable = false;
			if (reachables[test.agentRows[agentNr]][test.agentCols[agentNr]] > 0) {
				reachable = true;
			}

			return reachable;
		}


	}


	public static boolean isreachable_removeAgent(State s, int agent) {

		State test = copyState(s);

		int subgoal_row = 0;
		int subgoal_col = 0;
		char subgoal_char = 0;

		outer:
		for (int i = 0; i < test.goals.length; i++) {
			for (int j = 0; j < test.goals[i].length; j++) {
				if (test.goals[i][j] != 0) {
					subgoal_row = i;
					subgoal_col = j;
					subgoal_char = test.goals[i][j];
					break outer;
				}
			}
		}


		if ('A' <= subgoal_char && subgoal_char <= 'Z') {

			int color_index = subgoal_char - 'A';
			Color currentColor = test.boxColors[color_index];

			for (int i = 0; i < test.boxes.length; i++) {
				for (int j = 0; j < test.boxes[i].length; j++) {
					if (test.boxes[i][j] != 0) {
						if (test.boxColors[test.boxes[i][j] - 'A'] != currentColor) {
							test.walls[i][j] = true;
						}
					}
				}
			}

			LinkedList<Integer> same_color_agents = new LinkedList<Integer>();
			for (int i = 0; i < test.agentRows.length; i++) {
				if (currentColor == s.agentColors[i]) {
					same_color_agents.add(i);
				} else {
					if (i != agent) {
						test.walls[test.agentRows[i]][test.agentCols[i]] = true;
					}
				}
			}

			if (test.walls[subgoal_row][subgoal_col]) {
				for (int i = 0; i < test.agentRows.length; i++) {
					if (currentColor != s.agentColors[i]) {
						if (test.agentRows[i] == subgoal_row) {
							if (test.agentCols[i] == subgoal_col) {
								return false;
							}
						}
					}
				}
			}

			int[][] reachables = test.getdistance(subgoal_row, subgoal_col);

			boolean reachable = false;
			for (int agent1 : same_color_agents) {
				if (reachables[test.agentRows[agent1]][test.agentCols[agent1]] > 0) {
					reachable = true;
					break;
				}
			}

			reachable = false;
			for (int i = 0; i < test.boxes.length; i++) {
				for (int j = 0; j < test.boxes[i].length; j++) {
					if (test.boxes[i][j] == subgoal_char) {
						if (reachables[i][j] > 0) {
							reachable = true;
						}
					}
				}
			}

			return reachable;

		} else {

			int agentNr = Integer.parseInt(String.valueOf(subgoal_char));
			int[][] reachables = test.getdistance(subgoal_row, subgoal_col);

			boolean reachable = false;
			if (reachables[test.agentRows[agentNr]][test.agentCols[agentNr]] > 0) {
				reachable = true;
			}

			return reachable;
		}


	}


	public static int[] newAgentPosition(State s, int agent) {

		State test = copyState(s);
		for (int otheragent = 0; otheragent < test.agentRows.length; otheragent++) {
			if (otheragent != agent) {
				test.walls[test.agentRows[otheragent]][test.agentCols[otheragent]] = true;
			}
		}

		int[][] reachables = test.getdistance(test.agentRows[agent], test.agentCols[agent]);


		int[] goalcell = new int[]{test.agentRows[agent] + 1, test.agentCols[agent] + 1};
		LinkedList<int[]> neighboorcells = new LinkedList<int[]>();
		neighboorcells.add(new int[]{test.agentRows[agent], test.agentCols[agent]});
		boolean reachable = false;

		int count = 0;
		while (!reachable) {

			if (count > 200) {
				break;
			}

			int[] currentcell = neighboorcells.poll();
			State test1 = copyState(test);
			test1.agentRows[agent] = currentcell[0];
			test1.agentCols[agent] = currentcell[1];

			if (isreachable(test1)) {
				goalcell = currentcell;
				reachable = true;
				break;
			} else {

				//North neighboor
				if (reachables[currentcell[0] - 1][currentcell[1]] > 0) {
					neighboorcells.add(new int[]{currentcell[0] - 1, currentcell[1]});
				}

				//South neighboor
				if (reachables[currentcell[0] + 1][currentcell[1]] > 0) {
					neighboorcells.add(new int[]{currentcell[0] + 1, currentcell[1]});
				}

				//West neighboor
				if (reachables[currentcell[0]][currentcell[1] - 1] > 0) {
					neighboorcells.add(new int[]{currentcell[0], currentcell[1] - 1});
				}

				//East neighboor
				if (reachables[currentcell[0]][currentcell[1] + 1] > 0) {
					neighboorcells.add(new int[]{currentcell[0], currentcell[1] + 1});
				}

			}

			count += 1;

		}

		return goalcell;

	}


	private static long startTime = System.nanoTime();

	private static void printSearchStatus(HashSet<State> explored, Frontier frontier) {
		String statusTemplate = "#Expanded: %,8d, #Frontier: %,8d, #Generated: %,8d, Time: %3.3f s\n%s\n";
		double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000d;
		System.err.format(statusTemplate, explored.size(), frontier.size(), explored.size() + frontier.size(),
				elapsedTime, Memory.stringRep());
	}
}
