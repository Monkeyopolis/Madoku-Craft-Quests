package madoku.craft.java.quests;

/** A registered quest assigned to a player at a selected difficulty. */
public record QuestAssignment(QuestDefinition definition, QuestDifficulty difficulty) {
	public QuestAssignment {
		if (definition == null) throw new IllegalArgumentException("Quest assignment definition must not be null.");
		if (difficulty == null) throw new IllegalArgumentException("Quest assignment difficulty must not be null.");
	}
}
