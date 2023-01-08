package ru.hollowhorizon.hollowstory.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.entity.LivingEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.vector.Vector3f
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import ru.hollowhorizon.hc.client.screens.HollowScreen
import ru.hollowhorizon.hc.client.screens.util.Alignment
import ru.hollowhorizon.hc.client.screens.util.WidgetPlacement
import ru.hollowhorizon.hc.client.screens.widget.button.BaseButton
import ru.hollowhorizon.hc.client.utils.*
import ru.hollowhorizon.hollowstory.client.gui.widget.dialogue.DialogueTextBox
import ru.hollowhorizon.hollowstory.common.hollowscript.dialogues.DialogueExecutorThread
import ru.hollowhorizon.hollowstory.dialogues.HDImage
import ru.hollowhorizon.hollowstory.dialogues.HDScene
import kotlin.math.atan

class DialogueScreen(location: ResourceLocation, val onCloseCallback: ()->Unit = {}) : HollowScreen(StringTextComponent("")) {
    var background: String? = null
    private var imageToRemove: HDImage? = null
    private var imageToAdd: HDImage? = null
    private val images: ArrayList<HDImage> = ArrayList()
    val scene = HDScene(this)
    private val clickWaiter = Object()
    private val animAddCharacter = Object()
    private val animRemoveCharacter = Object()
    private val animDelay = Object()
    private val choiceWaiter = Object()
    private val imageAnimAddWaiter = Object()
    private val imageAnimRemoveWaiter = Object()
    var textBox: DialogueTextBox? = null
    private var choices: Map<String, DialogueChoice> = emptyMap()
    private var animAddCounter = 0
    private var animRemoveCounter = 0
    var currentName: ITextComponent = StringTextComponent("")
    var crystalAnimator = GuiAnimator.Looped(0, 20, 1.5F) { it }
    private var lastCount = 0
    private var delayTicks = -1
    var shouldClose = false
    var color: Int = 0xFFFFFFFF.toInt()
    var STATUS_ICON = "hollowstory:textures/gui/dialogues/status.png"
    var OVERLAY = "hollowstory:textures/gui/dialogues/overlay.png"
    var NAME_OVERLAY = "hollowstory:textures/gui/dialogues/name_overlay.png"
    var CHOICE_BUTTON = "hollowstory:textures/gui/dialogues/choice_button.png"

    init {
        val name =
            "${location.namespace}/${location.path.substring(0, location.path.length - ".hsd.kts".length)}".replace(
                "/", "."
            )
        DialogueExecutorThread(this, name, location.toIS().reader().readText()).start()
    }


    override fun init() {
        this.children.clear()
        this.buttons.clear()

        this.textBox = this.addButton(
            WidgetPlacement.configureWidget(
                ::DialogueTextBox, Alignment.BOTTOM_CENTER, 0, 0, this.width, this.height, 300, 50
            )
        )

        for ((i, choice) in choices.keys.withIndex()) {

            this.addButton(
                WidgetPlacement.configureWidget(
                    { x, y, w, h ->
                        BaseButton(x, y, w, h, StringTextComponent(choice), { button ->
                            val c = choices[choice]!!
                            this.choices = emptyMap()
                            init()

                            if (c.action != null) {
                                Thread {
                                    waitClick()
                                    notifyClick()
                                    notifyClick()
                                    c.action!!.invoke()
                                }.start()
                            }


                        }, CHOICE_BUTTON.toRL(), textColor = 0xFFFFFF, textColorHovered = 0xEDC213)
                    }, Alignment.CENTER, 0, this.height / 3 - 25 * i, this.width, this.height, 320, 20
                )
            )
        }
    }

    @Suppress("DEPRECATION")
    override fun render(stack: MatrixStack, mouseX: Int, mouseY: Int, partialTick: Float) {
        val col = color.toRGBA()

        renderBackground(stack)
        if (background != null) {
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F)
            Minecraft.getInstance().textureManager.bind(background!!.toRL())
            blit(stack, 0, 0, 0F, 0F, this.width, this.height, this.width, this.height)
        }
        drawCharacters(mouseX, mouseY)
        drawImages(stack)

        RenderSystem.color4f(col.r, col.g, col.b, col.a)
        drawStatus(stack)
        RenderSystem.color4f(1f, 1f, 1f, 1f)

        RenderSystem.enableAlphaTest()
        RenderSystem.enableBlend()
        RenderSystem.defaultAlphaFunc()
        RenderSystem.defaultBlendFunc()
        RenderSystem.color4f(col.r, col.g, col.b, col.a)
        Minecraft.getInstance().textureManager.bind(OVERLAY.toRL())
        blit(stack, 0, this.height - 55, 0F, 0F, this.width, 55, this.width, 55)
        RenderSystem.color4f(1F, 1F, 1F, 1F)

        super.render(stack, mouseX, mouseY, partialTick)

        if(!this.currentName.string.isEmpty()) drawNameBox(stack, col)

        if (shouldClose) onClose()

        if (delayTicks > 0) delayTicks--
        else if (delayTicks == 0) {
            synchronized(animDelay) {
                animDelay.notify()
            }
            delayTicks = -1
        }
    }

    private fun drawNameBox(stack: MatrixStack, col: RGBA) {
        RenderSystem.enableAlphaTest()
        RenderSystem.enableBlend()
        RenderSystem.defaultAlphaFunc()
        RenderSystem.defaultBlendFunc()
        RenderSystem.color4f(col.r, col.g, col.b, col.a)
        Minecraft.getInstance().textureManager.bind(NAME_OVERLAY.toRL())
        blit(
            stack,
            5,
            this.height - 73,
            0F,
            0F,
            this.font.width(this.currentName) + 10,
            15,
            this.font.width(this.currentName) + 10,
            15
        )
        RenderSystem.color4f(1F, 1F, 1F, 1F)

        this.font.drawShadow(
            stack, this.currentName, 10F, this.height - 60F - font.lineHeight, 0xFFFFFF
        )
    }

    private fun drawStatus(stack: MatrixStack) {
        if (this.textBox?.isComplete == true) {
            Minecraft.getInstance().textureManager.bind(STATUS_ICON.toRL())
            blit(stack, this.width - 55 + crystalAnimator.value, this.height - 47, 0F, 0F, 40, 40, 40, 40)
            crystalAnimator.update()
        }
    }

    private fun drawImages(stack: MatrixStack) {
        for (image in images) {
            if (image == imageToAdd) {
                if (!image.animate) synchronized(imageAnimAddWaiter) {
                    imageAnimAddWaiter.notify()
                }

                image.alpha += 0.07F

                if (image.alpha >= 1F) {
                    image.alpha = 1F
                    synchronized(imageAnimAddWaiter) {
                        imageAnimAddWaiter.notify()
                    }
                }
            }

            image.render(stack, width, height)

            if (image == imageToRemove) {
                if (!image.animate) synchronized(imageAnimRemoveWaiter) {
                    imageAnimRemoveWaiter.notify()
                }
                image.alpha -= 0.07F
                if (image.alpha <= 0F) {
                    image.alpha = 0F
                    synchronized(imageAnimRemoveWaiter) {
                        imageAnimRemoveWaiter.notify()
                    }
                }
            }
        }
    }

    private fun drawCharacters(mouseX: Int, mouseY: Int) {
        val count = scene.characters.size
        val w = this.width / (count + 1)

        for (i in 0 until count) {
            val character = scene.characters[i]

            var x = (i + 1F) * w
            var y = this.height * 0.85F

            if (character == scene.characterToAdd) {
                if (animAddCounter < 100) {
                    y += 100 - animAddCounter
                    animAddCounter += 4
                } else {
                    synchronized(animAddCharacter) {
                        animAddCharacter.notify()
                    }
                }
            } else if (character == scene.characterToRemove) {
                if (animRemoveCounter < 100) {
                    y += animRemoveCounter
                    animRemoveCounter += 4
                } else {
                    synchronized(animRemoveCharacter) {
                        animRemoveCharacter.notify()
                    }
                    break
                }
            }

            var scale = if (scene.currentCharacter == character) 75F else 70F
            scale *= character.scale
            x += character.translate.x
            y += character.translate.y
            val brightness = if (scene.currentCharacter == character) 1F else 0.6F
            drawEntity(
                x,
                y,
                scale,
                x - mouseX - character.rotate.x,
                y - mouseY - character.rotate.y - character.entity.bbHeight / 2,
                character.entity,
                brightness
            )
        }

        lastCount = count
    }

    override fun mouseClicked(p_231044_1_: Double, p_231044_3_: Double, p_231044_5_: Int): Boolean {
        if (this.textBox?.isComplete == true) notifyClick()
        else this.textBox?.complete()
        return super.mouseClicked(p_231044_1_, p_231044_3_, p_231044_5_)
    }

    fun notifyClick() {
        synchronized(this.clickWaiter) {
            this.clickWaiter.notify()
        }
    }

    fun waitClick() {
        synchronized(this.clickWaiter) {
            this.clickWaiter.wait()
        }
    }

    fun waitAddAnim() {
        synchronized(this.animAddCharacter) {
            this.animAddCharacter.wait()
            animAddCounter = 0
            scene.characterToAdd = null
        }
    }

    fun waitRemoveAnim() {
        synchronized(this.animRemoveCharacter) {
            this.animRemoveCharacter.wait()
            animRemoveCounter = 0
            scene.characters.remove(scene.characterToRemove)
            scene.characterToRemove = null
        }
    }

    @Suppress("DEPRECATION")
    private fun drawEntity(
        x: Float, y: Float, scale: Float, xRot: Float, yRot: Float, entity: LivingEntity, brightness: Float = 0.8F,
    ) {
        val f = atan((xRot / 40.0)).toFloat()
        val f1 = atan((yRot / 40.0)).toFloat()

        RenderSystem.pushMatrix()
        RenderSystem.translatef(x, y, 150.0f)
        RenderSystem.scalef(1.0f, 1.0f, -1.0f)

        val matrixstack = MatrixStack()

        matrixstack.translate(0.0, 0.0, 1000.0)
        matrixstack.scale(scale, scale, scale)
        val quaternion = Vector3f.ZP.rotationDegrees(180.0f)
        val quaternion1 = Vector3f.XP.rotationDegrees(f1 * 20.0f)
        quaternion.mul(quaternion1)
        matrixstack.mulPose(quaternion)
        val f2: Float = entity.yBodyRot
        val f3: Float = entity.yRot
        val f4: Float = entity.xRot
        val f5: Float = entity.yHeadRotO
        val f6: Float = entity.yHeadRot
        entity.yBodyRot = 180.0f + f * 20.0f
        entity.yRot = 180.0f + f * 40.0f
        entity.xRot = -f1 * 20.0f
        entity.yHeadRot = entity.yRot
        entity.yHeadRotO = entity.yRot
        val entityrenderermanager = Minecraft.getInstance().entityRenderDispatcher
        quaternion1.conj()
        entityrenderermanager.overrideCameraOrientation(quaternion1)
        entityrenderermanager.setRenderShadow(false)
        val renderBuffer = Minecraft.getInstance().renderBuffers().bufferSource()

        RenderSystem.runAsFancy {
            RenderSystem.enableAlphaTest()
            RenderSystem.enableBlend()
            RenderSystem.defaultAlphaFunc()
            RenderSystem.defaultBlendFunc()
            RenderSystem.color4f(0f, 0f, 0f, 0.5f)
            entityrenderermanager.render(
                entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, matrixstack, renderBuffer, 15728880
            )
        }

        renderBuffer.endBatch()
        entityrenderermanager.setRenderShadow(true)
        entity.yBodyRot = f2
        entity.yRot = f3
        entity.xRot = f4
        entity.yHeadRotO = f5
        entity.yHeadRot = f6

        RenderSystem.popMatrix()
    }

    fun delay(ticks: Int) {
        this.delayTicks = ticks
        synchronized(animDelay) {
            animDelay.wait()
        }
    }

    fun createChoice(pairs: Array<out Pair<String, () -> Unit>>) {
        val map = mutableMapOf<String, DialogueChoice>()
        for (pair in pairs) {
            val choice = DialogueChoice()
            choice.action = {
                pair.second.invoke()

                synchronized(choiceWaiter) {
                    choiceWaiter.notify()
                }
            }
            map[pair.first] = choice
        }
        this.choices = map

        val text = this.textBox?.text ?: "Загрзка..."
        init()
        this.textBox?.text = text
        this.textBox?.complete()
        synchronized(this.choiceWaiter) {
            this.choiceWaiter.wait()
        }
    }

    fun addImage(image: HDImage) {
        image.alpha = 0F
        this.imageToAdd = image
        this.images.add(image)

        synchronized(imageAnimAddWaiter) {
            imageAnimAddWaiter.wait()
        }

        this.imageToAdd = null
    }

    override fun onClose() {
        onCloseCallback.invoke()
        super.onClose()
    }

    fun removeImage(image: HDImage) {
        this.imageToRemove = image

        synchronized(this.imageAnimRemoveWaiter) {
            this.imageAnimRemoveWaiter.wait()
        }

        this.images.remove(image)
    }

    fun waitFocusAnim() {

    }
}

class DialogueChoice {
    val waiter = Object()
    var action: (() -> Unit)? = null
}