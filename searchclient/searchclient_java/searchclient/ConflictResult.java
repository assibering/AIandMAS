package searchclient;

public class ConflictResult {
    private final boolean isConflict;
    int agent1;
    int agent2;

    public static ConflictResult noConflict() {
        return new ConflictResult();
    }

    private ConflictResult() {
        this.isConflict = false;
    }

    public static ConflictResult thereIsConflict(int agent1, int agent2) {
        return new ConflictResult(agent1, agent2);
    }

    private ConflictResult(int agent1, int agent2) {
        this.isConflict = true;
        this.agent1 = agent1;
        this.agent2 = agent2;
    }

    public boolean isConflict() {
        return this.isConflict;
    }
}