package searchclient;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Heuristic
        implements Comparator<State> {
    ArrayList<Coordinates> goalSet = new ArrayList<>();

    public Heuristic(State initialState) {

//        for (int i = 0; i < initialState.goals.length; i++) {
//            for (int j = 0; j < initialState.goals[0].length; j++) {
//                if (initialState.goals[i][j] != 0) {
//                    goalSet.add(new Coordinates(i, j, initialState.goals[i][j]));
//                }
//            }
//        }

        for (int i = 0; i < initialState.goals.length; i++) {
            for (int j = 0; j < initialState.goals[0].length; j++) {
                if (initialState.goals[i][j] != 0) {
                    goalSet.add(new Coordinates(i, j, initialState.goals[i][j]));
                }
            }
        }
    }

    public int h_new(State s) {

        char goalchar = 0;

        outerloop1:
        for (int i = 1; i < s.goals.length - 1; i++) {
            for (int j = 1; j < s.goals[i].length - 1; j++) {
                if (s.goals[i][j] != 0) {
                    goalchar = s.goals[i][j];
                    break outerloop1;
                }
            }
        }

        if (goalchar >= 'A' && goalchar <= 'Z') {

            int row = 0;
            int col = 0;
            outerloop2:
            for (int i = 1; i < s.boxes.length - 1; i++) {
                for (int j = 1; j < s.boxes[i].length - 1; j++) {
                    if (s.boxes[i][j] == goalchar) {
                        row = i;
                        col = j;
                        break outerloop2;
                    }
                }
            }

            return s.distancegrid[row][col];

        } else {

            int agentrow = s.agentRows[0];
            int agentcol = s.agentCols[0];

            return s.distancegrid[agentrow][agentcol];

        }
    }


    public int h(State s) {


        //NEW for multi-agent
        long[] agentDistancesForColours = new long[s.boxColors.length];
        long[] remainingGoalsPerColour = new long[s.boxColors.length];
        long[] remainingAgentGoalsPerColour = new long[s.boxColors.length];
        long[] manhattanDistancesPerColour = new long[s.boxColors.length];
        long[] wallsInManhattanDistancesPerColour = new long[s.boxColors.length];
        Arrays.fill(agentDistancesForColours, Long.MAX_VALUE);

        List<Coordinates> remainingGoals = goalSet.stream()
                .filter(entry -> s.boxes[entry.x][entry.y] != entry.character)
                .collect(Collectors.toList());

        List<Coordinates> remainingAgentGoals = goalSet.stream()
                .filter(entry -> entry.character < 'A' && (entry.x != s.agentRows[entry.character - '0'] || entry.y != s.agentCols[entry.character - '0']))
                .collect(Collectors.toList());

        for (int i = 0; i < remainingGoalsPerColour.length; i++) {
            int finalI = i;
            remainingGoalsPerColour[i] = remainingGoals.stream().filter(coord -> coord.character - 'A' == finalI).count();
            remainingAgentGoalsPerColour[i] = remainingAgentGoals.stream().filter(coord -> coord.character - '0' == finalI).count();
        }
        long remainingGoalsSize = remainingGoals.size();
        long manhattanDistances = 0;
        long wallsInManhattanDistances = 0;
        long minimumAgentDistance = Long.MAX_VALUE;
        if (remainingGoals.size() != 0 || remainingAgentGoals.size() != 0) {
            for (Coordinates goalCoords : remainingAgentGoals) {
                int agentX = s.agentRows[goalCoords.character - '0'];
                int agentY = s.agentCols[goalCoords.character - '0'];
                manhattanDistances += Math.abs(goalCoords.x - agentX) + Math.abs(goalCoords.y - agentY);
                manhattanDistancesPerColour[goalCoords.character - '0'] += Math.abs(goalCoords.x - agentX) + Math.abs(goalCoords.y - agentY);
                long wallsInManhattanDistances1 = 0, wallsInManhattanDistances2 = 0;
                for (int wallX = Math.min(goalCoords.x, agentX); wallX < Math.max(goalCoords.x, agentX); wallX++) {
                    if (s.walls[wallX][goalCoords.y]) {
                        wallsInManhattanDistances1++;
                    }
                    if (s.walls[wallX][agentY]) {
                        wallsInManhattanDistances2++;
                    }
                }
                for (int wallY = Math.min(goalCoords.y, agentY); wallY < Math.max(goalCoords.y, agentY); wallY++) {
                    if (s.walls[goalCoords.x][wallY]) {
                        wallsInManhattanDistances1++;
                    }
                    if (s.walls[agentX][wallY]) {
                        wallsInManhattanDistances2++;
                    }
                }
                wallsInManhattanDistances += 4 * Math.min(wallsInManhattanDistances1, wallsInManhattanDistances2);
                wallsInManhattanDistancesPerColour[goalCoords.character - '0'] += 4 * Math.min(wallsInManhattanDistances1, wallsInManhattanDistances2);

            }
            for (int i = 0; i < s.boxes.length; i++) {
                for (int j = 0; j < s.boxes[0].length && !remainingGoals.isEmpty(); j++) {
                    if (s.boxes[i][j] != 0) {
                        //Look for any goal
                        int boxX = i;
                        int boxY = j;
                        Optional<Coordinates> goalOpt = remainingGoals.stream()
                                .filter(entry -> entry.character == s.boxes[boxX][boxY])
                                .findFirst();
                        if (goalOpt.isEmpty()) {
                            continue;
                        }
                        Coordinates goalCoords = goalOpt.get();
                        manhattanDistances += Math.abs(goalCoords.x - boxX) + Math.abs(goalCoords.y - boxY);
                        manhattanDistancesPerColour[goalCoords.character - 'A'] += Math.abs(goalCoords.x - boxX) + Math.abs(goalCoords.y - boxY);
                        //Add walls in between
                        long wallsInManhattanDistances1 = 0, wallsInManhattanDistances2 = 0;
                        for (int wallX = Math.min(goalCoords.x, boxX); wallX < Math.max(goalCoords.x, boxX); wallX++) {
                            if (s.walls[wallX][goalCoords.y]) {
                                wallsInManhattanDistances1++;
                            }
                            if (s.walls[wallX][boxY]) {
                                wallsInManhattanDistances2++;
                            }
                        }
                        for (int wallY = Math.min(goalCoords.y, boxY); wallY < Math.max(goalCoords.y, boxY); wallY++) {
                            if (s.walls[goalCoords.x][wallY]) {
                                wallsInManhattanDistances1++;
                            }
                            if (s.walls[boxX][wallY]) {
                                wallsInManhattanDistances2++;
                            }
                        }
                        wallsInManhattanDistances += 4 * Math.min(wallsInManhattanDistances1, wallsInManhattanDistances2);
                        wallsInManhattanDistancesPerColour[goalCoords.character - 'A'] += 4 * Math.min(wallsInManhattanDistances1, wallsInManhattanDistances2);
                        //Look for any matching agent per box
                        for (int agent = 0; agent < s.agentCols.length; agent++) {
                            if (s.agentColors[agent] == s.boxColors[goalOpt.get().character - 'A']) {
                                long distance = Math.abs(boxX - s.agentRows[agent]) + Math.abs(boxY - s.agentCols[agent]);
                                //Add walls in between
                                wallsInManhattanDistances1 = 0;
                                wallsInManhattanDistances2 = 0;
                                for (int wallX = Math.min(s.agentRows[agent], boxX); wallX < Math.max(s.agentRows[agent], boxX); wallX++) {
                                    if (s.walls[wallX][s.agentCols[agent]]) {
                                        wallsInManhattanDistances1++;
                                    }
                                    if (s.walls[wallX][boxY]) {
                                        wallsInManhattanDistances2++;
                                    }
                                }
                                for (int wallY = Math.min(s.agentCols[agent], boxY); wallY < Math.max(s.agentCols[agent], boxY); wallY++) {
                                    if (s.walls[s.agentRows[agent]][wallY]) {
                                        wallsInManhattanDistances1++;
                                    }
                                    if (s.walls[boxX][wallY]) {
                                        wallsInManhattanDistances2++;
                                    }
                                }
                                distance += Math.min(wallsInManhattanDistances1, wallsInManhattanDistances2);
                                //NEW for multi-agent
                                if (distance < agentDistancesForColours[goalOpt.get().character - 'A'])
                                    agentDistancesForColours[goalOpt.get().character - 'A'] = distance;
                                if (distance < minimumAgentDistance)
                                    minimumAgentDistance = distance;
                                break;
                            }
                        }
                        //Remove one goal from current consideration if fulfilled
                        remainingGoals.remove(goalOpt.get());
                    }
                }
            }
        }
        //NEW for multi-agent
        return (int)
                (s.boxes.length * s.boxes[0].length *
                        (Arrays.stream(remainingGoalsPerColour).sum()
                                + Arrays.stream(remainingAgentGoalsPerColour).sum())
                        + Arrays.stream(agentDistancesForColours)
                        .filter(dist -> dist != Long.MAX_VALUE)
                        .min().orElse(0)
                        + Arrays.stream(manhattanDistancesPerColour)
                        .sum()
                        + 4 * Arrays.stream(wallsInManhattanDistancesPerColour)
                        .sum());
    }

    public abstract int f(State s);

    @Override
    public int compare(State s1, State s2) {
        return this.f(s1) - this.f(s2);
    }
}

class HeuristicAStar
        extends Heuristic {
    public HeuristicAStar(State initialState) {
        super(initialState);
    }

    @Override
    public int f(State s) {
        return s.g() + this.h_new(s);
    }

    @Override
    public String toString() {
        return "A* evaluation";
    }
}

class HeuristicWeightedAStar
        extends Heuristic {
    private int w;

    public HeuristicWeightedAStar(State initialState, int w) {
        super(initialState);
        this.w = w;
    }

    @Override
    public int f(State s) {
        return s.g() + this.w * this.h_new(s);
    }

    @Override
    public String toString() {
        return String.format("WA*(%d) evaluation", this.w);
    }
}

class HeuristicGreedy
        extends Heuristic {
    public HeuristicGreedy(State initialState) {
        super(initialState);
    }

    @Override
    public int f(State s) {
        return this.h_new(s);
    }

    @Override
    public String toString() {
        return "greedy evaluation";
    }
}

class Coordinates {
    public final int x;
    public final int y;
    public final char character;

    Coordinates(int x, int y, char character) {
        this.x = x;
        this.y = y;
        this.character = character;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Coordinates coords = (Coordinates) obj;
            return this.x == coords.x && this.y == coords.y && this.character == coords.character;
        } catch (Exception e) {
            return false;
        }
    }
}