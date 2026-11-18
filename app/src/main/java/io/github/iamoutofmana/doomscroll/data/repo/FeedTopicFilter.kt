package io.github.iamoutofmana.doomscroll.data.repo

/**
 * Feed-level topic filter (v1.3): keeps a story group when the primary article or any of its
 * related coverage comes from a source in the selected topic. Pure and cheap to unit test.
 */
fun List<StoryClusterer.StoryGroup>.filterByTopic(
    sourceTopics: Map<Long, String>,
    topicId: String?,
): List<StoryClusterer.StoryGroup> {
    if (topicId == null) return this
    return filter { group ->
        (sequenceOf(group.primary) + group.related.asSequence())
            .any { sourceTopics[it.sourceId] == topicId }
    }
}
