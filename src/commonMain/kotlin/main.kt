import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.Circle
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import java.time.LocalDateTime


const val DEBUG_COLOR = "\u001b[33;1m"
const val RESET_COLOR = "\u001b[0m"

suspend fun main() = Korge(
    width = 1280,
    height = 720,
    virtualWidth = 1280,
    virtualHeight = 720,
) {

    val backDrop = solidRect(width = 1280, height = 720, color = Colors.WHITE)
    stage.addChild(backDrop)

    val deleteZone = solidRect(width = 10.0, height = 50.0, color = Colors.RED)
    deleteZone.centerOnStage()
    deleteZone.x = -(stage.width * 2.0)
    stage.addChild(deleteZone)


    val cityImage = resourcesVfs["city.png"].readBitmap().toBMP32()
    val skyline = image(cityImage)
    skyline.y = stage.height - skyline.height
    stage.addChild(skyline)

    val input = views.input

    // MOUSE TRACKING BLOCK, CAN MARK OUT LATER
    text("$mouseXY", color = Colors.BLACK) {
        debug("mouse tracker launched", "mouseTracker")
        val text = this
        launchImmediately {
            while (true) {
                text.text = "$mouseXY"
                text.x = mouseX + 10
                text.y = mouseY + 10
                delay(1.milliseconds)
            }
        }
    }

    // create the avatar on the stage, pass input for listeners
    val myAvatar = Avatar(stage, input)


}


class Avatar(stage: Stage, input: Input) : Circle() {

    private var rise: Float = 30.0F // vertical movement
    private var run: Float = 30.0F  // horizontal movement
    private var launched: Boolean = false // have we launched
    private var halfX = (stage.width / 2).toInt()
    private var halfY = (stage.height / 2).toInt()

    // TODO MAKE GRAVITY AND AIR RESISTANCE CONSTANTS HERE FUNCTION MAYBE?

    init {
        debug("creating avatar", "avatar.init()")
        radius = 20.0
        stroke = Colors.BLACK
        strokeThickness = 2.0
        fill = Colors.RED
        x = 0.0
        y = stage.height - (radius * 2)

        stage.addChild(this)


        debug("adding avatar updater", "avatar.init()")
        addFixedUpdater(60.timesPerSecond) {

            if (launched) {
                moveAvatar()
                gravity()
                airDrag()

                checkFloor()
            } else {
                showLaunchLine()
                if (input.mouseButtons > 0) {
                    debug("input, launching", "avatarUpdater")
                    launched = true
                }

            }
        }
    }

    private fun showLaunchLine() {
        // TODO IMPLEMENT THE LINE TO SHOW LAUNCH ANGLE
    }

    private fun checkFloor() {
        // TODO Do I run into the floor here?
    }

    private fun moveAvatar() {
        if (x > halfX) {

        } else {
            x += run
            y -= rise
        }
    }

    private fun gravity() {
        rise -= 0.5F
    }

    private fun airDrag() {
        run -= when {
            run > 40 -> 1.5F
            run > 20 -> 0.5F
            run > 3 -> 0.25F
            else -> 0F
        }
    }
}


fun debug(message: String, name: String = "UNKNOWN") {
    val now = LocalDateTime.now()
    println(DEBUG_COLOR + "[DEBUG] - $now - $name - ${message.uppercase()}" + RESET_COLOR)
}
