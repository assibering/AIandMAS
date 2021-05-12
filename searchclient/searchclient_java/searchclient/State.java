package searchclient;

import java.util.*;

public class State {
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
    ) {
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
    State(State parent, Action[] jointAction) {
        // Copy parent
        this.agentRows = Arrays.copyOf(parent.agentRows, parent.agentRows.length);
        this.agentCols = Arrays.copyOf(parent.agentCols, parent.agentCols.length);
        this.boxes = new char[parent.boxes.length][];
        this.goals = parent.goals;
        this.walls = parent.walls;
        this.distancegrid = parent.distancegrid;
        for (int i = 0; i < parent.boxes.length; i++) {
            this.boxes[i] = Arrays.copyOf(parent.boxes[i], parent.boxes[i].length);
        }

        // Set own parameters
        this.parent = parent;
        this.jointAction = Arrays.copyOf(jointAction, jointAction.length);
        this.g = parent.g + 1;

        // Apply each action
        int numAgents = this.agentRows.length;
        for (int agent = 0; agent < numAgents; ++agent) {
            Action action = jointAction[agent];
            char box;

            switch (action.type) {
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

    public int g() {
        return this.g;
    }

    public void setGoalstate(char[][] goal) {
        this.goals = goal;
    }

    public int[] getSingleAgentRow(int agent) {
        return new int[]{this.agentRows[agent]};
    }

    public int[] getSingleAgentCol(int agent) {
        return new int[]{this.agentCols[agent]};
    }

    public char[][] getSingleAgentBoxes(int agent, char[][] subgoal) {
        char[][] boxes = new char[this.boxes.length][this.boxes[0].length];
        char currentGoal = 0;
        for (int row = 1; row < subgoal.length; row++) {
            for (int col = 1; col < subgoal[row].length; col++) {
                if (subgoal[row][col] != 0) {
                    currentGoal = subgoal[row][col];
                    break;
                }
            }
            if (currentGoal != 0)
                break;
        }

        for (int row = 1; row < this.boxes.length; row++) {
            for (int col = 1; col < this.boxes[row].length; col++) {

                char box = this.boxes[row][col];
                if ('A' <= box && box <= 'Z' && box == currentGoal) {
                    if (agentColors[agent].equals(boxColors[box - 'A'])) {
                        boxes[row][col] = box;
                    }
                }

            }
        }
        return boxes;
    }


    public LinkedList<char[][]> getAgentSubGoals(int agent) {

        LinkedList<char[][]> subgoalsBox = new LinkedList<>();
        LinkedList<char[][]> subgoalsBoxPrio = new LinkedList<>();
        LinkedList<char[][]> result = new LinkedList<>();

        for (int row = 0; row < this.goals.length; row++) {
            for (int col = 0; col < this.goals[row].length; col++) {
                char goal = this.goals[row][col];

                if ('A' <= goal && goal <= 'Z') {

                    if (agentColors[agent].equals(boxColors[goal - 65])) {
                        char[][] subgoal = new char[this.goals.length][this.goals[0].length];
                        subgoal[row][col] = goal;

                        if (prioritizeSubGoal(row, col)) {
                            subgoalsBoxPrio.add(subgoal);
                        } else {
                            subgoalsBox.add(subgoal);
                        }
                    }

                } else if ('0' <= goal && goal <= '9') {
                    if (agent == Integer.parseInt(String.valueOf(goal))) {
                        char[][] subgoal = new char[this.goals.length][this.goals[0].length];
                        subgoal[row][col] = goal;
                        subgoalsBox.add(subgoal);
                    }
                }
            }
        }

        result.addAll(subgoalsBoxPrio);
        result.addAll(subgoalsBox);
        //    	LinkedList<char[][]> subgoal_split_all = new LinkedList<char[][]>();
//
//    	for (char[][] subgoal : subgoalsBox) {
//    		LinkedList<char[][]> subgoal_split = new LinkedList<char[][]>();
//    		subgoal_split = splitSubgoal(subgoal, agent);
//    		for (char[][] subgoalS : subgoal_split) {
//    			subgoal_split_all.addLast(subgoalS);
//    		}
//    	}

        return result;
    }

    public LinkedList<char[][]> splitSubgoal(char[][] subgoal, int agent) {

        char[][] findBoxGoal = new char[subgoal.length][subgoal[0].length];
        String agentString = Integer.toString(0);
        char agentchar = agentString.charAt(0);
        System.err.println("AGENTCHAR" + agentchar);
        LinkedList<char[][]> subgoal_split = new LinkedList<char[][]>();
        char subgoal_char = 0;

        outerloop1:
        for (int i = 1; i < subgoal.length - 1; i++) {
    		for (int j=1; j<subgoal[i].length; j++) {
    			if (subgoal[i][j] != 0) {
    				subgoal_char = subgoal[i][j];
    				break outerloop1;
    			}
    		}
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
    	if (!this.walls[i-1][j]) {
    		if (grid[i-1][j] == 0) {
    			grid[i-1][j] = dist + 1;
    			queue.add(new int[]{i-1, j});
    		}
    	}

    	//South
    	if (!this.walls[i+1][j]) {
    		if (grid[i+1][j] == 0) {
    			grid[i+1][j] = dist + 1;
    			queue.add(new int[]{i+1, j});
    		}
    	}

        //East
    	if (!this.walls[i][j+1]) {
    		if (grid[i][j+1] == 0) {
    			grid[i][j+1] = dist + 1;
    			queue.add(new int[]{i, j+1});
    		}
    	}

        //West
    	if (!this.walls[i][j-1]) {
    		if (grid[i][j-1] == 0) {
    			grid[i][j-1] = dist + 1;
    			queue.add(new int[]{i, j-1});
    		}
    	}


        return distance(grid, queue);

    }


    public boolean prioritizeSubGoal(int row, int col) {

        char north = this.goals[row - 1][col];
        char south = this.goals[row + 1][col];
        char west = this.goals[row][col - 1];
        char east = this.goals[row][col + 1];

        if (this.walls[row - 1][col] || ('A' <= north && north <= 'Z')) {
            if (this.walls[row + 1][col] || ('A' <= south && south <= 'Z')) {
                if (this.walls[row][col - 1] || ('A' <= west && west <= 'Z')) {
                    if (this.walls[row][col + 1] || ('A' <= east && east <= 'Z')) {
                        return true;
                    }
                }
            }
        }

        return false;

    }


    public boolean isGoalState() {
        for (int row = 1; row < this.goals.length - 1; row++) {
            for (int col = 1; col < this.goals[row].length - 1; col++) {
                char goal = this.goals[row][col];

                if ('A' <= goal && goal <= 'Z' && this.boxes[row][col] != goal) {
                    return false;
                } else if ('0' <= goal && goal <= '9' &&
                        !(this.agentRows[goal - '0'] == row && this.agentCols[goal - '0'] == col)) {
                    return false;
                }
            }
        }

        return true;
    }

    public ArrayList<State> getExpandedStates() {
        int numAgents = this.agentRows.length;

        // Determine list of applicable actions for each individual agent.
        Action[][] applicableActions = new Action[numAgents][];
        for (int agent = 0; agent < numAgents; ++agent) {
            ArrayList<Action> agentActions = new ArrayList<>(Action.values().length);
            for (Action action : Action.values()) {
                if (this.isApplicable(agent, action)) {
                    agentActions.add(action);
                }
            }
            applicableActions[agent] = agentActions.toArray(new Action[0]);
        }


        // Iterate over joint actions, check conflict and generate child states.
        Action[] jointAction = new Action[numAgents];
        int[] actionsPermutation = new int[numAgents];
        ArrayList<State> expandedStates = new ArrayList<>();
        while (true) {
            for (int agent = 0; agent < numAgents; ++agent) {
                jointAction[agent] = applicableActions[agent][actionsPermutation[agent]];
            }

            if (!this.isConflicting(jointAction)) {
                expandedStates.add(new State(this, jointAction));
            }

            // Advance permutation
            boolean done = false;
            for (int agent = 0; agent < numAgents; ++agent) {
                if (actionsPermutation[agent] < applicableActions[agent].length - 1) {
                    ++actionsPermutation[agent];
                    break;
                } else {
                    actionsPermutation[agent] = 0;
                    if (agent == numAgents - 1) {
                        done = true;
                    }
                }
            }

            // Last permutation?
            if (done) {
                break;
            }
        }

        Collections.shuffle(expandedStates, State.RNG);

        return expandedStates;
    }

    boolean isApplicable(int agent, Action action) {
        int agentRow = this.agentRows[agent];
        int agentCol = this.agentCols[agent];
        Color agentColor = this.agentColors[agent];
        int boxRow;
        int boxCol;
        char box;
        int destinationRow;
        int destinationCol;
        switch (action.type) {
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
                    box = this.boxes[boxRow - action.boxRowDelta][boxCol - action.boxColDelta];
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

    private boolean isConflicting(Action[] jointAction) {
        int numAgents = this.agentRows.length;

        int[] destinationRows = new int[numAgents]; // row of new cell to become occupied by action
        int[] destinationCols = new int[numAgents]; // column of new cell to become occupied by action
        int[] boxRows = new int[numAgents]; // current row of box moved by action
        int[] boxCols = new int[numAgents]; // current column of box moved by action

        // Collect cells to be occupied and boxes to be moved
        for (int agent = 0; agent < numAgents; ++agent) {
            Action action = jointAction[agent];
            int agentRow = this.agentRows[agent];
            int agentCol = this.agentCols[agent];
            int boxRow;
            int boxCol;

            switch (action.type) {
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


        for (int a1 = 0; a1 < numAgents; ++a1) {
            if (jointAction[a1] == Action.NoOp) {
                continue;
            }

            for (int a2 = a1 + 1; a2 < numAgents; ++a2) {
                if (jointAction[a2] == Action.NoOp) {
                    continue;
                }

                // Moving into same cell?
                if (destinationRows[a1] == destinationRows[a2] && destinationCols[a1] == destinationCols[a2]) {
                    return true;
                }

                if (boxRows[a1] == boxRows[a2] && boxCols[a1] == boxCols[a2]) {
                    return true;
                }

                if (destinationRows[a1] == boxRows[a2] && destinationCols[a1] == boxCols[a2]) {
                    return true;
                }

                if (destinationRows[a2] == boxRows[a1] && destinationCols[a2] == boxCols[a1]) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean cellIsFree(int row, int col) {
        return !this.walls[row][col] && this.boxes[row][col] == 0 && this.agentAt(row, col) == 0;
    }

    private char agentAt(int row, int col) {
        for (int i = 0; i < this.agentRows.length; i++) {
            if (this.agentRows[i] == row && this.agentCols[i] == col) {
                return (char) ('0' + i);
            }
        }
        return 0;
    }

    public Action[][] extractPlan() {
        Action[][] plan = new Action[this.g][];
        State state = this;
        while (state.jointAction != null) {
            plan[state.g - 1] = state.jointAction;
            state = state.parent;
        }
        return plan;
    }

    @Override
    public int hashCode() {
        if (this.hash == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(this.agentColors);
            result = prime * result + Arrays.hashCode(this.boxColors);
            result = prime * result + Arrays.deepHashCode(this.walls);
            result = prime * result + Arrays.deepHashCode(this.goals);
            result = prime * result + Arrays.hashCode(this.agentRows);
            result = prime * result + Arrays.hashCode(this.agentCols);
            for (int row = 0; row < this.boxes.length; ++row) {
                for (int col = 0; col < this.boxes[row].length; ++col) {
                    char c = this.boxes[row][col];
                    if (c != 0) {
                        result = prime * result + (row * this.boxes[row].length + col) * c;
                    }
                }
            }
            this.hash = result;
        }
        return this.hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
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
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int row = 0; row < this.walls.length; row++) {
            for (int col = 0; col < this.walls[row].length; col++) {
                if (this.boxes[row][col] > 0) {
                    s.append(this.boxes[row][col]);
                } else if (this.walls[row][col]) {
                    s.append("+");
                } else if (this.agentAt(row, col) != 0) {
                    s.append(this.agentAt(row, col));
                } else {
                    s.append(" ");
                }
            }
            s.append("\n");
        }
        return s.toString();
    }
}
