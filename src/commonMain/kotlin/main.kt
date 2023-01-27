import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.math.*
import java.time.LocalDateTime


const val DEBUG_COLOR = "\u001b[33;1m"
const val RESET_COLOR = "\u001b[0m"

suspend fun main() = Korge(
    width = 1280,
    height = 720,
    virtualWidth = 1280,
    virtualHeight = 720,
) {

    // create object for even handling
    val input = views.input

    // create viewable area window
    val window = solidRect(stage.actualVirtualWidth, stage.actualVirtualHeight)
    window.centerOnStage()
    window.alpha = 0.0


    val inertia = Inertia()
    val physics = Physics()

    val halfX = (stage.actualVirtualWidth / 2)
    val halfY = (stage.actualVirtualHeight / 2)
    debug("i think stage half is $halfX, $halfY")


    // create a collision object for scrolling item deletion. we place this item offstage to the left
    // and watch for collisions to delete objects.
    debug("creating collision object")
    val deleteZone = solidRect(width = 10.0, height = (stage.height * 2.0), color = Colors.RED)
    deleteZone.centerOnStage()
    deleteZone.x = -100.0
//    deleteZone.x = -(stage.width * 2.0)
    stage.addChild(deleteZone)

    // create the background layer
    // TODO CAN YOU MAKE A STAGE? ANOTHER VIEW? FIND OUT
    val backDropContainer = mutableListOf<Any>()
    // backDrop is the skybox behind the scrolling images in the background layer
    val skyBox = solidRect(width = stage.width * 2, height = stage.height * 2, color = Colors.WHITE)
    backDropContainer.add(skyBox)
    // all other layers are added in front of the skybox
    val cityImage = resourcesVfs["city.png"].readBitmap().toBMP32()
    val skyline = image(cityImage)
    skyline.y = stage.height - skyline.height
    backDropContainer.add(skyline)
    val groundLayer = solidRect(width = stage.width * 1.5, height = 50.0, color = Colors.LIGHTGREEN)
    groundLayer.centerOnStage()
    groundLayer.y = (stage.height - (groundLayer.height / 2))
    backDropContainer.add(groundLayer)

    for (item in backDropContainer) stage.addChild(item as View)


    // create the avatar
    debug("creating the avatar")
    var launched = false
    val avatar = circle(
        radius = 20.0,
        stroke = Colors.BLACK,
        strokeThickness = 2.0,
        fill = Colors.RED,
    )
    var avatarActualX = avatar.x
    var avatarActualY = avatar.y
    avatar.x = 0.0
    avatar.y = (stage.height - (avatar.radius * 2.0))
    stage.addChild(avatar)


    // create the score
    debug("creating the score")
    var distance = 0.0
    val scoreBox = text("0.00  meters", color = Colors.BLACK)
    scoreBox.addUpdater {
        text = "${distance.roundDecimalPlaces(2)} meters"
        scoreBox.alignTopToTopOf(window, padding = 10)
        scoreBox.alignRightToRightOf(window, padding = 10)
    }

    debug("adding the updater")
    addFixedUpdater(stage.gameWindow.timePerFrame) {
        if (launched) {
            // we check to see if we are done, if run is negative that means we have
            // hit the ground and skidded to a stop, set all movement to 0 and tally score
            if (inertia.run < 0) {
                debug("Round has ended, score ${distance.roundDecimalPlaces(2)}")
                inertia.rise = 0.0
                inertia.run = 0.0
            }


            // we do the actual movement of the avatar here, later on we position it on screen
            avatarActualX += inertia.run
            avatarActualY -= inertia.rise


            // TODO: CHECK MOVEMENT HERE FOR RISE AND RUN, KEEP THE AVATAR CENTERED
            // if the avatar has moved past mid-screen, we scroll the background, not move the avatar
            if (avatar.x < halfX) {
                debug("nothing", "avatar run")
            } else {
                debug("nothing", "screen run")
            }

            // TODO: WE NEED TO SCROLL THE BACKGROUND UP AND DOWN WITH THE RISE OF THE AVATAR
            // if the avatar has moved past mid-screen, we move the background up or down
            if (avatar.y < halfY) {
                debug("nothing", "avatar rise")
            } else {
                debug("nothing", "screen rise")
            }

            // count score
            distance += inertia.run / 10

            // We adjust the inertia using the physics numbers here, after all movement is completed this frame
            inertia.rise += physics.gravity
            inertia.run *= physics.windResist

            // landing
            if (avatar.collidesWith(groundLayer)) {
                // TODO CAN WE BOUNCE HERE? DEPENDS ON INCOMING SPEED
                inertia.rise = 0.0
                if (inertia.run > 0) inertia.run -= physics.friction
            }
        } else {
            if (input.mouseButtons > 0) {
                debug("input, launching", "fixedUpdater")
                /** HERE IS THE KICK VALUES
                 *
                 */
                inertia.rise += 90.0
                inertia.run += 10.0
                launched = true
            }
        }
    }

    // avatar info debug
    text("", color = Colors.BLACK) {
        val text = this
        launchImmediately {
            text.x = 5.0
            text.y = 5.0
            debug("avatar info box launched")
            while (true) {
                text.text =
                    "XY: ${avatar.x + avatar.radius}, ${avatar.y + avatar.radius} \nInertia: ${inertia.rise}, ${inertia.run}"
                delay(stage?.gameWindow?.timePerFrame ?: 1.milliseconds)
            }
        }
    }

    // MOUSE TRACKING BLOCK, CAN MARK OUT LATER
    // NEEDS TO COME LAST TO BE RENDERED ON TOP LAYER
    text("$mouseXY", color = Colors.BLACK) {
        val text = this
        launchImmediately {
            debug("mouse tracker launched", "mouseTracker")
            while (true) {
                text.text = "$mouseXY"
                text.x = mouseX + 10
                text.y = mouseY + 10
                delay(stage?.gameWindow?.timePerFrame ?: 1.milliseconds)
            }
        }
    }
}


data class Inertia(
    var rise: Double = 0.0,
    var run: Double = 0.0,
)

/**
 * physics holds items we use to calculate movement changes.
 * gravity is a constant that we expect to be subtracted from vertical movement (inertia.rise)
 * windResist is a constant that we expect to be multiplied against horizontal movement (inertia.run)
 * friction is a constant that we expect to be subtracted from horizontal movement on the ground (inertia.run)
 */
data class Physics(
    val gravity: Double = -0.8,
    val windResist: Double = 0.9999,
    val friction: Double = 1.0,
)

fun debug(message: String, name: String = "UNKNOWN") {
    val now = LocalDateTime.now()
    println(DEBUG_COLOR + "[DEBUG] - $now - $name - ${message.uppercase()}" + RESET_COLOR)
}
