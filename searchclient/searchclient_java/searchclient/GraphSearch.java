package searchclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
//import java.awt.Desktop.Action;
//import java.awt.Desktop.Action;
import java.util.*;

public class GraphSearch {

    public static Action[][] search(State initialState, Frontier frontier)
    {
        boolean outputFixedSolution = false;

        if (outputFixedSolution) {
            //Part 1:
            //The agents will perform the sequence of actions returned by this method.
            //Try to solve a few levels by hand, enter the found solutions below, and run them:

            return new Action[][] {
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
            
            LinkedList<char[][]> NB_and_R_subgoals = new LinkedList<char[][]>();
            LinkedList<Integer> NB_and_R_subgoals_agent = new LinkedList<Integer>();
           
            LinkedList<char[][]> NB_subgoals = new LinkedList<char[][]>();
            LinkedList<Integer> NB_subgoals_agent = new LinkedList<Integer>();
            
            LinkedList<char[][]> B_subgoals = new LinkedList<char[][]>();
            LinkedList<Integer> B_subgoals_agent = new LinkedList<Integer>();
            
            for (int agent=0; agent<MAsubgoals.length; agent++) {
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
            for (int i=0; i<all_plans.length; i++) {
            	all_plans[i] = new LinkedList<Action[][]>();
            }
            
            //Maintaining State
            State s = copyState(initialState);
            
            //State from an agent's perspective
            State agent_s = copyState(s);
            
            searchclient.Color[] initcoloragent =  s.agentColors;
            searchclient.Color[] initcolorbox =  s.boxColors;
            
            
            CentralPlanner planner = new CentralPlanner(s);
            
            
            //Integers to store
            int agents = s.agentRows.length;
            
            int subgoal_total = 0;
            for (int i=0; i<s.goals.length; i++) {
            	for (int j=0; j<s.goals[i].length; j++) {
            		if (s.goals[i][j] != 0) {
            			subgoal_total += 1;
            		}
            	}
            }
            
            int subgoal_count = 0;
            int sequence_solution_length = 0;
            int unreachable_count = 0;
            
            outerwhile:
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
            		
            		for (int i=0; i<currentsubgoal.length; i++) {
            			for (int j=0; j<currentsubgoal[i].length; j++) {
            				if (currentsubgoal[i][j] == 0) {
            					unreachedSubgoals[i][j] = s.goals[i][j];
            				}
            			}
            		}
            		
            		System.err.println("ALL GOALS IN S: ");
            		for (int i=0; i<s.goals.length; i++) {
                		System.err.println(Arrays.toString(s.goals[i]));
                	}
            		
            		System.err.println("SUBGOAL IS POPPED");
            		
            		s.goals = currentsubgoal;
            		
            		for (int i=0; i<s.goals.length; i++) {
                		System.err.println(Arrays.toString(s.goals[i]));
                	}
            		
            		System.err.println();
            		
            		
                	
                	LinkedList<Integer> same_color_agents = new LinkedList<Integer>();
                	for (int i=0; i<agents; i++) {
                		if (currentColor == s.agentColors[i]) {
                			same_color_agents.add(i);
                		}
                	}
                	
                	LinkedList<char[][]> subgoal_split = new LinkedList<char[][]>();
                	
                	System.err.println("CURRENT LEVEL: ");
                 	char[][] currentlevel = new char[s.boxes.length][s.boxes[0].length];
                 	
                 	for (int i=0; i<currentlevel.length; i++) {
                 		for (int j=0; j<currentlevel[i].length; j++) {
                 			currentlevel[i][j] = ' ';
                 		}
                 	}
                 	
                 	
                 	for (int i=0; i<s.agentRows.length; i++) {
                 		char b = (char)(i+'0');
                 		currentlevel[s.agentRows[i]][s.agentCols[i]] = b;
                 	}
                 	
                 	for (int i=0; i<s.boxes.length; i++) {
                 		for (int j=0; j<s.boxes[i].length; j++) {
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
                        	for (int i=0; i<boxColor.length; i++) {
                        		if (initialState.boxColors[i] == currentColor) {
                        			boxColor[i] = currentColor;
                        		}
                        	}
                        	
                        	agent_s = new State(aRows, aCols, agentColor,
                             		aWalls, aBoxes, boxColor, currentsubgoal, s.distancegrid);
                        	
                        	
                        	subgoal_split = agent_s.splitSubgoal(agent_s.goals, agentNr);
                        	
                        	for (char[][] subsubgoal : subgoal_split) {
                        		
                        		int[] goal_coord = new int[] {0, 0};
                         		
                         		char subgoal_char = 0;
                         		outer1:
                         		for (int i=0; i<subsubgoal.length; i++) {
                         			for (int j=0; j<subsubgoal[i].length; j++) {
                         				if (subsubgoal[i][j] != 0) {
                         					subgoal_char = subsubgoal[i][j];
                         					goal_coord = new int[] {i, j};
                         					break outer1;
                         				}
                         			}
                         		}
                         		
                         		int[][] dist_grid = agent_s.getdistance(goal_coord[0], goal_coord[1]);
                         		
                         		
                         		agent_s = new State(aRows, aCols, agentColor,
                                 		aWalls, aBoxes, boxColor, subsubgoal, dist_grid);
                         		
                         		System.err.println("SUBSUBGOAL");
                        		for (int i=0; i<subsubgoal.length; i++) {
                            		System.err.println(Arrays.toString(subsubgoal[i]));
                            	}
                         		System.err.println("AGENTROW: " + Arrays.toString(agent_s.agentRows));
                         		System.err.println("AGENTCOL: " + Arrays.toString(agent_s.agentCols));
                         		System.err.println("AGENTCOLORS: " + Arrays.toString(agent_s.agentColors));
                         		System.err.println("BOXCOLORS: " + Arrays.toString(agent_s.boxColors));
                         		System.err.println("BOXES");
                         		for (int i=0; i<agent_s.boxes.length; i++) {
                            		System.err.println(Arrays.toString(agent_s.boxes[i]));
                            	}
                         		System.err.println("DISTANCEGRID");
                         		for (int i=0; i<agent_s.distancegrid.length; i++) {
                            		System.err.println(Arrays.toString(agent_s.distancegrid[i]));
                            	}
                         		System.err.println();
                         		
                        		
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
        	                             
        	                             //Update all goals in S
        	                             s.goals = unreachedSubgoals;
        	                             
        	                             
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
        		for (int i=0; i<s.goals.length; i++) {
            		System.err.println(Arrays.toString(s.goals[i]));
            	}
         		System.err.println("AGENTROW: " + Arrays.toString(s.agentRows));
         		System.err.println("AGENTCOL: " + Arrays.toString(s.agentCols));
         		System.err.println("AGENTCOLORS: " + Arrays.toString(s.agentColors));
         		System.err.println("BOXCOLORS: " + Arrays.toString(s.boxColors));
         		System.err.println("BOXES");
         		for (int i=0; i<s.boxes.length; i++) {
            		System.err.println(Arrays.toString(s.boxes[i]));
            	}
         		System.err.println("DISTANCEGRID");
         		for (int i=0; i<s.distancegrid.length; i++) {
            		System.err.println(Arrays.toString(s.distancegrid[i]));
            	}
         		System.err.println();
            	
            	 subgoals = new LinkedList<char[][]>();
                 MAsubgoals = new LinkedList[s.agentRows.length];
                 
                 
                 for (int agent=0; agent<s.agentRows.length; agent++) {
                 	subgoals = s.getAgentSubGoals(agent);
                 	MAsubgoals[agent] = subgoals;
                 }
                 
                 NB_and_R_subgoals = new LinkedList<char[][]>();
                 NB_and_R_subgoals_agent = new LinkedList<Integer>();
                
                 NB_subgoals = new LinkedList<char[][]>();
                 NB_subgoals_agent = new LinkedList<Integer>();
                 
                 B_subgoals = new LinkedList<char[][]>();
                 B_subgoals_agent = new LinkedList<Integer>();
                 
                 for (int agent=0; agent<MAsubgoals.length; agent++) {
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
            
            int index = 0;
            for (int turn : subgoal_actions_order) {
            	Action[][] currentAction = all_plans[turn].poll();
            	int action_length = currentAction.length;
            	for (int i=0; i<action_length; i++) {
            		finalactions[index][turn] = currentAction[i][0];
            		index += 1;
            	}
            }
            
            
            return finalactions;
        }
    }
            	
            	
//            while (subgoal_count < subgoal_total) { 	
//            
//            for (int agent=0; agent<agents; agent++) {
//            
//            	s.agentColors = initcoloragent;
//            	s.boxColors = initcolorbox;
//            
//            	int[] aRows = s.getSingleAgentRow(agent);
//            	int[] aCols = s.getSingleAgentCol(agent);
//            	char[][] aBoxes = s.getSingleAgentBoxes(agent);
//            	boolean[][] aWalls = s.otherEntities(agent);
//            	
//            	
//            	searchclient.Color currentColor = initialState.agentColors[agent];
//            	searchclient.Color[] agentColor = new searchclient.Color[initialState.agentColors.length];
//            	searchclient.Color[] boxColor = new searchclient.Color[initialState.boxColors.length];
//            	
//            	agentColor[0] = currentColor;
//            	for (int i=0; i<boxColor.length; i++) {
//            		if (initialState.boxColors[i] == currentColor) {
//            			boxColor[i] = currentColor;
//            		}
//            	}
//            	
//            	
//            	LinkedList<Integer> same_color_agents = new LinkedList<Integer>();
//            	for (int i=0; i<agents; i++) {
//            		if (i != agent && currentColor == s.agentColors[i]) {
//            			same_color_agents.add(i);
//            		}
//            	}
//            	
//            	
//            	int agentsubgoal_count = 0;
//            	
//            	int subgoalsLeft = 0;
//            	for (int i=0; i<MAsubgoals.length; i++) {
//            		subgoalsLeft += MAsubgoals[i].size();
//            	}
//            	
//            	System.err.println("SUBGOALS LEFT: " + subgoalsLeft);
//            	if (subgoalsLeft == 0) {
//            		break outerwhile;
//            	}
//            	
//            	outersubgoalloop:
//            	for (char[][] subgoal : MAsubgoals[agent]) {
//            		LinkedList<char[][]> subgoal_split = new LinkedList<char[][]>();
//            		
//            		State test_s = new State(s.agentRows, s.agentCols, initcoloragent,
//                    		s.walls, s.boxes, initcolorbox, subgoal, s.distancegrid);
//            		
//            		System.err.println("AGENT: " + agent);
//            		System.err.println("SUBGOAL: ");
//            		for (int i=0; i<subgoal.length; i++) {
//            			System.err.println(Arrays.toString(subgoal[i]));
//            		}
////            		System.err.println("BOXCOLORS: " + Arrays.toString(test_s.boxColors));
////            		System.err.println("AGENTCOLORS: " + Arrays.toString(test_s.agentColors));
//            		
//                 	Coordinates coor = test_s.conflictRecognition(agent);
//            		System.err.println("CONFICT RECOG: " + coor.x + ":" + coor.y);
//            		
//            		
//            		agent_s = new State(aRows, aCols, agentColor,
//                     		aWalls, aBoxes, boxColor, subgoal, s.distancegrid);
//                 
//                 	//Check if subgoal is reachable
//                 	char goalchar = 0;
//                 	int[] dst1 = new int[] {1,1};
//                 	outer01:
//                 	for (int i=1; i<subgoal.length-1; i++) {
//                 		for (int j=1; j<subgoal[i].length-1; j++) {
//                 			goalchar = subgoal[i][j];
//                 			if ('A' <= goalchar && goalchar <= 'Z') {
//                 				dst1 = new int[] {i,j};
//                 				break outer01;
//                 			}
//                 		}
//                 	}
//                 	
//                 	boolean[][] temp_aWalls = new boolean[aWalls.length][aWalls[0].length];
//                 	for (int i=0; i<aWalls.length; i++) {
//                 		for (int j=0; j<aWalls[i].length; j++) {
//                 			temp_aWalls[i][j] = aWalls[i][j];
//                 		}
//                 	}
//                 	
//                 	temp_aWalls[dst1[0]][dst1[1]] = true;
//                 	
//                 	State test_agent_s = new State(aRows, aCols, agentColor,
//                     		temp_aWalls, aBoxes, boxColor, subgoal, s.distancegrid);
//                 	
//                 	
//                 	boolean[] wallneighboors = new boolean[4];
//                 	wallneighboors[0] = temp_aWalls[dst1[0]-1][dst1[1]];
//                 	wallneighboors[1] = temp_aWalls[dst1[0]+1][dst1[1]];
//                 	wallneighboors[2] = temp_aWalls[dst1[0]][dst1[1]-1];
//                 	wallneighboors[3] = temp_aWalls[dst1[0]][dst1[1]+1];
//                 	
//                 	LinkedList<int[]> goal_neighboors = new LinkedList<int[]>();
//                 	for (int i=0; i<wallneighboors.length; i++) {
//                 		if(!wallneighboors[i]) {
//                 			if (i == 0) {
//                 				goal_neighboors.add(new int[] {dst1[0]-1, dst1[1]});
//                 			} else if (i==1) {
//                 				goal_neighboors.add(new int[] {dst1[0]+1, dst1[1]});
//                 			} else if (i==2) {
//                 				goal_neighboors.add(new int[] {dst1[0], dst1[1]-1});
//                 			} else if (i==3) {
//                 				goal_neighboors.add(new int[] {dst1[0], dst1[1]+1});
//                 			}
//                 		}
//                 	}
//                 	
//                 	int[] dst = goal_neighboors.poll();
//                 	int[][] region = test_agent_s.getdistance(dst[0], dst[1]);
//                 	for (int i=0; i<region.length; i++) {
//                 		for(int j=0; j<region[i].length; j++) {
//                 			if (region[i][j] != 0) {
//                 				region[i][j] = 1;
//                 			}
//                 		}
//                 	}
//                 	System.err.println(Arrays.toString(dst));
//                 	
//                 	int currRegion = 2;
//                 	for (int i=0; i<goal_neighboors.size(); i++) {
//                 		dst = goal_neighboors.get(i);
//                 		if (region[dst[0]][dst[1]] == 0) {
//                 			System.err.println(Arrays.toString(dst));
//                 			int[][] region_temp = test_agent_s.getdistance(dst[0],dst[1]);
//                 			for (int j=0; j<region_temp.length; j++) {
//                         		for(int k=0; k<region_temp[j].length; k++) {
//                         			if (region_temp[j][k] != 0) {
//                         				region[j][k] = currRegion;
//                         			}
//                         		}
//                         	}
//                 		}
//                 		currRegion += 1;
//                 	}
//                 	
//                 	
//                 	
//                 	System.err.println("REGIONS: ");
//                 	for (int i=0; i<region.length; i++) {
//                 		System.err.println(Arrays.toString(region[i]));
//                 	}
//                 	
//                 	
////                 	
////                 	int[] dst2 = new int[] {aRows[0], aCols[0]};
////                 	
////                 	//Get source coordinate
////                 	char boxchar = 0;
////                 	int[] src = new int[] {1,1};
////                 	outer02:
////                 	for (int i=1; i<aBoxes.length-1; i++) {
////                 		for (int j=1; j<aBoxes[i].length-1; j++) {
////                 			boxchar = aBoxes[i][j];
////                 			if (boxchar == goalchar) {
////                 				src = new int[] {i,j};
////                 				break outer02;
////                 			}
////                 		}
////                 	}
//                 	
//                 	System.err.println("CURRENT LEVEL: ");
//                 	char[][] currentlevel = new char[s.boxes.length][s.boxes[0].length];
//                 	
//                 	for (int i=0; i<currentlevel.length; i++) {
//                 		for (int j=0; j<currentlevel[i].length; j++) {
//                 			currentlevel[i][j] = ' ';
//                 		}
//                 	}
//                 	
//                 	
//                 	for (int i=0; i<s.agentRows.length; i++) {
//                 		char b = (char)(i+'0');
//                 		currentlevel[s.agentRows[i]][s.agentCols[i]] = b;
//                 	}
//                 	
//                 	for (int i=0; i<s.boxes.length; i++) {
//                 		for (int j=0; j<s.boxes[i].length; j++) {
//                 			char box = s.boxes[i][j];
//                  			if ('A' <= box && box <= 'Z') {
//                  				currentlevel[i][j] = box;
//                  			}
//                 		}
//             		}
//                 	
//                 	for (int i=0; i<s.walls.length; i++) {
//                 		for (int j=0; j<s.walls[i].length; j++) {
//                 			if (s.walls[i][j]) {
//                 				currentlevel[i][j] = 'W';
//                 			}
//                 		}
//             		}
//                 	
//                 	for (int i=0; i<currentlevel.length; i++) {
//             			System.err.println(Arrays.toString(currentlevel[i]));
//             		}
//                 	
//                 	
//                 	boolean reachable = false;
//                 	
//                 	if (coor.x == -1) {
//                 		reachable = true;
//                 	}
//                 	
////                 	//Check if dst1 and dst2 can be reached from source
////                 	int[][] reachables = agent_s.getdistance(src[0], src[1]);
////                 	if (reachables[dst1[0]][dst1[1]] != 0) {
////                 		if (reachables[dst2[0]][dst2[1]] != 0) {
////                 			reachable = true;
////                 		}
////                 	}
////                 	
//                 	System.err.println("REACHABLE?: " + reachable);
//                 	
//                 	
////                 	//Check if subgoal blocks other subgoal
////                 	boolean[][] temp_walls = new boolean[agent_s.walls.length][agent_s.walls[0].length];
////                 	for (int i=0; i<agent_s.walls.length; i++) {
////                 		for (int j=0; j<agent_s.walls[i].length; j++) {
////                 			temp_walls[i][j] = agent_s.walls[i][j];
////                 		}
////                 	}
////                 	temp_walls[dst1[0]][dst1[1]] = true;
////                 	
////                 	State agent_s_test = new State(aRows, aCols, agentColor,
////                     		temp_walls, aBoxes, boxColor, subgoal, agent_s.distancegrid);
////                 	
////                 	int[][] test_reachables = agent_s_test.getdistance(src[0], src[1]);
////                 	
////                 	int count_after = 0;
////                 	for (int i=0; i<test_reachables.length; i++) {
////                 		for (int j=0; j<test_reachables[i].length; j++) {
////                 			if (test_reachables[i][j] > 0) {
////                 				count_after += 1;
////                 			}
////                 		}
////                 	}
////                 	
////                 	int count_before = 0;
////                 	for (int i=0; i<reachables.length; i++) {
////                 		for (int j=0; j<reachables[i].length; j++) {
////                 			if (reachables[i][j] > 0) {
////                 				count_before += 1;
////                 			}
////                 		}
////                 	}
////                 	
////                 	if (count_before - 1 > count_after) {
////                 		
////                 		
////                 		
////                 		
////                 		reachable = false;
////                 	}
//                 	
//                 	
//                 	if (!reachable) {
//                 		System.err.println("NOT REACHABLE");
//                 		unreachable_count += 1;
//                 		if (unreachable_count > 10) {
//                 			break outerwhile;
//                 		}
//                 	} else {
//                 		
//                 	unreachable_count = 0;
//                 	MAsubgoals[agent].remove(agentsubgoal_count);
//                 	
//                 	for (int same_col_agent : same_color_agents) {
//                 		MAsubgoals[same_col_agent].remove(agentsubgoal_count);
//                 	}
//                 	
//                 	agentsubgoal_count += 1;
//                 	subgoal_count += 1;	
//                 	subgoal_split = agent_s.splitSubgoal(agent_s.goals, agent);
//                 	
//                 	for (char[][] subgoalS : subgoal_split) {
//                 		
//                 		int[] goal_coord = new int[] {0, 0};
//                 		
//                 		char subgoal_char = 0;
//                 		outer1:
//                 		for (int i=0; i<subgoalS.length; i++) {
//                 			for (int j=0; j<subgoalS[i].length; j++) {
//                 				if (subgoalS[i][j] != 0) {
//                 					subgoal_char = subgoalS[i][j];
//                 					goal_coord = new int[] {i, j};
//                 					break outer1;
//                 				}
//                 			}
//                 		}
//                 		
//                 		int[][] dist_grid = agent_s.getdistance(goal_coord[0], goal_coord[1]);
//                 		
//                 		agent_s = new State(aRows, aCols, agentColor,
//                         		aWalls, aBoxes, boxColor, subgoalS, dist_grid);
//	                 	
//	                 	if (greedy) {
//	                 		frontier = new FrontierBestFirst(new HeuristicGreedy(agent_s));
//	                 	}
//	                 	
//	                 	else if (astar) {
//	                 		frontier = new FrontierBestFirst(new HeuristicAStar(agent_s));
//	                 	}
//	                 	
//	                 	else if (wastar) {
//	                 		frontier = new FrontierBestFirst(new HeuristicWeightedAStar(agent_s, 0));
//	                 	}
//	                 	
//	                 	
//	                 	frontier.add(agent_s);
//	                    HashSet<State> explored = new HashSet<>();
//	                 	
//	                     while (true) {
//	
//	                         //Print a status message every 10000 iteration
//	                         if (++iterations % 10000 == 0) {
//	                             printSearchStatus(explored, frontier);
//	                         }
//	
//	                         if(frontier.isEmpty()) {
//	                             printSearchStatus(explored, frontier);
//	                             return null;
//	                         }
//	                         
//	
//	                         agent_s = frontier.pop();
//	                         if(agent_s.isGoalState()){
//	                        	System.err.println("SUBGOAL FOUND");
//	                        	for (int i=0; i<agent_s.goals.length; i++) {
//	                        		System.err.println(Arrays.toString(agent_s.goals[i]));
//	                        	}
//	                        	 
//	                          	//Update State
//	                          	s.agentRows[agent] = agent_s.getSingleAgentRow(0)[0];
//	                          	s.agentCols[agent] = agent_s.getSingleAgentCol(0)[0];
//	                          	
//	                          	//Update boxes
//	                          	aBoxes = agent_s.getSingleAgentBoxes(0);
//	                          	for (int i=0; i<aBoxes.length; i++) {
//	                          		for (int j=0; j<aBoxes[i].length; j++) {
//	                          			
//	                          			char agent_box = aBoxes[i][j];
//	                          			char s_box = s.boxes[i][j];
//	                          			boolean agent_wall = agent_s.walls[i][j];
//	                          			
//	                          			
//	                          			
//	                          			if ('A' <= agent_box && agent_box <= 'Z') {
//	                          				if (agent_box != s_box) {
//	                          					s.boxes[i][j] = agent_box;
//	                          				}
//	                          			} else if ('A' <= s_box && s_box <= 'Z') {
//	                          				if (agent_box != s_box) {
//	                          					if (!agent_wall) {
//	                          						s.boxes[i][j] = agent_box;
//	                          					}
//	                          				}
//	                          			}
//	                          		}
//	                          	}
//	                          	
//	                          	//Turn achieved box-subgoal into a wall
//	                             for (int row=1; row<agent_s.goals.length-1; row++) {
//	                             	for (int col=1; col<agent_s.goals[row].length-1; col++) {
//	                             		char goal = agent_s.goals[row][col];
//	                             		if ('A' <= goal && goal <= 'Z') {
//	                             			s.boxes[row][col] = 0;
//	                             			s.walls[row][col] = true;
//	                             		}
//	                             	}
//	                             }
//	                             
//	                            aRows = agent_s.getSingleAgentRow(0);
//	                         	aCols = agent_s.getSingleAgentCol(0);
//	                         	aBoxes = agent_s.getSingleAgentBoxes(0);
//	                         	aWalls = agent_s.otherEntities(0);
//	                             
//	                             Action[][] subactions =  agent_s.extractPlan();
//	                             sequence_solution_length += subactions.length;
//	                             all_plans[agent].addLast(subactions);
//	                             subgoal_actions_order.addLast(agent);
//	                         	 
//	                         	 while (!frontier.isEmpty()) {
//	                         		 frontier.pop();
//	                         	 }
//	                            
//	                         	System.err.println();
//	                         	if ('A' <= subgoal_char && subgoal_char <= 'Z') {
//	                         		break outersubgoalloop;
//	                         	}
//	                            break;
//	                         }
//	                         
//	                         
//	                         
//	                         explored.add(agent_s);
//	
//	
//	
//	                         ArrayList<State> States = agent_s.getExpandedStates();
//	                         for(int i = 0; i < States.size(); ++i){
//	                             State expanded = States.get(i);
//	                             if(!explored.contains(expanded) && !frontier.contains(expanded)) {
//	                                 frontier.add(expanded);
//	                             }
//	                         }
//	                     }
//                     }
//                 	}
//            		}
//                 }
//            	 
//            }
//            
//            
//            
//            Action[][] finalactions = new Action[sequence_solution_length][agents];
//            for (Action[] row : finalactions) {
//            	Arrays.fill(row, Action.NoOp);
//            }
//            
//            int index = 0;
//            for (int turn : subgoal_actions_order) {
//            	Action[][] currentAction = all_plans[turn].poll();
//            	int action_length = currentAction.length;
//            	for (int i=0; i<action_length; i++) {
//            		finalactions[index][turn] = currentAction[i][0];
//            		index += 1;
//            	}
//            }
//            
//            
//            return finalactions;
//            
//        }
//    }
    
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

    private static void printSearchStatus(HashSet<State> explored, Frontier frontier)
    {
        String statusTemplate = "#Expanded: %,8d, #Frontier: %,8d, #Generated: %,8d, Time: %3.3f s\n%s\n";
        double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000d;
        System.err.format(statusTemplate, explored.size(), frontier.size(), explored.size() + frontier.size(),
                          elapsedTime, Memory.stringRep());
    }
}
