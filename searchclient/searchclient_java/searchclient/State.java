package searchclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.*;



public class State
{
    private static final Random RNG = new Random(1);

    /*
        The agent rows, columns, and colors are indexed by the agent number.
        For example, this.agentRows[0] is the row location of agent '0'.
    */
    public int[] agentRows;
    public int[] agentCols;
    public static Color[] agentColors;

    /*
        The walls, boxes, and goals arrays are indexed from the top-left of the level, row-major order (row, col).
               Col 0  Col 1  Col 2  Col 3
        Row 0: (0,0)  (0,1)  (0,2)  (0,3)  ...
        Row 1: (1,0)  (1,1)  (1,2)  (1,3)  ...
        Row 2: (2,0)  (2,1)  (2,2)  (2,3)  ...
        ...

        For example, this.walls[2] is an array of booleans for the third row.
        this.walls[row][col] is true if there's a wall at (row, col).
    */
    public boolean[][] walls;
    public char[][] boxes;
    
    
//    public static char[][] goals;
    public char[][] goals;

    /*
        The box colors are indexed alphabetically. So this.boxColors[0] is the color of A boxes, 
        this.boxColor[1] is the color of B boxes, etc.
    */
    public static Color[] boxColors;
 
    public final State parent;
    public final Action[] jointAction;
    private final int g;
    public int[][] distancegrid;

    private int hash = 0;


    // Constructs an initial state.
    // Arguments are not copied, and therefore should not be modified after being passed in.
    public State(int[] agentRows, int[] agentCols, Color[] agentColors, boolean[][] walls,
                 char[][] boxes, Color[] boxColors, char[][] goals, int[][] distancegrid
    )
    {
        this.agentRows = agentRows;
        this.agentCols = agentCols;
        this.agentColors = agentColors;
        this.walls = walls;
        this.boxes = boxes;
        this.boxColors = boxColors;
        this.goals = goals;
        this.parent = null;
        this.jointAction = null;
        this.g = 0;
        this.distancegrid = distancegrid;
    }


    // Constructs the state resulting from applying jointAction in parent.
    // Precondition: Joint action must be applicable and non-conflicting in parent state.
    public State(State parent, Action[] jointAction)
    {
        // Copy parent
        this.agentRows = Arrays.copyOf(parent.agentRows, parent.agentRows.length);
        this.agentCols = Arrays.copyOf(parent.agentCols, parent.agentCols.length);
        this.boxes = new char[parent.boxes.length][];
        this.goals = parent.goals;
        this.walls = parent.walls;
        this.distancegrid = parent.distancegrid;
        for (int i = 0; i < parent.boxes.length; i++)
        {
            this.boxes[i] = Arrays.copyOf(parent.boxes[i], parent.boxes[i].length);
        }

        // Set own parameters
        this.parent = parent;
        this.jointAction = Arrays.copyOf(jointAction, jointAction.length);
        this.g = parent.g + 1;

        // Apply each action
        int numAgents = this.agentRows.length;
        for (int agent = 0; agent < numAgents; ++agent)
        {
            Action action = jointAction[agent];
            char box;

            switch (action.type)
            {
                case NoOp:
                    break;

                case Move:
                    this.agentRows[agent] += action.agentRowDelta;
                    this.agentCols[agent] += action.agentColDelta;
                    break;
                case Push:
                    this.agentRows[agent] += action.agentRowDelta;
                    this.agentCols[agent] += action.agentColDelta;
                    box = this.boxes[this.agentRows[agent]][this.agentCols[agent]];
                    this.boxes[this.agentRows[agent] + action.boxRowDelta][this.agentCols[agent] + action.boxColDelta] = box;
                    this.boxes[this.agentRows[agent]][this.agentCols[agent]] = 0;
                    break;
                case Pull:
                    box = this.boxes[this.agentRows[agent] - action.boxRowDelta][this.agentCols[agent] - action.boxColDelta];
                    this.boxes[this.agentRows[agent]][this.agentCols[agent]] = box;
                    this.boxes[this.agentRows[agent] - action.boxRowDelta][this.agentCols[agent] - action.boxColDelta] = 0;
                    this.agentRows[agent] += action.agentRowDelta;
                    this.agentCols[agent] += action.agentColDelta;
                    break;
            }
        }
    }

    public int g()
    {
        return this.g;
    }
    
    public void setGoalstate(char[][] goal) {
    	this.goals = goal;
    }
    
    public int[] getSingleAgentRow(int agent) {
    	int[] single = new int[] {this.agentRows[agent]};
    	return single;
    }
    
    public int[] getSingleAgentCol(int agent) {
    	int[] single = new int[] {this.agentCols[agent]};
    	return single;
    }
    
    public char[][] getSingleAgentBoxes(int agent) {
    	char[][] boxes = new char[this.boxes.length][this.boxes[0].length];
    	
    	for (int row=0; row<this.boxes.length; row++) {
    		for (int col=0; col<this.boxes[row].length; col++) {
    			
    			char box = this.boxes[row][col];
    			if ('A' <= box && box <= 'Z') {
    				if (this.agentColors[agent].equals(boxColors[box - 'A'])) {
    					boxes[row][col] = box;
    				}
    			}
    			
    		}
    	}
    	return boxes;
    }
    
    //Function to return position of all other agents and their boxes
    //Adds walls to these entities
    public boolean[][] otherEntities(int agent) {
    	boolean[][] walls = new boolean[this.walls.length][this.walls[0].length];
    	
    	for (int row=0; row<this.walls.length; row++) {
    		for (int col=0; col<this.walls[row].length; col++) {
    			walls[row][col] = this.walls[row][col];
    			char box = this.boxes[row][col];
    			if ('A' <= box && box <= 'Z') {
    				if (!this.agentColors[agent].equals(boxColors[box - 'A'])) {
    					walls[row][col] = true;
    				}
    			}
    		}
    	}
    	
    	for (int i=0; i<this.agentRows.length; i++) {
    		if (i != agent) {
    			walls[this.agentRows[i]][this.agentCols[i]] = true;
    		}
    	}
    	
    	return walls;
    }
    
    
    public LinkedList<char[][]> getAgentSubGoals(int agent) {
    	
    	LinkedList<char[][]> subgoalsBox = new LinkedList<char[][]>();
    	LinkedList<char[][]> subgoalsBoxPrio = new LinkedList<char[][]>();
    	 
    	for (int row=1; row<this.goals.length - 1; row ++) {
        	for (int col=1; col < this.goals[row].length - 1 ; col++) {
        		
        		char goal = this.goals[row][col];
        		
        		if ('A' <= goal && goal <= 'Z') {
        			
        			if (this.agentColors[agent].equals(this.boxColors[goal - 'A'])) {
        				char[][] subgoal = new char[this.goals.length][this.goals[0].length];
            			subgoal[row][col] = goal;
            			
            			if (prioritizeSubGoal(row, col)) {
            				subgoalsBoxPrio.addLast(subgoal);
            			} else {
            				subgoalsBox.addLast(subgoal);
            			}
        			}
        			
                } else if ('0' <= goal && goal <= '9') {
                	if (agent == Integer.parseInt(String.valueOf(goal))) {
                		char[][] subgoal = new char[this.goals.length][this.goals[0].length];
            			subgoal[row][col] = goal;
            			subgoalsBox.addLast(subgoal);
                	}
                }
        	}
        }
    	
    	for (char [][] priosubgoal : subgoalsBoxPrio) {
    		subgoalsBox.addFirst(priosubgoal);
    	}
    	
//    	LinkedList<char[][]> subgoal_split_all = new LinkedList<char[][]>();
//    	
//    	for (char[][] subgoal : subgoalsBox) {
//    		LinkedList<char[][]> subgoal_split = new LinkedList<char[][]>();
//    		subgoal_split = splitSubgoal(subgoal, agent);
//    		for (char[][] subgoalS : subgoal_split) {
//    			subgoal_split_all.addLast(subgoalS);
//    		}
//    	}
    	
    	return subgoalsBox;
    }
    
    
    public LinkedList<char[][]> splitSubgoal(char[][] subgoal, int agent) {
    	
    	char[][] findBoxGoal = new char[subgoal.length][subgoal[0].length];
    	String agentString = Integer.toString(0);
    	char agentchar = agentString.charAt(0);
//    	System.err.println("AGENTCHAR" + agentchar);
    	LinkedList<char[][]> subgoal_split = new LinkedList<char[][]>();
    	char subgoal_char = 0;
    	
    	outerloop1:
    	for (int i=1; i<subgoal.length-1; i++) {
    		for (int j=1; j<subgoal[i].length; j++) {
    			if (subgoal[i][j] != 0) {
    				subgoal_char = subgoal[i][j];
    				break outerloop1;
    			}
    		}
    	}
    	
    	if (subgoal_char >= '0' && subgoal_char <= '9') {
    		subgoal_split.addFirst(subgoal);
    		return subgoal_split;
    	}
    	
    	outerloop2:
    	for (int i=1; i<this.boxes.length-1; i++) {
    		for (int j=1; j<this.boxes[i].length; j++) {
    			char boxchar = this.boxes[i][j];
    			if (boxchar == subgoal_char) {
    				if (this.cellIsFree(i-1,j)) {
    					findBoxGoal[i-1][j] = agentchar;
    					break outerloop2;
    				} else if (this.cellIsFree(i+1,j)) {
    					findBoxGoal[i+1][j] = agentchar;
    					break outerloop2;
    				} else if (this.cellIsFree(i,j-1)) {
    					findBoxGoal[i][j-1] = agentchar;
    					break outerloop2;
    				} else if (this.cellIsFree(i,j+1)) {
    					findBoxGoal[i][j+1] = agentchar;
    					break outerloop2;
    				}
    				
    				if (!this.walls[i-1][j]) {
    					findBoxGoal[i-1][j] = agentchar;
    					break outerloop2;
    				} else if (!this.walls[i+1][j]) {
    					findBoxGoal[i+1][j] = agentchar;
    					break outerloop2;
    				} else if (!this.walls[i][j-1]) {
    					findBoxGoal[i][j-1] = agentchar;
    					break outerloop2;
    				} else if (!this.walls[i][j+1]) {
    					findBoxGoal[i][j+1] = agentchar;
    					break outerloop2;
    				}
    				
    			}
    		}
    	}
    	
    	subgoal_split.addFirst(findBoxGoal);
    	subgoal_split.addLast(subgoal);
    	
    	return subgoal_split;
    }
    
    public int[][] getdistance(int row_source, int col_source) {
    	int[][] init_grid = new int[this.boxes.length][this.boxes[0].length];
    	init_grid[row_source][col_source] = 1;
    	LinkedList<int[]> queue = new LinkedList<int[]>();
    	queue.add(new int[] {row_source, col_source});
    	
    	return distance(init_grid, queue);
    }
    
    public int[][] distance(int[][] grid, LinkedList<int[]> queue) {
    	if (queue.isEmpty()) {
    		return grid;
    	}
    	
    	int[] element = queue.pollFirst();
    	int i = element[0];
    	int j = element[1];
    	int dist = grid[i][j];
    	
    	//North
    	if (i > 0) {
    		if (!this.walls[i-1][j]) {
        		if (grid[i-1][j] == 0) {
        			grid[i-1][j] = dist + 1;
        			queue.add(new int[]{i-1, j});
        		}
        	}
    	}
    	
    	//South
    	if (i+1 < grid.length) {
    		if (!this.walls[i+1][j]) {
        		if (grid[i+1][j] == 0) {
        			grid[i+1][j] = dist + 1;
        			queue.add(new int[]{i+1, j});
        		}
        	}
    	}
    	
    	//East
    	if(j+1 < grid[0].length) {
    		if (!this.walls[i][j+1]) {
        		if (grid[i][j+1] == 0) {
        			grid[i][j+1] = dist + 1;
        			queue.add(new int[]{i, j+1});
        		}
        	}
    	}
    	
    	//West
    	if(j > 0) {
    		if (!this.walls[i][j-1]) {
        		if (grid[i][j-1] == 0) {
        			grid[i][j-1] = dist + 1;
        			queue.add(new int[]{i, j-1});
        		}
        	}
    	}
    	
    	
    	return distance(grid, queue);
    	
    }
    
    
    
    public boolean prioritizeSubGoal(int row, int col) {
    	
    	char north = this.goals[row-1][col];
    	char south = this.goals[row+1][col];
    	char west = this.goals[row][col-1];
    	char east = this.goals[row][col+1];
    	
    	if (this.walls[row-1][col] || ('A' <= north && north <= 'Z')) {
    		if (this.walls[row+1][col] || ('A' <= south && south <= 'Z')) {
    			if (this.walls[row][col-1] || ('A' <= west && west <= 'Z')) {
    				if (this.walls[row][col+1] || ('A' <= east && east <= 'Z')) {
    					return true;
    				}
    			}
    		}
    	}
    	
    	return false;
    	
    }
    
    

    public boolean isGoalState()
    {
        for (int row = 1; row < this.goals.length - 1; row++)
        {
            for (int col = 1; col < this.goals[row].length - 1; col++)
            {
                char goal = this.goals[row][col];

                if ('A' <= goal && goal <= 'Z' && this.boxes[row][col] != goal)
                {
                    return false;
                }
                else if ('0' <= goal && goal <= '9' &&
                         !(this.agentRows[goal - '0'] == row && this.agentCols[goal - '0'] == col))
                {
                    return false;
                }
            }
        }
        
        return true;
    }

    public ArrayList<State> getExpandedStates()
    {
        int numAgents = this.agentRows.length;

        // Determine list of applicable actions for each individual agent.
        Action[][] applicableActions = new Action[numAgents][];
        for (int agent = 0; agent < numAgents; ++agent)
        {
            ArrayList<Action> agentActions = new ArrayList<>(Action.values().length);
            for (Action action : Action.values())
            {
                if (this.isApplicable(agent, action))
                {
                    agentActions.add(action);
                }
            }
            applicableActions[agent] = agentActions.toArray(new Action[0]);
        }
        
        
        

        // Iterate over joint actions, check conflict and generate child states.
        Action[] jointAction = new Action[numAgents];
        int[] actionsPermutation = new int[numAgents];
        ArrayList<State> expandedStates = new ArrayList<>();
        while (true)
        {
            for (int agent = 0; agent < numAgents; ++agent)
            {
                jointAction[agent] = applicableActions[agent][actionsPermutation[agent]];
            }
            
            if (!this.isConflicting(jointAction))
            {
                expandedStates.add(new State(this, jointAction));
            }

            // Advance permutation
            boolean done = false;
            for (int agent = 0; agent < numAgents; ++agent)
            {
                if (actionsPermutation[agent] < applicableActions[agent].length - 1)
                {
                    ++actionsPermutation[agent];
                    break;
                }
                else
                {
                    actionsPermutation[agent] = 0;
                    if (agent == numAgents - 1)
                    {
                        done = true;
                    }
                }
            }

            // Last permutation?
            if (done)
            {
                break;
            }
        }

        Collections.shuffle(expandedStates, State.RNG);
        
        return expandedStates;
    }

    public boolean isApplicable(int agent, Action action)
    {
        int agentRow = this.agentRows[agent];
        int agentCol = this.agentCols[agent];
        Color agentColor = this.agentColors[agent];
        int boxRow;
        int boxCol;
        char box;
        int destinationRow;
        int destinationCol;
        switch (action.type)
        {
            case NoOp:
                return true;

            case Move:
                destinationRow = agentRow + action.agentRowDelta;
                destinationCol = agentCol + action.agentColDelta;
                return this.cellIsFree(destinationRow, destinationCol);

            case Push:
                destinationRow = agentRow + action.agentRowDelta;
                destinationCol = agentCol + action.agentColDelta;
                box = this.boxes[destinationRow][destinationCol];
                
                if ('A' <= box && box <= 'Z') {
                	if (agentColor.equals(boxColors[box - 65])) {
                		boxRow = destinationRow + action.boxRowDelta;
                        boxCol = destinationCol + action.boxColDelta;
                        return this.cellIsFree(boxRow, boxCol);
                	}
                 
                }
                return false;

            case Pull:
                destinationRow = agentRow + action.agentRowDelta;
                destinationCol = agentCol + action.agentColDelta;
                
                if (this.cellIsFree(destinationRow, destinationCol)) {
                	boxRow = agentRow;
                    boxCol = agentCol;
                	box = this.boxes[boxRow-action.boxRowDelta][boxCol-action.boxColDelta];
                	if ('A' <= box && box <= 'Z') {
                		if (agentColor.equals(boxColors[box - 65])) {
                			return true;
                		}
                	}
                }
                
                return false;
        }

        // Unreachable:
        return false;
    }

    public boolean isConflicting(Action[] jointAction)
    {
        int numAgents = this.agentRows.length;

        int[] destinationRows = new int[numAgents]; // row of new cell to become occupied by action
        int[] destinationCols = new int[numAgents]; // column of new cell to become occupied by action
        int[] boxRows = new int[numAgents]; // current row of box moved by action
        int[] boxCols = new int[numAgents]; // current column of box moved by action

        // Collect cells to be occupied and boxes to be moved
        for (int agent = 0; agent < numAgents; ++agent)
        {
            Action action = jointAction[agent];
            int agentRow = this.agentRows[agent];
            int agentCol = this.agentCols[agent];
            int boxRow;
            int boxCol;

            switch (action.type)
            {
                case NoOp:
                    break;

                case Move:
                    destinationRows[agent] = agentRow + action.agentRowDelta;
                    destinationCols[agent] = agentCol + action.agentColDelta;
                    boxRows[agent] = agentRow; // Distinct dummy value
                    boxCols[agent] = agentCol; // Distinct dummy value
                    break;

                case Push:
                    destinationRows[agent] = agentRow + action.agentRowDelta;
                    destinationCols[agent] = agentCol + action.agentColDelta;
                    boxRows[agent] = destinationRows[agent] + action.boxRowDelta;
                    boxCols[agent] = destinationCols[agent] + action.boxColDelta;
                    break;

                case Pull:
                    destinationRows[agent] = agentRow + action.agentRowDelta;
                    destinationCols[agent] = agentCol + action.agentColDelta;
                    boxRows[agent] = agentRow;
                    boxCols[agent] = agentCol;
                    break;
           }
            
        }
        //DEFINE OCCUPIED POSITIONS
        
        
        
        for (int a1 = 0; a1 < numAgents; ++a1)
        {
            if (jointAction[a1] == Action.NoOp)
            {
                continue;
            }

            for (int a2 = a1 + 1; a2 < numAgents; ++a2)
            {
                if (jointAction[a2] == Action.NoOp)
                {
                    continue;
                }

                // Moving into same cell?
                if (destinationRows[a1] == destinationRows[a2] && destinationCols[a1] == destinationCols[a2])
                {
                    return true;
                }

                if (boxRows[a1] == boxRows[a2] && boxCols[a1] == boxCols[a2])
                {
                    return true;
                }

                if (destinationRows[a1] == boxRows[a2]  && destinationCols[a1] == boxCols[a2])
                {
                    return true;
                }
                
                if (destinationRows[a2] == boxRows[a1]  && destinationCols[a2] == boxCols[a1])
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean cellIsFree(int row, int col)
    {
        return !this.walls[row][col] && this.boxes[row][col] == 0 && this.agentAt(row, col) == 0;
    }

    private char agentAt(int row, int col)
    {
        for (int i = 0; i < this.agentRows.length; i++)
        {
            if (this.agentRows[i] == row && this.agentCols[i] == col)
            {
                return (char) ('0' + i);
            }
        }
        return 0;
    }

    public Action[][] extractPlan()
    {
        Action[][] plan = new Action[this.g][];
        State state = this;
        while (state.jointAction != null)
        {
            plan[state.g - 1] = state.jointAction;
            state = state.parent;
        }
        return plan;
    }

    @Override
    public int hashCode()
    {
        if (this.hash == 0)
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(this.agentColors);
            result = prime * result + Arrays.hashCode(this.boxColors);
            result = prime * result + Arrays.deepHashCode(this.walls);
            result = prime * result + Arrays.deepHashCode(this.goals);
            result = prime * result + Arrays.hashCode(this.agentRows);
            result = prime * result + Arrays.hashCode(this.agentCols);
            for (int row = 0; row < this.boxes.length; ++row)
            {
                for (int col = 0; col < this.boxes[row].length; ++col)
                {
                    char c = this.boxes[row][col];
                    if (c != 0)
                    {
                        result = prime * result + (row * this.boxes[row].length + col) * c;
                    }
                }
            }
            this.hash = result;
        }
        return this.hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (this.getClass() != obj.getClass())
        {
            return false;
        }
        State other = (State) obj;
        return Arrays.equals(this.agentRows, other.agentRows) &&
               Arrays.equals(this.agentCols, other.agentCols) &&
               Arrays.equals(this.agentColors, other.agentColors) &&
               Arrays.deepEquals(this.walls, other.walls) &&
               Arrays.deepEquals(this.boxes, other.boxes) &&
               Arrays.equals(this.boxColors, other.boxColors) &&
               Arrays.deepEquals(this.goals, other.goals);
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for (int row = 0; row < this.walls.length; row++)
        {
            for (int col = 0; col < this.walls[row].length; col++)
            {
                if (this.boxes[row][col] > 0)
                {
                    s.append(this.boxes[row][col]);
                }
                else if (this.walls[row][col])
                {
                    s.append("+");
                }
                else if (this.agentAt(row, col) != 0)
                {
                    s.append(this.agentAt(row, col));
                }
                else
                {
                    s.append(" ");
                }
            }
            s.append("\n");
        }
        return s.toString();
    }
    
    public Coordinates conflictRecognition(int agent)
    {
        //get the goal color
        char goal = '0';
        int goalI = -1;
        int goalJ = -1;
        int agentI = -1;
        int agentJ = -1;

        LinkedList<Point> tempLinkedList = new LinkedList<Point>();
        Queue<Coordinates> boxCords = new LinkedList<Coordinates>();
        for(int i = 0; i < goals.length; ++i)
        {
            for(int j = 0; j < goals[i].length; ++j)
            {
                if(goals[i][j] != 0)
                {
                    goal = goals[i][j];
                    goalI = i;
                    goalJ = j;
                    break;
                }
            }
        }
        
        System.err.println("GOALCOORD: " + goalI + ":" + goalJ);
        System.err.println("GOALCHAR: " + goal);
        

        boolean[][] tempWalls = new boolean[walls.length][];
        for (int i = 0; i < walls.length; i++)
        {
                tempWalls[i] = Arrays.copyOf(walls[i], walls[i].length);
        }

        Color c = boxColors[goal - 'A'];

        //turn everything else to walls
        for(int i = 0; i < boxes.length; ++i)
        {
            for(int j = 0; j < boxes[i].length; ++j)
            {
                if(boxes[i][j] != 0)
                {
                    if(boxColors[boxes[i][j] - 'A'] != c)
                    {
                        tempWalls[i][j] = true;
                    }
                    else
                    {
                        if(goal == boxes[i][j])
                        {
                        	System.err.println("BOXCOORDINATES: " + i + ":" + j);
                            boxCords.add(new Coordinates(i,j,'0'));
                        }
                    }
                }
//                if(this.agentAt(i, j) != 0)
//                {
//                    if(agentColors[this.agentAt(i, j)-'0'] != c)
//                    {
//                        tempWalls[i][j] = true;
//                    }
//                    else
//                    {
//                        agentI = i;
//                        agentJ = j;
//                    }
//                }
            }
        }
        
        for (int i=0; i<this.agentRows.length; i++) {
        	if (i != agent) {
        		tempWalls[this.agentRows[i]][this.agentCols[i]] = true;
        	}
        }
        
        agentI = this.agentRows[agent];
		agentJ = this.agentCols[agent];


        int reachedBox = -1;
        int count = 0;

        Queue<Coordinates> boxCords2 = new LinkedList<>(boxCords);

        while(!boxCords2.isEmpty())
        {

            System.err.println("Searching through boxes");
            Queue<Coordinates> q = new LinkedList<Coordinates>();

            q.add(new Coordinates(agentI, agentJ, '0'));

            Coordinates boxCord = boxCords2.poll();

            boolean[][] tempWallsCopy = new boolean[tempWalls.length][];
            for (int i = 0; i < tempWalls.length; i++)
            {
                tempWallsCopy[i] = Arrays.copyOf(tempWalls[i], tempWalls[i].length);
            }

            tempLinkedList.add(new Point(agentI, agentJ, null));
            if(recursiveBFS(q, tempWallsCopy, boxCord, tempLinkedList))
            {
                reachedBox = count;

                System.err.println("Box is reachable");

                Queue<Coordinates> q2 = new LinkedList<Coordinates>();
                q2.add(boxCord);

                for (int i = 0; i < tempWalls.length; i++)
                {
                    tempWallsCopy[i] = Arrays.copyOf(tempWalls[i], tempWalls[i].length);
                }

                tempLinkedList.add(new Point(boxCord.x, boxCord.y, null));

                if(recursiveBFS(q2,tempWallsCopy,new Coordinates(goalI, goalJ, '0'), tempLinkedList))
                {
                    System.err.println("Goal is reachable");
                    return new Coordinates(-1, -1, '0');
                }
            }

            count++;
        }

        //we can't reach any box, try to see what's th problem at the first one
        if(reachedBox == -1)
        {
            System.err.println("Didn't reach box");
            Coordinates boxCord = boxCords.poll();
            LinkedList<Point> collection = new LinkedList<Point>();
            LinkedList<Coordinates> path = new LinkedList<Coordinates>();
            Queue<Coordinates> q = new LinkedList<Coordinates>();

            q.add(new Coordinates(agentI, agentJ, '0'));
            collection.add(new Point(agentI, agentJ, null));

            for (int i = 0; i < walls.length; i++)
            {
                tempWalls[i] = Arrays.copyOf(walls[i], walls[i].length);
            }

            recursiveBFS(q,tempWalls,new Coordinates(boxCord.x, boxCord.y, '0'), collection);

            Point p = getPoint(boxCord.x, boxCord.y, collection);

            while(p != null)
            {
                path.addFirst(new Coordinates(p.x, p.y, '0'));
                p=p.parent;
            }

            for(int i = 0; i < path.size(); ++i)
            {
                if(boxes[path.get(i).x][path.get(i).y] != 0)
                {
                    if(boxColors[boxes[path.get(i).x][path.get(i).y] - 'A'] != c)
                    {
                        System.err.println("Box in the way: " + path.get(i).x + " " + path.get(i).y);
                        return new Coordinates(path.get(i).x, path.get(i).y, '0'); 
                    }
                }

                if(this.agentAt(path.get(i).x, path.get(i).y) != 0)
                {
                    if(agentColors[this.agentAt(path.get(i).x, path.get(i).y)-'0'] != c)
                    {
                        System.err.println("Agent in the way: " + path.get(i).x + " " + path.get(i).y);
                        return new Coordinates(path.get(i).x, path.get(i).y, '0');
                    }
                }
            }
        }
        // we can't reach the goal
        else
        {
            System.err.println("Didn't reach the goal");
            Coordinates boxCord = boxCords.poll();
            reachedBox--;
            while(reachedBox != -1)
            {
                boxCord = boxCords.poll();
                reachedBox--;
            }
            LinkedList<Point> collection = new LinkedList<Point>();
            LinkedList<Coordinates> path = new LinkedList<Coordinates>();
            Queue<Coordinates> q = new LinkedList<Coordinates>();

            q.add(new Coordinates(boxCord.x, boxCord.y, '0'));
            collection.add(new Point(boxCord.x, boxCord.y, null));

            for (int i = 0; i < walls.length; i++)
            {
                tempWalls[i] = Arrays.copyOf(walls[i], walls[i].length);
            }

            recursiveBFS(q,tempWalls,new Coordinates(goalI, goalJ, '0'), collection);

            Point p = getPoint(goalI, goalJ, collection);

            while(p != null)
            {
                path.addFirst(new Coordinates(p.x, p.y, '0'));
                p=p.parent;
            }

            for(int i = 0; i < path.size(); ++i)
            {
                if(boxes[path.get(i).x][path.get(i).y] != 0)
                {
                    if(boxColors[boxes[path.get(i).x][path.get(i).y] - 'A'] != c)
                    {
                        System.err.println("Box in the way: " + path.get(i).x + " " + path.get(i).y);
                        return new Coordinates(path.get(i).x, path.get(i).y, '0'); 
                    }
                }

                if(this.agentAt(path.get(i).x, path.get(i).y) != 0)
                {
                    if(agentColors[this.agentAt(path.get(i).x, path.get(i).y)-'0'] != c)
                    {
                        System.err.println("Agent in the way: " + path.get(i).x + " " + path.get(i).y);
                        return new Coordinates(path.get(i).x, path.get(i).y, '0');
                    }
                }
            }
        }
        return new Coordinates(-2, -2, '0');

    }

    public Point getPoint(int x, int y, LinkedList<Point> path)
    {
        for(int i = 0; i < path.size(); ++i)
        {
            if(path.get(i).x == x && path.get(i).y == y)
            {
                return path.get(i);
            }
        }
        return new Point(-1, -1, null);
    }

    public boolean recursiveBFS(Queue<Coordinates> q,
                                      boolean[][] visited, Coordinates goal, LinkedList<Point> path) {
        if (q.isEmpty()) {
            return false;
        }
        Coordinates v = q.poll();

        if (v.x == goal.x && v.y == goal.y){
            return true;
        }

        if(!visited[v.x+1][v.y])
        {
            visited[v.x+1][v.y] = true;
            q.add(new Coordinates(v.x+1, v.y, v.character));
            path.add(new Point(v.x+1, v.y, getPoint(v.x, v.y, path)));
        }

        if(!visited[v.x-1][v.y])
        {
            visited[v.x-1][v.y] = true;
            q.add(new Coordinates(v.x-1, v.y, v.character));
            path.add(new Point(v.x-1, v.y, getPoint(v.x, v.y, path)));
        }

        if(!visited[v.x][v.y+1])
        {
            visited[v.x][v.y+1] = true;
            q.add(new Coordinates(v.x, v.y+1, v.character));
            path.add(new Point(v.x, v.y+1, getPoint(v.x, v.y, path)));
        }

        if(!visited[v.x][v.y-1])
        {
            visited[v.x][v.y-1] = true;
            q.add(new Coordinates(v.x, v.y-1, v.character));
            path.add(new Point(v.x, v.y-1, getPoint(v.x, v.y, path)));
        }

        return recursiveBFS(q, visited, goal, path);
    }
}
