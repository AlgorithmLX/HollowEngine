package ru.hollowhorizon.hollowengine.dialogues.executors

import net.minecraft.entity.player.PlayerEntity
import ru.hollowhorizon.hollowengine.dialogues.DialogueScene
import ru.hollowhorizon.hollowengine.dialogues.IDialogueExecutor
import ru.hollowhorizon.hollowengine.story.waitForgeEvent

class ServerDialogueExecutor(val player: PlayerEntity) : IDialogueExecutor {
    init {
        OpenScreenPacket().send(0, player)
    }

    override fun waitAction() {
        WaitActionPacketS2C().send(0, player)
        waitForgeEvent<WaitActionEvent> { it.player == player }
    }

    override fun updateScene(scene: DialogueScene) {
        UpdateScenePacket().send(scene, player)
        scene.actions.clear() //Необходимо очищать действия, иначе они будут накапливаться
    }

    override fun applyChoice(choices: Collection<String>): Int {
        ApplyChoicePacketS2C().send(ChoicesContainer(choices), player)
        var choice = 0
        waitForgeEvent<ApplyChoiceEvent> {
            choice = it.choice
            it.player == player
        }
        return choice
    }

    override fun stop() {
        CloseScreenPacket().send(0, player)
    }
}