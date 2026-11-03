package madoku.craft.java.quests;

import java.util.List;

/** Standard contract for one category's quest definitions. */
public interface QuestCategoryRegistrar {
	String categoryId();
	List<QuestDefinition> definitions();
}
