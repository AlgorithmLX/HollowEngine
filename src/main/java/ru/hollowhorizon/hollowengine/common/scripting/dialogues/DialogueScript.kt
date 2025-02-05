package ru.hollowhorizon.hollowengine.common.scripting.dialogues


import net.minecraft.entity.player.PlayerEntity
import ru.hollowhorizon.hc.common.scripting.kotlin.AbstractHollowScriptConfiguration
import ru.hollowhorizon.hollowengine.common.scripting.dialogues.executors.ServerDialogueExecutor
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.defaultImports

@KotlinScript(
    displayName = "Hollow Dialogue Script",
    fileExtension = "hds",
    compilationConfiguration = HollowDialogueConfiguration::class
)
abstract class HollowDialogue(player: PlayerEntity) :
    DialogueScriptBaseV2(ServerDialogueExecutor(player)) {

}

class HollowDialogueConfiguration : AbstractHollowScriptConfiguration({
    defaultImports("ru.hollowhorizon.hollowengine.dialogues.*")
    baseClass(DialogueScriptBaseV2::class)
})