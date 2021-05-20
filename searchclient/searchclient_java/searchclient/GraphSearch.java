package searchclient;


import java.util.*;
import java.util.stream.Collectors;

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
			//Part 2:
			//Now try to implement the Graph-Search algorithm from R&N figure 3.7
			//In the case of "failure to find a solution" you should return null.
			//Some useful methods on the state class which you will need to use are:
			//state.isGoalState() - Returns true if the state is a goal state.
			//state.extractPlan() - Returns the Array of actions used to reach this state.
			//state.getExpandedStates() - Returns an ArrayList<State> containing the states reachable from the current state.
			//You should also take a look at Frontier.java to see which methods the Frontier interface exposes
			//
			//printSearchStates(explored, frontier): As you can see below, the code will print out status
			//(#explored states, size of the frontier, #generated states, total time used) for every 10000th node generated.
			//You might also find it helpful to print out these stats when a solution has been found, so you can keep
			//track of the exact total number of states generated.


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

			LinkedList<char[][]> NB_and_R_subgoals = new LinkedList<char[][]>();
			LinkedList<Integer> NB_and_R_subgoals_agent = new LinkedList<Integer>();

			LinkedList<char[][]> NB_subgoals = new LinkedList<char[][]>();
			LinkedList<Integer> NB_subgoals_agent = new LinkedList<Integer>();

			LinkedList<char[][]> B_subgoals = new LinkedList<char[][]>();
			LinkedList<Integer> B_subgoals_agent = new LinkedList<Integer>();

			for (int agent = 0; agent < MAsubgoals.length; agent++) {
				for (char[][] subgoal : MAsubgoals[agent]) {
					if (nonBlocking(initialState, subgoal)) {
						State reachability_test_state = copyState(initialState);
						reachability_test_state.goals = subgoal;
						Coordinates reachable = reachability_test_state.conflictRecognition(agent);
						if (reachable.x < 0) {
							NB_and_R_subgoals.add(subgoal);
							NB_and_R_subgoals_agent.add(agent);
						} else {
							NB_subgoals.add(subgoal);
							NB_subgoals_agent.add(agent);
						}
					} else {
						B_subgoals.add(subgoal);
						B_subgoals_agent.add(agent);
					}
				}
			}

			System.err.println("SUBGOALS ARE PRIORITIZED");
			System.err.println();

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

			// TODO: mark agents as initially blocked by other agents
			boolean[] blockedAgents = new boolean[agents];
			for (int agent = 0; agent < agents; agent++) {
				List<Character> blocksByAgent = AgentWiggleSearch.checkAgentBlocks(originalState, agent);
				for (char blockedAgent : blocksByAgent) {
					blockedAgents[blockedAgent - '0'] = true;
				}
			}

			for (int agent = 0; agent < agents; agent++) {
				if (blockedAgents[agent]) {
					System.err.println("Following agent blocked, starting wiggle search: " + agent);
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

				while (!NB_and_R_subgoals.isEmpty()) {


					s.agentColors = initcoloragent;
					s.boxColors = initcolorbox;

					char[][] currentsubgoal = NB_and_R_subgoals.pollFirst();
					int agent = NB_and_R_subgoals_agent.pollFirst();
					searchclient.Color currentColor = initialState.agentColors[agent];

//            		char currentsubgoal_char = 0;
//            		for (int i=0; i<currentsubgoal.length; i++) {
//            			for (int j=0; j<currentsubgoal[i].length; j++) {
//            				if (currentsubgoal[i][j] != 0) {
//            					currentsubgoal_char = currentsubgoal[i][j];
//            				}
//            			}
//            		}
//
//            		boolean subgoal_exists = false;
//            		for (int i=0; i<s.goals.length; i++) {
//            			for (int j=0; j<s.goals[i].length; j++) {
//            				if (s.goals[i][j] == currentsubgoal_char) {
//            					subgoal_exists = true;
//            				}
//            			}
//            		}
//
//            		if (!subgoal_exists) {
//            			break;
//            		}


					char[][] unreachedSubgoals = new char[s.goals.length][s.goals[0].length];

					for (int i = 0; i < currentsubgoal.length; i++) {
						for (int j = 0; j < currentsubgoal[i].length; j++) {
							if (currentsubgoal[i][j] == 0) {
								unreachedSubgoals[i][j] = s.goals[i][j];
							}
						}
					}

					System.err.println("ALL GOALS IN S: ");
					for (int i = 0; i < s.goals.length; i++) {
						System.err.println(Arrays.toString(s.goals[i]));
					}

					System.err.println("SUBGOAL IS POPPED");
					s.goals = currentsubgoal;

					for (int i = 0; i < s.goals.length; i++) {
						System.err.println(Arrays.toString(s.goals[i]));
					}

					System.err.println();


					LinkedList<Integer> same_color_agents = new LinkedList<Integer>();
					for (int i = 0; i < agents; i++) {
						if (currentColor == s.agentColors[i]) {
							same_color_agents.add(i);
						}
					}


					LinkedList<char[][]> subgoal_split = new LinkedList<char[][]>();

					System.err.println("CURRENT LEVEL: ");
					char[][] currentlevel = new char[s.boxes.length][s.boxes[0].length];

					for (char[] chars : currentlevel) {
						Arrays.fill(chars, ' ');
					}


					for (int i = 0; i < s.agentRows.length; i++) {
						char b = (char) (i + '0');
						currentlevel[s.agentRows[i]][s.agentCols[i]] = b;
					}

					for (int i = 0; i < s.walls.length; i++) {
						for (int j = 0; j < s.walls[i].length; j++) {
							if (s.walls[i][j]) {
								currentlevel[i][j] = 'W';
							}
						}
					}

					for (int i = 0; i < currentlevel.length; i++) {
						System.err.println(Arrays.toString(currentlevel[i]));
					}

					boolean agent_found = false;
					find_solvable_agent:
					for (int agentNr : same_color_agents) {

						System.err.println("CHECKING AGENT: " + agentNr);
						Coordinates reachable_agent = s.conflictRecognition(agentNr);

						if (reachable_agent.x < 0) {
							agent_found = true;
							System.err.println("AGENT TO SOLVE SUBGOAL IS FOUND: " + agentNr);
							System.err.println();

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

								System.err.println("SUBSUBGOAL");
								for (int i = 0; i < subsubgoal.length; i++) {
									System.err.println(Arrays.toString(subsubgoal[i]));
								}
								System.err.println("AGENTROW: " + Arrays.toString(agent_s.agentRows));
								System.err.println("AGENTCOL: " + Arrays.toString(agent_s.agentCols));
								System.err.println("AGENTCOLORS: " + Arrays.toString(agent_s.agentColors));
								System.err.println("BOXCOLORS: " + Arrays.toString(agent_s.boxColors));
								System.err.println("BOXES");
								for (int i = 0; i < agent_s.boxes.length; i++) {
									System.err.println(Arrays.toString(agent_s.boxes[i]));
								}
								System.err.println("DISTANCEGRID");
								for (int i = 0; i < agent_s.distancegrid.length; i++) {
									System.err.println(Arrays.toString(agent_s.distancegrid[i]));
								}
								System.err.println();


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
										System.err.println("SUBGOAL FOUND");
										for (int i = 0; i < agent_s.goals.length; i++) {
											System.err.println(Arrays.toString(agent_s.goals[i]));
										}

										//Update State
										s.agentRows[agent] = agent_s.getSingleAgentRow(0)[0];
										s.agentCols[agent] = agent_s.getSingleAgentCol(0)[0];

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
										all_plans[agent].addLast(subactions);
										subgoal_actions_order.addLast(agent);

										while (!frontier.isEmpty()) {
											frontier.pop();
										}

										System.err.println();
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


				}


				s.agentColors = initcoloragent;
				s.boxColors = initcolorbox;

				System.err.println("STATE S");
				System.err.println("GOAL: ");
				for (int i = 0; i < s.goals.length; i++) {
					System.err.println(Arrays.toString(s.goals[i]));
				}
				System.err.println("AGENTROW: " + Arrays.toString(s.agentRows));
				System.err.println("AGENTCOL: " + Arrays.toString(s.agentCols));
				System.err.println("AGENTCOLORS: " + Arrays.toString(s.agentColors));
				System.err.println("BOXCOLORS: " + Arrays.toString(s.boxColors));
				System.err.println("BOXES");
				for (int i = 0; i < s.boxes.length; i++) {
					System.err.println(Arrays.toString(s.boxes[i]));
				}
				System.err.println("DISTANCEGRID");
				for (int i = 0; i < s.distancegrid.length; i++) {
					System.err.println(Arrays.toString(s.distancegrid[i]));
				}
				System.err.println();

				subgoals = new LinkedList<char[][]>();
				MAsubgoals = new LinkedList[s.agentRows.length];


				for (int agent = 0; agent < s.agentRows.length; agent++) {
					subgoals = s.getAgentSubGoals(agent);
					MAsubgoals[agent] = subgoals;
				}

				NB_and_R_subgoals = new LinkedList<char[][]>();
				NB_and_R_subgoals_agent = new LinkedList<Integer>();

				NB_subgoals = new LinkedList<char[][]>();
				NB_subgoals_agent = new LinkedList<Integer>();

				B_subgoals = new LinkedList<char[][]>();
				B_subgoals_agent = new LinkedList<Integer>();

				for (int agent = 0; agent < MAsubgoals.length; agent++) {
					for (char[][] subgoal : MAsubgoals[agent]) {
						if (nonBlocking(s, subgoal)) {
							State reachability_test_state = copyState(s);
							reachability_test_state.goals = subgoal;
							Coordinates reachable = reachability_test_state.conflictRecognition(agent);
							if (reachable.x < 0) {
								NB_and_R_subgoals.add(subgoal);
								NB_and_R_subgoals_agent.add(agent);
							} else {
								NB_subgoals.add(subgoal);
								NB_subgoals_agent.add(agent);
							}
						} else {
							B_subgoals.add(subgoal);
							B_subgoals_agent.add(agent);
						}
					}
				}

			}

			Action[][] finalactions = new Action[sequence_solution_length][agents];
			for (Action[] row : finalactions) {
				Arrays.fill(row, Action.NoOp);
			}

            /*int index = 0;
            for (int turn : subgoal_actions_order) {
            	Action[][] currentAction = all_plans[turn].poll();
            	int action_length = currentAction.length;
            	for (int i=0; i<action_length; i++) {
            		finalactions[index][turn] = currentAction[i][0];
            		index += 1;
            	}
            }*/

			/*for (int i = 0; i < all_plans.length; i++) {
				planner.addPlan(i, all_plans[i]);
				PlanningResult planningResult = planner.plan(initialState, 0);
				if (planningResult.type != PlanningResult.PlanningResultType.NO_CONFLICT) {
					System.err.println("Could not find a solution.");
				}
			}
			 */

			System.err.println("Plan before resolution.");
			for (LinkedList<Action[][]> agent : all_plans) {
				LinkedList<Action[]> resolvedActions = new LinkedList<>();
				for (Action[][] steps : agent) {
					resolvedActions.addAll(Arrays.asList(steps));
				}
				for (Action[] step : resolvedActions) {
					System.err.println(Arrays.toString(step));
				}
			}
			System.err.println("Subgoal order: " + subgoal_actions_order.toString());
			for (int i = 0; i < subgoal_actions_order.size(); i += 2) {
				int turn = subgoal_actions_order.get(i);
				planner.addSubplan(all_plans[turn].poll(), turn);
				planner.addSubplan(all_plans[turn].poll(), turn);
				try {
					planner.plan(originalState, 0);
				} catch (StackOverflowError ignored) {
				}
			}

			// If we fail to find a solution, then at least go sequential - will suck, but at least
			// it may work

			if (planner.delve(originalState, 0).type == PlanningResult.PlanningResultType.WITH_CONFLICT) {
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
    	for (int i=0; i<subgoal.length; i++) {
    		for (int j=0; j<subgoal[i].length; j++) {
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
     	wallneighboors[0] = test_state.walls[goal_row-1][goal_col];
     	wallneighboors[1] = test_state.walls[goal_row+1][goal_col];
     	wallneighboors[2] = test_state.walls[goal_row][goal_col-1];
     	wallneighboors[3] = test_state.walls[goal_row][goal_col+1];

     	LinkedList<int[]> goal_neighboors = new LinkedList<int[]>();
     	for (int i=0; i<wallneighboors.length; i++) {
     		if(!wallneighboors[i]) {
     			if (i == 0) {
     				goal_neighboors.add(new int[] {goal_row-1, goal_col});
     			} else if (i==1) {
     				goal_neighboors.add(new int[] {goal_row+1, goal_col});
     			} else if (i==2) {
     				goal_neighboors.add(new int[] {goal_row, goal_col-1});
     			} else if (i==3) {
     				goal_neighboors.add(new int[] {goal_row, goal_col+1});
     			}
     		}
     	}

     	int[] dst = goal_neighboors.poll();
     	int[][] region = test_state.getdistance(dst[0], dst[1]);
     	for (int i=0; i<region.length; i++) {
     		for(int j=0; j<region[i].length; j++) {
     			if (region[i][j] != 0) {
     				region[i][j] = 1;
     			}
     		}
     	}
     	System.err.println(Arrays.toString(dst));

     	int currRegion = 2;
     	for (int i=0; i<goal_neighboors.size(); i++) {
     		dst = goal_neighboors.get(i);
     		if (region[dst[0]][dst[1]] == 0) {
     			System.err.println(Arrays.toString(dst));
     			int[][] region_temp = test_state.getdistance(dst[0],dst[1]);
     			for (int j=0; j<region_temp.length; j++) {
             		for(int k=0; k<region_temp[j].length; k++) {
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
     	for (int i=0; i<agent_chars.length; i++) {
     		agent_chars[i] = new LinkedList<Character>();
     	}

     	LinkedList<Character>[] box_chars = new LinkedList[currRegion];
     	for (int i=0; i<box_chars.length; i++) {
     		box_chars[i] = new LinkedList<Character>();
     	}

     	LinkedList<Character>[] goal_chars = new LinkedList[currRegion];
     	for (int i=0; i<goal_chars.length; i++) {
     		goal_chars[i] = new LinkedList<Character>();
     	}

     	//Adding boxes and goals in region
     	int thisregion;
     	for (int i=0; i<region.length; i++) {
     		for (int j=0; j<region[i].length; j++) {
     			thisregion = region[i][j];
     			if (thisregion != 0) {

     				if (test_state.boxes[i][j] != 0) {
     					box_chars[thisregion-1].add(test_state.boxes[i][j]);
     				} else if (test_state.goals[i][j] != 0) {
     					goal_chars[thisregion-1].add(test_state.goals[i][j]);
     				}

     			}
     		}
     	}

     	//Adding agents in region
     	for (int i=0; i<test_state.agentRows.length; i++) {
     		int agent_row = test_state.agentRows[i];
     		int agent_col = test_state.agentCols[i];

     		int agentregion = region[agent_row][agent_col];

     		if (agentregion != 0) {
     			char agent_char = (char)(i+'0');
     			agent_chars[agentregion-1].add(agent_char);
     		}

     	}


     	for (int i=0; i<agent_chars.length; i++) {

     		for (char goal : goal_chars[i]) {
     				// Box check
     				boolean boxFound = false;
     				Color boxColor = null;
     				for (int j=0; j<box_chars[i].size(); j++) {
     					if (box_chars[i].get(j).equals(goal)) {
     						boxColor = test_state.boxColors[box_chars[i].get(j) - 'A'];
     						box_chars[i].remove(j);
     						boxFound = true;
     						break;
     					}
     				}
     				if(!boxFound) {
     					return false;
     				}

     				// Agent check
     				boolean agentFound = false;
     				for(int j = 0; j < agent_chars[i].size(); j++) {
     					if(test_state.agentColors[agent_chars[i].get(j) - '0'].equals(boxColor)) {
     						agentFound = true;
     						break;
     					}
     				}
     				if(!agentFound) {
     					return false;
     				}
     		}
     	}

     	return true;

    }


	private static long startTime = System.nanoTime();

	private static void printSearchStatus(HashSet<State> explored, Frontier frontier) {
		String statusTemplate = "#Expanded: %,8d, #Frontier: %,8d, #Generated: %,8d, Time: %3.3f s\n%s\n";
		double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000d;
		System.err.format(statusTemplate, explored.size(), frontier.size(), explored.size() + frontier.size(),
				elapsedTime, Memory.stringRep());
	}
}
