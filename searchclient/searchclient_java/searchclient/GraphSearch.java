package searchclient;



import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static searchclient.CentralPlanner.copyState;
//import java.awt.Desktop.Action;
//import java.awt.Desktop.Action;
import java.util.*;

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
            
            LinkedList<char[][]> subgoals = new LinkedList<char[][]>();
            LinkedList<char[][]>[] MAsubgoals = new LinkedList[initialState.agentRows.length];

            for (int agent=0; agent<initialState.agentRows.length; agent++) {
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
			State s = new State(initialState.agentRows, initialState.agentCols, initialState.agentColors,
					initialState.walls, initialState.boxes, initialState.boxColors, initialState.goals, initialState.distancegrid);

			//State from an agent's perspective
			State agent_s;

			searchclient.Color[] initcoloragent = s.agentColors;
			searchclient.Color[] initcolorbox = s.boxColors;


			CentralPlanner planner = new CentralPlanner(s);


			//Integers to store
			int agents = s.agentRows.length;
            int subgoal_total = 0;
            for (int i=0; i<MAsubgoals.length; i++) {
            	subgoal_total += MAsubgoals[i].size();
            }
            int subgoal_count = 0;
            int sequence_solution_length = 0;
            int unreachable_count = 0;

            outerwhile:
            while (subgoal_count < subgoal_total) {

            for (int agent=0; agent<agents; agent++) {
            	s.agentColors = initcoloragent;
            	s.boxColors = initcolorbox;


            	int[] aRows = s.getSingleAgentRow(agent);
            	int[] aCols = s.getSingleAgentCol(agent);
            	char[][] aBoxes = s.getSingleAgentBoxes(agent);
            	boolean[][] aWalls = s.otherEntities(agent);


            	searchclient.Color currentColor = initialState.agentColors[agent];
            	searchclient.Color[] agentColor = new searchclient.Color[initialState.agentColors.length];
            	searchclient.Color[] boxColor = new searchclient.Color[initialState.boxColors.length];

            	agentColor[0] = currentColor;
            	for (int i=0; i<boxColor.length; i++) {
            		if (initialState.boxColors[i] == currentColor) {
            			boxColor[i] = currentColor;
            		}
            	}
LinkedList<Integer> same_color_agents = new LinkedList<Integer>();
            	for (int i=0; i<agents; i++) {
            		if (i != agent && currentColor == s.agentColors[i]) {
            			same_color_agents.add(i);
            		}
            	}


            	int agentsubgoal_count = 0;

            	int subgoalsLeft = 0;
            	for (int i=0; i<MAsubgoals.length; i++) {
            		subgoalsLeft += MAsubgoals[i].size();
            	}

            	System.err.println("SUBGOALS LEFT: " + subgoalsLeft);
            	if (subgoalsLeft == 0) {
            		break outerwhile;
            	}
            	outersubgoalloop:
            	for (char[][] subgoal : MAsubgoals[agent]) {
            		LinkedList<char[][]> subgoal_split = new LinkedList<char[][]>();
//            		State test_s = new State(s.agentRows, s.agentCols, initcoloragent,
//                    		s.walls, s.boxes, initcolorbox, subgoal, s.distancegrid);

            		System.err.println("AGENT: " + agent);
            		System.err.println("SUBGOAL: ");
            		for (int i=0; i<subgoal.length; i++) {
            			System.err.println(Arrays.toString(subgoal[i]));
            		}
//            		System.err.println("BOXCOLORS: " + Arrays.toString(test_s.boxColors));
//            		System.err.println("AGENTCOLORS: " + Arrays.toString(test_s.agentColors));

//                 	Coordinates coor = test_s.conflictRecognition(agent);
//            		System.err.println("CONFICT RECOG: " + coor.x + ":" + coor.y);

                 	agent_s = new State(aRows, aCols, agentColor,
                 		aWalls, aBoxes, boxColor, subgoal, s.distancegrid);
                 	//Check if subgoal is reachable
                 	char goalchar = 0;
                 	int[] dst1 = new int[] {1,1};
                 	outer01:
                 	for (int i=1; i<subgoal.length-1; i++) {
                 		for (int j=1; j<subgoal[i].length-1; j++) {
                 			goalchar = subgoal[i][j];
                 			if ('A' <= goalchar && goalchar <= 'Z') {
                 				dst1 = new int[] {i,j};
                 				break outer01;
                 			}
                 		}
                 	}

                 	int[] dst2 = new int[] {aRows[0], aCols[0]};

                 	//Get source coordinate
                 	char boxchar = 0;
                 	int[] src = new int[] {1,1};
                 	outer02:
                 	for (int i=1; i<aBoxes.length-1; i++) {
                 		for (int j=1; j<aBoxes[i].length-1; j++) {
                 			boxchar = aBoxes[i][j];
							if (boxchar == goalchar) {
								src = new int[]{i, j};
								break outer02;
							}
						}
					}

					System.err.println("CURRENT LEVEL: ");
					char[][] currentlevel = new char[s.boxes.length][s.boxes[0].length];

					for (char[] chars : currentlevel) {
						Arrays.fill(chars, ' ');
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

                 	for (int i=0; i<s.walls.length; i++) {
                 		for (int j=0; j<s.walls[i].length; j++) {
                 			if (s.walls[i][j]) {
                 				currentlevel[i][j] = 'W';
                 			}
                 		}
             		}

                 	for (int i=0; i<currentlevel.length; i++) {
             			System.err.println(Arrays.toString(currentlevel[i]));
             		}

boolean reachable = false;

//                 	if (coor.x == -1) {
//                 		reachable = true;
//                 	}
                 	
                 	//Check if dst1 and dst2 can be reached from source
                 	int[][] reachables = agent_s.getdistance(src[0], src[1]);
                 	if (reachables[dst1[0]][dst1[1]] != 0) {
                 		if (reachables[dst2[0]][dst2[1]] != 0) {
                 			reachable = true;
                 		}
                 	}

                 	System.err.println("REACHABLE?: " + reachable);


//                 	//Check if subgoal blocks other subgoal
//                 	boolean[][] temp_walls = new boolean[agent_s.walls.length][agent_s.walls[0].length];
//                 	for (int i=0; i<agent_s.walls.length; i++) {
//                 		for (int j=0; j<agent_s.walls[i].length; j++) {
//                 			temp_walls[i][j] = agent_s.walls[i][j];
//                 		}
//                 	}
//                 	temp_walls[dst1[0]][dst1[1]] = true;
//
//                 	State agent_s_test = new State(aRows, aCols, agentColor,
//                     		temp_walls, aBoxes, boxColor, subgoal, agent_s.distancegrid);
//
//                 	int[][] test_reachables = agent_s_test.getdistance(src[0], src[1]);
//
//                 	int count_after = 0;
//                 	for (int i=0; i<test_reachables.length; i++) {
//                 		for (int j=0; j<test_reachables[i].length; j++) {
//                 			if (test_reachables[i][j] > 0) {
//                 				count_after += 1;
//                 			}
//                 		}
//                 	}
//
//                 	int count_before = 0;
//                 	for (int i=0; i<reachables.length; i++) {
//                 		for (int j=0; j<reachables[i].length; j++) {
//                 			if (reachables[i][j] > 0) {
//                 				count_before += 1;
//                 			}
//                 		}
//                 	}
//
//                 	if (count_before - 1 > count_after) {
//
//
//
//
//                 		reachable = false;
//                 	}

                 	if (!reachable) {
                 		System.err.println("NOT REACHABLE");
                 		unreachable_count += 1;
                 		if (unreachable_count > 10) {
                 			break outerwhile;
                 		}
                 	} else {

                 	unreachable_count = 0;
                 	MAsubgoals[agent].remove(agentsubgoal_count);

                 	for (int same_col_agent : same_color_agents) {
                 		MAsubgoals[same_col_agent].remove(agentsubgoal_count);
                 	}

                 	agentsubgoal_count += 1;
                 	subgoal_count += 1;
                 	subgoal_split = agent_s.splitSubgoal(agent_s.goals, agent);

                 	for (char[][] subgoalS : subgoal_split) {

                 		int[] goal_coord = new int[] {0, 0};

                 		char subgoal_char = 0;
                 		outer1:
                 		for (int i=0; i<subgoalS.length; i++) {
                 			for (int j=0; j<subgoalS[i].length; j++) {
                 				if (subgoalS[i][j] != 0) {
                 					subgoal_char = subgoalS[i][j];
                 					goal_coord = new int[] {i, j};
                 					break outer1;
                 				}
                 			}
                 		}

                 		int[][] dist_grid = agent_s.getdistance(goal_coord[0], goal_coord[1]);

                 		agent_s = new State(aRows, aCols, agentColor,
                         		aWalls, aBoxes, boxColor, subgoalS, dist_grid);

	                 	if (greedy) {
	                 		frontier = new FrontierBestFirst(new HeuristicGreedy(agent_s));
	                 	}

	                 	else if (astar) {
	                 		frontier = new FrontierBestFirst(new HeuristicAStar(agent_s));
	                 	}

	                 	else if (wastar) {
	                 		frontier = new FrontierBestFirst(new HeuristicWeightedAStar(agent_s, 0));
	                 	}


	                 	frontier.add(agent_s);
	                    HashSet<State> explored = new HashSet<>();

	                     while (true) {

	                         //Print a status message every 10000 iteration
	                         if (++iterations % 10000 == 0) {
	                             printSearchStatus(explored, frontier);
	                         }

	                         if(frontier.isEmpty()) {
	                             printSearchStatus(explored, frontier);
	                             return null;
	                         }


	                         agent_s = frontier.pop();
	                         if(agent_s.isGoalState()){
	                        	System.err.println("SUBGOAL FOUND");
for (int i=0; i<agent_s.goals.length; i++) {
	                        		System.err.println(Arrays.toString(agent_s.goals[i]));
	                        	}
	                          	//Update State
	                          	s.agentRows[agent] = agent_s.getSingleAgentRow(0)[0];
	                          	s.agentCols[agent] = agent_s.getSingleAgentCol(0)[0];

	                          	//Update boxes
	                          	aBoxes = agent_s.getSingleAgentBoxes(0);
	                          	for (int i=0; i<aBoxes.length; i++) {
	                          		for (int j=0; j<aBoxes[i].length; j++) {

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
	                             for (int row=1; row<agent_s.goals.length-1; row++) {
	                             	for (int col=1; col<agent_s.goals[row].length-1; col++) {
	                             		char goal = agent_s.goals[row][col];
	                             		if ('A' <= goal && goal <= 'Z') {
	                             			s.boxes[row][col] = 0;
	                             			s.walls[row][col] = true;
	                             		}
	                             	}
	                             }

	                            aRows = agent_s.getSingleAgentRow(0);
	                         	aCols = agent_s.getSingleAgentCol(0);
	                         	aBoxes = agent_s.getSingleAgentBoxes(0);
	                         	aWalls = agent_s.otherEntities(0);

	                             Action[][] subactions =  agent_s.extractPlan();
	                             sequence_solution_length += subactions.length;
	                             all_plans[agent].addLast(subactions);
	                             subgoal_actions_order.addLast(agent);

	                         	 while (!frontier.isEmpty()) {
	                         		 frontier.pop();
	                         	 }

	                         	System.err.println();
	                         	if ('A' <= subgoal_char && subgoal_char <= 'Z') {
	                         		break outersubgoalloop;
	                         	}
	                            break;
	                         }



	                         explored.add(agent_s);



	                         ArrayList<State> States = agent_s.getExpandedStates();
	                         for(int i = 0; i < States.size(); ++i){
	                             State expanded = States.get(i);
	                             if(!explored.contains(expanded) && !frontier.contains(expanded)) {
	                                 frontier.add(expanded);
	                             }
	                         }
	                     }
                     }
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

			planner.addAllPlans(all_plans);
			planner.plan(originalState, 0);
			return planner.getFullPlan();
		}
    }


	private static long startTime = System.nanoTime();

	private static void printSearchStatus(HashSet<State> explored, Frontier frontier) {
		String statusTemplate = "#Expanded: %,8d, #Frontier: %,8d, #Generated: %,8d, Time: %3.3f s\n%s\n";
		double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000d;
		System.err.format(statusTemplate, explored.size(), frontier.size(), explored.size() + frontier.size(),
				elapsedTime, Memory.stringRep());
	}
}
