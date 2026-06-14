package cn.edu.whut.sept.dungeon.quest;

public final class QuestState {
    private final boolean reportIssued;
    private final boolean slidesExported;
    private final boolean passIssued;
    private final boolean mavenPuzzleSolved;
    private final boolean completed;

    public QuestState(boolean reportIssued, boolean slidesExported, boolean passIssued,
                      boolean mavenPuzzleSolved, boolean completed) {
        this.reportIssued = reportIssued;
        this.slidesExported = slidesExported;
        this.passIssued = passIssued;
        this.mavenPuzzleSolved = mavenPuzzleSolved;
        this.completed = completed;
    }

    public static QuestState initial() {
        return new QuestState(false, false, false, false, false);
    }

    public boolean isReportIssued() {
        return reportIssued;
    }

    public boolean isSlidesExported() {
        return slidesExported;
    }

    public boolean isPassIssued() {
        return passIssued;
    }

    public boolean isMavenPuzzleSolved() {
        return mavenPuzzleSolved;
    }

    public boolean isCompleted() {
        return completed;
    }

    public QuestState withReportIssued() {
        return new QuestState(true, slidesExported, passIssued, mavenPuzzleSolved, completed);
    }

    public QuestState withSlidesExported() {
        return new QuestState(reportIssued, true, passIssued, mavenPuzzleSolved, completed);
    }

    public QuestState withPassIssued() {
        return new QuestState(reportIssued, slidesExported, true, mavenPuzzleSolved, completed);
    }

    public QuestState withMavenPuzzleSolved() {
        return new QuestState(reportIssued, slidesExported, passIssued, true, completed);
    }

    public QuestState withCompleted() {
        return new QuestState(reportIssued, slidesExported, passIssued, mavenPuzzleSolved, true);
    }
}
