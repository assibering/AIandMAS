package searchclient;

class PlanningResult {
    int agent = -1;
    int step = -1;
    char cause = '\0';
    PlanningResultType type = PlanningResultType.NO_CONFLICT;

    enum PlanningResultType {
        NO_CONFLICT,
        WITH_CONFLICT
    }
}
