package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.minecraft.util.ResourceLocation;

public final class PonderChapterRegistry {
    private final Map<ResourceLocation, PonderChapter> chapters = new LinkedHashMap<ResourceLocation, PonderChapter>();
    private final Map<ResourceLocation, List<StoryBoardEntry>> stories = new LinkedHashMap<ResourceLocation, List<StoryBoardEntry>>();

    public synchronized PonderChapter addChapter(PonderChapter chapter) {
        chapters.put(chapter.getId(), chapter);
        stories.computeIfAbsent(chapter.getId(), ignored -> new ArrayList<StoryBoardEntry>());
        return chapter;
    }

    public synchronized PonderChapter getChapter(ResourceLocation id) { return chapters.get(id); }

    public synchronized void addStoriesToChapter(PonderChapter chapter, StoryBoardEntry... entries) {
        if (!chapters.containsKey(chapter.getId())) addChapter(chapter);
        stories.get(chapter.getId()).addAll(Arrays.asList(entries));
    }

    public synchronized List<PonderChapter> getAllChapters() {
        return Collections.unmodifiableList(new ArrayList<PonderChapter>(chapters.values()));
    }

    public synchronized List<StoryBoardEntry> getStories(PonderChapter chapter) {
        List<StoryBoardEntry> result = stories.get(chapter.getId());
        return result == null ? Collections.<StoryBoardEntry>emptyList()
            : Collections.unmodifiableList(new ArrayList<StoryBoardEntry>(result));
    }
}
