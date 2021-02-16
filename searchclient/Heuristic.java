package searchclient;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Heuristic
        implements Comparator<State> {
    HashMap<Coordinates, Character> goalSet = new HashMap<>();

    public Heuristic(State initialState) {
        for (int i = 0; i < State.goals.length; i++) {
            for (int j = 0; j < State.goals[0].length; j++) {
                if (State.goals[i][j] != 0) {
                    goalSet.put(new Coordinates(i, j), State.goals[i][j]);
                }
            }
        }
    }

    public int h(State s) {
        List<Map.Entry<Coordinates, Character>> remainingGoals = goalSet.entrySet().stream()
                .filter(entry -> s.boxes[entry.getKey().x][entry.getKey().y] != entry.getValue())
                .collect(Collectors.toList());
        long remainingGoalsSize = remainingGoals.size();

        long manhattanDistances = 0;
        long wallsInManhattanDistances = 0;
        long minimumAgentDistance = Long.MAX_VALUE;
        long totalAgentDistance = 0;
        if (remainingGoals.size() != 0) {
            for (int i = 0; i < s.boxes.length; i++) {
                for (int j = 0; j < s.boxes[0].length && !remainingGoals.isEmpty(); j++) {
                    if (s.boxes[i][j] != 0) {
                        //Look for any goal
                        int boxX = i;
                        int boxY = j;
                        Optional<Map.Entry<Coordinates, Character>> goalOpt = remainingGoals.stream()
                                .filter(entry -> entry.getValue() == s.boxes[boxX][boxY])
                                .findFirst();
                        if (goalOpt.isEmpty()) {
                            continue;
                        }
                        Coordinates goalCoords = goalOpt.get().getKey();
                        manhattanDistances += Math.abs(goalCoords.x - boxX) + Math.abs(goalCoords.y - boxY);
                        long wallsInManhattanDistances1 = 0, wallsInManhattanDistances2 = 0;
                        //Add walls in between (for simplicity, x first, then y)
                        for(int wallX = Math.min(goalCoords.x, boxX); wallX < Math.max(goalCoords.x, boxX); wallX++){
                            if(State.walls[wallX][goalCoords.y]){
                                wallsInManhattanDistances1++;
                            }
                            if(State.walls[wallX][boxY]){
                                wallsInManhattanDistances2++;
                            }
                        }
                        wallsInManhattanDistances += Math.min(wallsInManhattanDistances1, wallsInManhattanDistances2);
                        wallsInManhattanDistances1 = 0;
                        wallsInManhattanDistances2 = 0;
                        for(int wallY = Math.min(goalCoords.y, boxY); wallY < Math.max(goalCoords.y, boxY); wallY++){
                            if(State.walls[goalCoords.x][wallY]){
                                wallsInManhattanDistances1++;
                            }
                            if(State.walls[boxX][wallY]){
                                wallsInManhattanDistances2++;
                            }
                        }
                        wallsInManhattanDistances += Math.min(wallsInManhattanDistances1, wallsInManhattanDistances2);
                        //Look for any matching agent per box
                        for(int agent = 0; agent < s.agentCols.length; agent++) {
                            if(State.agentColors[agent] == State.boxColors[goalOpt.get().getValue() - 65]){
                                long distance = Math.abs(boxX - s.agentRows[agent]) + Math.abs(boxY - s.agentCols[agent]);
                                //Add walls in between (for simplicity, x first, then y)
                                for(int wallX = Math.min(s.agentRows[agent], boxX); wallX < Math.max(s.agentRows[agent], boxX); wallX++){
                                    if(State.walls[wallX][s.agentCols[agent]]){
                                        wallsInManhattanDistances1++;
                                    }
                                    if(State.walls[wallX][boxY]){
                                        wallsInManhattanDistances2++;
                                    }
                                }
                                distance += Math.min(wallsInManhattanDistances1, wallsInManhattanDistances2);
                                wallsInManhattanDistances1 = 0;
                                wallsInManhattanDistances2 = 0;
                                for(int wallY = Math.min(s.agentCols[agent], boxY); wallY < Math.max(s.agentCols[agent], boxY); wallY++){
                                    if(State.walls[s.agentRows[agent]][wallY]){
                                        wallsInManhattanDistances1++;
                                    }
                                    if(State.walls[boxX][wallY]){
                                        wallsInManhattanDistances2++;
                                    }
                                }
                                distance += Math.min(wallsInManhattanDistances1, wallsInManhattanDistances2);
                                totalAgentDistance += distance;
                                if (distance < minimumAgentDistance)
                                    minimumAgentDistance = distance;
                                break;
                            }
                        }

                        //Remove one goal from current consideration if fulfilled
                        for (ListIterator<Map.Entry<Coordinates, Character>> iter = remainingGoals.listIterator(); iter.hasNext(); ) {
                            Map.Entry<Coordinates, Character> a = iter.next();
                            if (a.getKey().equals(goalCoords)) {
                                iter.remove();
                                break;
                            }
                        }
                    }
                }
            }
        }

        return (int) (2*s.boxes.length*s.boxes[0].length*remainingGoalsSize + minimumAgentDistance + manhattanDistances + 4*wallsInManhattanDistances);
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
        return s.g() + this.h(s);
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
        return s.g() + this.w * this.h(s);
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
        return this.h(s);
    }

    @Override
    public String toString() {
        return "greedy evaluation";
    }
}

class Coordinates {
    public final int x;
    public final int y;

    Coordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Coordinates coords = (Coordinates) obj;
            return this.x == coords.x && this.y == coords.y;
        } catch (Exception e) {
            return false;
        }
    }
}