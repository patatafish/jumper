import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.math.*
import java.time.LocalDateTime

// needed for debug options at end of main
import com.soywiz.korio.async.*
import com.soywiz.korge.time.*
import kotlin.math.*


const val DEBUG_COLOR = "\u001b[33;1m"
const val RESET_COLOR = "\u001b[0m"


/**
 *
 * TODO LIST:
 * add bounce items
 * show direction of avatar (line for now?) maybe trailing dot
 * tile the background items
 * add further layers back
 * all leyers should use the floor/cieling methods that the space layer uses
 *
 * make sure delete zone is working (create a create/delete log?)
 */


suspend fun main() = Korge(
    width = 1280,
    height = 720,
    virtualWidth = 1280,
    virtualHeight = 720,
) {


    // create object for even handling
    val input = views.input

    // load fonts
    val menuFont = resourcesVfs["Freedom.ttf"].readTtfFont()
    val scoreFont = resourcesVfs["PocketCalculator.ttf"].readTtfFont()

    // create viewable area window for reference
    val window = solidRect(stage.actualVirtualWidth, stage.actualVirtualHeight)
    window.centerOnStage()
    window.alpha = 0.0

    // flag for creating game over menu items
    var menuShow = false

    // value for limit of atmosphere, or where blue sky will be at 0.0 alpha
    val atmosphereCeiling = 50_000.0

    val inertia = Inertia()
    val physics = Physics()

    val halfX = (stage.actualVirtualWidth / 2)
    val halfY = (stage.actualVirtualHeight / 2)
    debug("i think stage half is $halfX, $halfY")


    // create a collision object for scrolling item deletion. we place this item offstage to the left
    // and watch for collisions to delete objects.
    debug("creating collision object")
    val deleteZone = solidRect(width = 10.0, height = (stage.height * 1000.0))
    deleteZone.centerOnStage()
    deleteZone.x = -100.0   // for testing delete, to be commented out later
//    deleteZone.x = -(stage.width * 2.0)
    // keep delete zone centered no matter the altitude
    deleteZone.addUpdater { deleteZone.centerYOnStage() }
    stage.addChild(deleteZone)

    // create the background layer
    // TODO CAN YOU MAKE A STAGE? ANOTHER VIEW? FIND OUT
    val backDropContainer = container()
    // this image is the skybox behind the scrolling images in the background layer
    val skyBoxImage = resourcesVfs["iss_image.jpg"].readBitmap().toBMP32()
    val skyBox = image(skyBoxImage)
    skyBox.scale = 0.5
    skyBox.centerOnStage().apply {this.y = halfY.toDouble()}
    backDropContainer.addChild(skyBox)
    // this is the blue sky layer, to fade in and out (using alpha setting) as we go up and down
    val blueSky = solidRect(
        width = actualVirtualWidth * 1.2,
        height = actualVirtualHeight * 1.2,
        color = Colors.LIGHTBLUE,
    ).centerOnStage()
    backDropContainer.addChild(blueSky)
    // all other layers are added in front of the skybox and blue sky layer
    //City Skyline imaging
    val cityImage = resourcesVfs["city.png"].readBitmap().toBMP32()
    val skyline = image(cityImage)
    skyline.y = actualVirtualHeight - skyline.height
    backDropContainer.addChild(skyline)
    // layer to represent the ground at 0 elevation
    val groundLayer = solidRect(
        width = stage.width * 1.5,
        height = 400.0,
        color = Colors.LIGHTGREEN,
    )
    groundLayer.centerOnStage()
    groundLayer.y = (actualVirtualHeight - 25.0)
    backDropContainer.addChild(groundLayer)

    // create the avatar
    debug("creating the avatar")
    var launched = false
    var gameOver = false
    val avatar = circle(
        radius = 20.0,
        stroke = Colors.BLACK,
        strokeThickness = 2.0,
        fill = Colors.RED,
    )
    avatar.x = 0.0
    avatar.alignBottomToTopOf(groundLayer, 10)
    // variables to help locate avatar and control scene
    var avatarActualX = avatar.x
    var avatarActualY = avatar.y
    var altitude = 0.0
    stage.addChild(avatar)


    // create the score
    debug("creating the score")
    val scoreBox = text("0.00  meters", color = Colors.BLACK)
    scoreBox.addUpdater {
        text = "${(avatarActualX / 10).roundDecimalPlaces(2)} meters"
        scoreBox.alignTopToTopOf(window, padding = 10)
        scoreBox.alignRightToRightOf(window, padding = 10)
    }


    debug("adding the updater")
    addFixedUpdater(60.timesPerSecond) {
//    addFixedUpdater(stage.gameWindow.timePerFrame) {

        // find relative altitude of avatar
        altitude = avatarActualY.absoluteValue - actualVirtualHeight

        // we have launched, but it is not game over
        if (launched && !gameOver) {
            // we check to see if we are done, if run is negative that means we have
            // hit the ground and skidded to a stop, set all movement to 0 and tally score
            if (inertia.run <= 0) {
                debug("Round has ended, score ${avatarActualX.roundDecimalPlaces(2)}")
                inertia.rise = 0.0
                inertia.run = 0.0
                // set launched false to stop updating avatar
                launched = false
                // set flags to get game over menu
                gameOver = true
            }


            // we do the actual movement of the avatar here, later on we position it on screen
            avatarActualX += inertia.run
            avatarActualY -= inertia.rise


            // if the avatar has moved past mid-screen, we scroll the background, not move the avatar
            if (avatarActualX <= halfX) {
                // if the avatar hasn't moved yet to halfway across the screen, move the avatar
                avatar.x = avatarActualX
            } else {
                // else we want to scroll the background behind the avatar
                skyline.x -= (inertia.run * 0.9)
            }

            // if the avatar has moved past mid-screen, we move the background up or down
            when (avatarActualY) {
                // when we are within 1/2 window height, move the avatar
                in halfY.toDouble()..(stage.actualVirtualHeight * 2.0) -> {
                    avatar.y = avatarActualY
                    groundLayer.visible = true
                }
                // otherwise we keep the avatar static and move background
                else -> {
                    avatar.y = halfY.toDouble()
                    // move the background in a parallax manner
                    groundLayer.visible = false


                    skyline.y += (inertia.rise * 0.5)
                }
            }

            // move the space image
            skyBox.y = if (altitude < atmosphereCeiling) {
                halfY - (halfY * (altitude / atmosphereCeiling))
            } else {
                0.0
            }

            // adjusting the alpha of the blue sky layer to expose space behind it
            blueSky.alpha = 1.0 - (((2 * altitude) / atmosphereCeiling) - 1.0)


            // We adjust the inertia using the physics numbers here, after all movement is completed this frame
            if (inertia.rise >= physics.terminalVelocity) {
                inertia.rise += physics.gravity
            } else {
                inertia.rise = physics.terminalVelocity
            }
            inertia.run *= physics.windResist

            // landing
            if (avatar.collidesWith(groundLayer)) {
                // TODO CAN WE BOUNCE HERE? DEPENDS ON INCOMING SPEED
                // stop the falling
                inertia.rise = 0.0
                // if the avatar has fallen through the ground, reset them to ground level
                avatarActualY = groundLayer.y - avatar.radius
                // create friction with ground to slow the avatar
                if (inertia.run > 0) inertia.run -= physics.friction
            }
        } else if (!gameOver) { // we have not launched, but still not game over (before round starts)
            if (input.mouseButtons > 0) {
                debug("input, launching", "fixedUpdater")
                debug("mouse ${input.mouseButtons}", "fixedUpdater")
                /** HERE IS THE KICK VALUES
                 *8888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888
                 */
                avatar.alignBottomToTopOf(groundLayer, 10)
                inertia.rise += 270.0
                inertia.run += 10.0
                launched = true
            }
        }
        // game over actions
        if (gameOver && !menuShow) {
            debug("game over menu start")
            menuShow = true

            // define menu button sizes
            val buttonPadding = 35
            val buttonWidth = 175
            val buttonHeight = 100
            val buttonCorners = 10

            // show game over message and score
            // TODO: CAN WE BLUR BACKGROUND AND HAVE DATA OVER?
            debug("Creating game over screen")
            // container to hold game over screen
            val gameOverContainer = container()

            // background for visibility
            val darkerOverlay = solidRect(
                width = actualVirtualWidth.toDouble(),
                height = actualVirtualHeight.toDouble(),
                color = Colors.LIGHTGRAY
            )
            darkerOverlay.alpha = 0.8
            gameOverContainer.addChild(darkerOverlay)
            val gameOverBackground = solidRect(
                width = halfX.toDouble(),
                height = actualVirtualHeight.toDouble(),
                color = Colors.BLACK,
            )
            gameOverBackground.centerOnStage()
            gameOverBackground.alpha = 0.5
            gameOverContainer.addChild(gameOverBackground)

            // load game over message
            val gameOverText = text(
                text = "GAME OVER",
                textSize = 96.0,
                color = Colors.YELLOW,
                font = menuFont,
            )
            gameOverText.centerXOnStage()
            gameOverText.y = (halfY / 2.0)
            gameOverContainer.addChild(gameOverText)

            // load score
            // TODO: COUNT UP SCORE, THEN SHOW POINTS EARNED
            // TODO: SHOW HIGH SCORE, BONUS PTS
            val gameOverScore = text(
                text = "${(avatarActualX / 10).roundDecimalPlaces(2)} meters",
                textSize = 48.0,
                color = Colors.YELLOW,
                font = scoreFont,
            )
            gameOverScore.centerXOnStage()
            gameOverScore.alignTopToBottomOf(gameOverText)
            gameOverContainer.addChild(gameOverScore)

            // show end of game options
            val replayButton = roundRect(
                width = buttonWidth,
                height = buttonHeight,
                rx = buttonCorners,
                fill = Colors.BLACK,
                stroke = Colors.WHITE,
                strokeThickness = 2.0,
            )
            replayButton.centerOnStage()
            replayButton.alignTopToBottomOf(gameOverScore, padding = buttonPadding)
            val replayText = text(
                text = "Replay",
                textSize = 24.0,
                color = Colors.WHITE,
                font = menuFont,
            )
            replayText.centerOn(replayButton)
            gameOverContainer.addChild(replayButton)
            gameOverContainer.addChild(replayText)

            val upgradeButton = roundRect(
                width = buttonWidth,
                height = buttonHeight,
                rx = buttonCorners,
                fill = Colors.BLACK,
                stroke = Colors.WHITE,
                strokeThickness = 2.0,
            )
            upgradeButton.alignTopToTopOf(replayButton)
            upgradeButton.alignRightToLeftOf(replayButton, padding = buttonPadding)
            val upgradeText = text(
                text = "upgrade",
                textSize = 24.0,
                color = Colors.WHITE,
                font = menuFont,
            )
            upgradeText.centerOn(upgradeButton)
            gameOverContainer.addChild(upgradeButton)
            gameOverContainer.addChild(upgradeText)

            val quitButton = roundRect(
                width = buttonWidth,
                height = buttonHeight,
                rx = buttonCorners,
                fill = Colors.BLACK,
                stroke = Colors.WHITE,
                strokeThickness = 2.0,
            )
            quitButton.alignTopToTopOf(replayButton)
            quitButton.alignLeftToRightOf(replayButton, padding = buttonPadding)
            val quitText = text(
                text = "exit game",
                textSize = 24.0,
                color = Colors.WHITE,
                font = menuFont,
            )
            quitText.centerOn(quitButton)
            gameOverContainer.addChild(quitButton)
            gameOverContainer.addChild(quitText)

            // listen for mouse input
            debug("listening for menu choice")
            gameOverContainer.addUpdater {
                if (input.mouseButtons == 1) {
                    debug("mouse click in menu", "quit menu")
                    if (quitButton.mouse.isOver) {
                        debug("quit clicked", "quit menu")
                        stage?.removeChildren()
                        this@Korge.gameWindow.close()
                    }
                    if (upgradeButton.mouse.isOver) {
                        debug("upgrade clicked", "quit menu")
                    }
                    if (replayButton.mouse.isOver) {
                        debug("replay clicked", "quit menu")
                        debug("resetting game...")

                        debug("removing game over screen...")
                        gameOverContainer.removeChildren()
                        gameOverContainer.parent?.removeChild(gameOverContainer)
                        menuShow = false

                        debug("reset inertia values")
                        inertia.rise = 0.0
                        inertia.run = 0.0

                        debug("resetting the background")
                        skyBox.y = halfY.toDouble()
                        skyline.y = actualVirtualHeight - skyline.height
                        skyline.x = 0.0
                        groundLayer.visible = true

                        debug("resetting the avatar")
                        launched = false
                        gameOver = false
                        avatar.x = 0.0
                        avatar.alignBottomToTopOf(groundLayer, 10)
                        avatarActualX = avatar.x
                        avatarActualY = avatar.y

                        debug("empty mouse input")
                        input.mouseButtons = 0
                    }
                }
            }
        }
    }


    // avatar info debug
    text("", color = Colors.BLACK) {
        val text = this
        launchImmediately {
            text.x = 5.0
            text.y = 5.0
            text.color = Colors.YELLOW
            debug("avatar info box launched")
            while (true) {
                text.text =
                    "XY: ${avatarActualX.roundDecimalPlaces(2)}, ${(avatarActualY.roundDecimalPlaces(2))}, ${altitude.roundDecimalPlaces(2)}\nInertia: ${inertia.rise.roundDecimalPlaces(2)}, ${inertia.run.roundDecimalPlaces(2)}"
                delay(stage?.gameWindow?.timePerFrame ?: 1.milliseconds)
            }
        }
    }

    // MOUSE TRACKING BLOCK, CAN MARK OUT LATER
    // NEEDS TO COME LAST TO BE RENDERED ON TOP LAYER
    text("$mouseXY", color = Colors.BLACK) {
        val text = this
        text.color = Colors.YELLOW
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






fun debug(message: String, name: String = "UNKNOWN") {
    val now = LocalDateTime.now()
    println(DEBUG_COLOR + "[DEBUG] - $now - $name - ${message.uppercase()}" + RESET_COLOR)
}
