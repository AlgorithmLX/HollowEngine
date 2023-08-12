package ru.hollowhorizon.hollowengine.common.scripting.story

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class StoryVariable(val name: String, @Contextual var value: Any?)