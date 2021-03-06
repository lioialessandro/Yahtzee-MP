package me.lioironzello.yahtzee.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.github.skgmn.composetooltip.AnchorEdge
import com.github.skgmn.composetooltip.Tooltip
import com.github.skgmn.composetooltip.rememberTooltipStyle
import com.google.ar.sceneform.rendering.RenderableInstance
import com.google.gson.Gson
import io.github.sceneview.SceneView
import io.github.sceneview.material.setBaseColor
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.utils.colorOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.lioironzello.yahtzee.R
import me.lioironzello.yahtzee.database.GameDatabase
import me.lioironzello.yahtzee.database.GameRepository
import me.lioironzello.yahtzee.model.*
import me.lioironzello.yahtzee.model.DiceModel.Companion.Values3D
import java.io.File
import java.nio.charset.Charset
import kotlin.random.Random

// Function used to load the page
@ExperimentalAnimationApi
@Composable
fun PlayLayout(settingsModel: SettingsModel, numberOfPlayers: Int, continueGame: Boolean) {
    // 3d model
    var referenceModel by remember { mutableStateOf<RenderableInstance?>(null) }
    // 2d images created at runtime
    var faces = listOf<Bitmap>()

    val context = LocalContext.current

    // Loading 3D model only on slow dice velocity
    if (settingsModel.diceVelocity == DiceVelocity.Slow) {
        val lifecycleOwner = LocalLifecycleOwner.current

        // Code from https://github.com/SceneView/sceneview-android/blob/main/samples/model-viewer/src/main/java/io/github/sceneview/sample/modelviewer/MainFragment.kt#L48
        lifecycleOwner.lifecycleScope.launchWhenCreated {
            if (referenceModel == null) {
                referenceModel = ModelNode().loadModel(
                    context = context,
                    lifecycle = lifecycleOwner.lifecycle,
                    glbFileLocation = "cube.glb",
                    centerOrigin = Position(x = 0.0f, y = 0.0f, z = 0.0f)
                )
                // Changing the model settings only after it loaded
                referenceModel?.let {
                    // Changing the dice color
                    // The 3d model has a material named "background"
                    it.getMaterial("background")?.filamentMaterialInstance?.setBaseColor(
                        colorOf(
                            settingsModel.diceColor.color.red,
                            settingsModel.diceColor.color.green,
                            settingsModel.diceColor.color.blue
                        )
                    )
                }
            }

        }
    } else { // Creating the 2d images if the dice velocity is not slow
        // The density is used to have the image dimensions related to the device size
        val density = context.resources.displayMetrics.density
        val size = 64 * density // bitmap size
        val radius = 64 / 14 * density // circle radius
        // Used to draw the circles
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 4f
        }
        // Create 2D faces
        faces = List<Bitmap>(6) {
            Bitmap.createBitmap(
                size.toInt(),
                size.toInt(),
                Bitmap.Config.ARGB_8888
            )
        }
        faces.forEachIndexed { index, bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(settingsModel.diceColor.color.toArgb()) // background color of the dice
            // Black border lines
            canvas.drawLine(0f, 0f, 0f, size, paint)
            canvas.drawLine(0f, 0f, size, 0f, paint)
            canvas.drawLine(size, 0f, size, size, paint)
            canvas.drawLine(0f, size, size, size, paint)
            // Adding the circles based on the face index
            when (index) {
                // Face 1
                0 -> canvas.drawCircle(size / 2, size / 2, radius, paint)
                // Face 2
                1 -> {
                    canvas.drawCircle(size / 3, 2 * size / 3, radius, paint)
                    canvas.drawCircle(2 * size / 3, size / 3, radius, paint)
                }
                // Face 3
                2 -> {
                    canvas.drawCircle(size / 2, size / 2, radius, paint)
                    canvas.drawCircle(size / 3, 2 * size / 3, radius, paint)
                    canvas.drawCircle(2 * size / 3, size / 3, radius, paint)
                }
                // Face 4
                3 -> {
                    canvas.drawCircle(size / 3, size / 3, radius, paint)
                    canvas.drawCircle(size / 3, 2 * size / 3, radius, paint)
                    canvas.drawCircle(2 * size / 3, size / 3, radius, paint)
                    canvas.drawCircle(2 * size / 3, 2 * size / 3, radius, paint)
                }
                // Face 5
                4 -> {
                    canvas.drawCircle(size / 2, size / 2, radius, paint)
                    canvas.drawCircle(size / 3, size / 3, radius, paint)
                    canvas.drawCircle(size / 3, 2 * size / 3, radius, paint)
                    canvas.drawCircle(2 * size / 3, size / 3, radius, paint)
                    canvas.drawCircle(2 * size / 3, 2 * size / 3, radius, paint)
                }
                // Face 6
                5 -> {
                    canvas.drawCircle(size / 3, size / 3, radius, paint)
                    canvas.drawCircle(size / 3, 2 * size / 3, radius, paint)
                    canvas.drawCircle(2 * size / 3, size / 3, radius, paint)
                    canvas.drawCircle(2 * size / 3, 2 * size / 3, radius, paint)
                    canvas.drawCircle(size / 3, size / 2, radius, paint)
                    canvas.drawCircle(2 * size / 3, size / 2, radius, paint)
                }
            }
        }
    }

    val gameState = if (continueGame) {
        val file = File(context.cacheDir, "state")
        val json = file.readText()
        val gson = Gson()
        gson.fromJson(json, LastGameState::class.java)
    } else null

    // Loading the Play function with the correct information
    if (settingsModel.diceVelocity == DiceVelocity.Slow) {
        if (referenceModel != null) {
            Play(settingsModel, numberOfPlayers, referenceModel, faces, gameState)
        } else { // Model is not loaded
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.loading))
            }
        }
    } else {
        Play(settingsModel, numberOfPlayers, null, faces, gameState)
    }
}

@ExperimentalAnimationApi
@Composable
fun Play(
    settingsModel: SettingsModel,
    numPlayers: Int,
    referenceModel: RenderableInstance?,
    faces: List<Bitmap>,
    lastGameState: LastGameState?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Calculate the number of players based on the last game saved
    var numberOfPlayers = numPlayers
    if (lastGameState != null) numberOfPlayers = lastGameState.players.size
    // Model for controlling the game state
    val gameState by rememberSaveable {
        mutableStateOf(
            GameState(
                numberOfPlayers,
                settingsModel.diceVelocity == DiceVelocity.Slow,
            ).apply { restore(lastGameState) }
        )
    }

    // Triggered by the "gameState.animate" variable used to control the animation
    val transition = updateTransition(targetState = gameState.animate, label = "Cube")

    // Variable used to update the scores after the animation finishes
    val waitTime = when (settingsModel.diceVelocity) {
        DiceVelocity.Slow -> 1000
        DiceVelocity.Medium -> 500
        DiceVelocity.Fast -> 0
    }

    // Two different sounds for different velocity
    val mediumSound = MediaPlayer.create(context, R.raw.medium)
    val slowSound = MediaPlayer.create(context, R.raw.slow)

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        // Background image
        Image(
            painter = painterResource(if (settingsModel.darkTheme) R.drawable.black else R.drawable.white),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
    Column(Modifier.fillMaxSize()) {
        // Header row
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = {
                saveState(context, gameState)
                ScreenRouter.navigateTo(Screens.Home)
            }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.size(16.dp))
            gameState.players.forEachIndexed { index, player ->
                Text(
                    stringResource(
                        R.string.player_index,
                        index + 1
                    ) + ": ${player.scores.values.sum() + if (player.bonusReached) 35 else 0}",
                    modifier = Modifier
                        .weight(1f, true)
                        .align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.h6,
                    fontWeight = if (gameState.currentPlayer == index) FontWeight.Bold else FontWeight.Light
                )
            }
        }
        Column(Modifier.weight(1f, true)) { ScoreBoard(gameState) }
        // Dices
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            gameState.dices.forEach {
                Column {
                    Dice(
                        it,
                        settingsModel.diceColor.color,
                        referenceModel,
                        faces,
                        settingsModel.diceVelocity,
                        transition
                    )
                    IconButton(
                        onClick = { if (gameState.currentRoll > 0) it.locked = !it.locked },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            if (it.locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = "Lock",
                            tint = if (it.locked) Color(0xFFFFA000) else MaterialTheme.colors.onBackground
                        )
                    }
                }
            }
        }
        // Buttons
        // Roll Button enabled only if the number of rolls is less than 3 (starting from 0)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                // Play the sound if it's enabled
                if (settingsModel.soundEnabled) {
                    if (settingsModel.diceVelocity == DiceVelocity.Slow) slowSound.start()
                    else mediumSound.start()
                }
                gameState.currentRoll++
                gameState.dices.forEach {
                    // Skip the dice if it's locked
                    if (it.locked) return@forEach
                    // Select a random value
                    it.number = Random(System.nanoTime()).nextInt(0, 6)
                    // Update variables used for animation in the Dice class
                    if (it.is3D) {
                        // Setting the kx and ky variable to the values from the Values3D utility array (rotation values on the x and y axis)
                        // Then it adds a random value multiplied by 4 to have multiple spins
                        // 4 represents a complete spin of the dice
                        val randomX = Random(System.nanoTime()).nextInt(1, 10) * 4
                        val randomY = Random(System.nanoTime()).nextInt(1, 10) * 4
                        val selectedNumber = Values3D[it.number]
                        it.rotation =
                            Pair(selectedNumber.first + randomX, selectedNumber.second + randomY)
                    } else {
                        // Adding a random value multiplied by 6 to the selected number to have multiple spins
                        // 6 represents a complete spin of the dice
                        it.randomValue = it.number + Random(System.nanoTime()).nextInt(1, 10) * 6
                    }
                }
                // Enabling animation
                gameState.animate = true

                // Disable button
                gameState.buttonEnabled = false
            }, enabled = gameState.currentRoll < 3 && gameState.buttonEnabled) {
                Text(stringResource(R.string.roll))
            }
            // Save score button (enabled only if selectedScore is null)
            Button(onClick = {
                // This value is not null (otherwise the button would be disabled)
                val score = gameState.selectedScore!!
                // Enabling the possibility to make another Yahtzee after the first one
                if (score.first == ScoreType.Yahtzee && score.second == 50)
                    gameState.players[gameState.currentPlayer].doubleYahtzee = true
                // Disabling double yahtzee after the second yahtzee
                if (score.first != ScoreType.Yahtzee && score.second == 50)
                    gameState.players[gameState.currentPlayer].doubleYahtzee = false
                // Save the score in the player map
                gameState.players[gameState.currentPlayer].scores[score.first] = score.second
                // Calculate bonus with the new saved score
                gameState.players[gameState.currentPlayer].calculateBonus()
                // Current Player is the last player
                if (gameState.currentPlayer == gameState.players.size - 1) {
                    // Switching player
                    gameState.currentPlayer = 0
                    // Go to next round
                    gameState.currentRound++
                } else {
                    // Go to next player
                    gameState.currentPlayer++
                }
                // Resetting variables
                gameState.currentRoll = 0
                gameState.selectedScore = null
                gameState.dices.forEach { it.locked = false }
            }, enabled = gameState.selectedScore != null && gameState.buttonEnabled) {
                Text(stringResource(R.string.save_score))
            }
        }

        if (gameState.currentRound == 13)
            GameFinished(gameState)

        BackHandler {
            // Saving the current game state before exiting
            saveState(context, gameState)
            ScreenRouter.navigateTo(Screens.Home)
        }

        // Side-effect to run a coroutine to calculate the scores
        LaunchedEffect(gameState.buttonEnabled) {
            // Wait for animation to finish
            delay(waitTime.toLong())
            if (gameState.currentRoll > 0) {
                // Calculate all the scores for all the players (to display in the ScoreBoard)
                gameState.players.forEachIndexed { index, player ->
                    ScoreType.values().forEach { score ->
                        // Get the current saved score
                        val savedScore = player.scores[score]
                        val newScore =
                            if (gameState.currentPlayer == index && savedScore == null) { // it needs to be updated
                                calculateScore(
                                    gameState.dices.map { dice -> dice.number + 1 },
                                    score,
                                    player.doubleYahtzee
                                )
                            } else savedScore ?: 0 // otherwise returns the savedScore or 0
                        gameState.scores[index][score] = newScore
                    }
                }
            }
            // Re-enables the button after the animation
            gameState.buttonEnabled = true
        }

        // Side-effect to release sound resources and to listen to lifecycle changes
        DisposableEffect(lifecycleOwner) {
            // Observing lifecycle changes
            val observer = LifecycleEventObserver { _, event ->
                // Saving game state
                if (event == Lifecycle.Event.ON_STOP) {
                    // Saving the current game state
                    saveState(context, gameState)
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                mediumSound.release()
                slowSound.release()
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }
}

@Composable
fun ScoreBoard(gameState: GameState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Column(Modifier.weight(1f, true)) {
            // Showing the first six score type on the left side (number scores) and the bonus score
            ScoreType.values().take(6).forEach {
                Row(Modifier.weight(1f, true)) {
                    Score(it, gameState)
                }
            }
            Row(Modifier.weight(1f, true)) {
                BonusScore(gameState.players, gameState.currentPlayer)
            }
        }
        Column(Modifier.weight(1f, true)) {
            // Showing the last 7 on the right side
            ScoreType.values().takeLast(7).forEach {
                Row(Modifier.weight(1f, true)) {
                    Score(it, gameState)
                }
            }
        }
    }
}

@Composable
fun Score(type: ScoreType, gameState: GameState) {
    // Variable to show an help dialog
    var tooltipVisible by remember { mutableStateOf(false) }
    val tooltipStyle = rememberTooltipStyle().apply {
        color = MaterialTheme.colors.background
        tipWidth = 0.dp
        tipHeight = 0.dp
    }

    Row(
        Modifier
            .border(2.dp, MaterialTheme.colors.onBackground)
            .padding(2.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(2f, true)
                .clickable {
                    tooltipVisible = true
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(3000)
                        tooltipVisible = false
                    }
                }) {
            Text(
                stringResource(type.text), modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.Center),
                textAlign = TextAlign.Center
            )
            if (tooltipVisible)
                Tooltip(
                    anchorEdge = AnchorEdge.Top,
                    tooltipStyle = tooltipStyle,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(stringResource(type.helperText), style = MaterialTheme.typography.caption)
                }
        }
        gameState.players.forEachIndexed { index, player ->
            // Getting the score from the player map
            val savedScore = player.scores[type]
            val score = gameState.scores[index][type] ?: 0
            // Change the background color only if the score was already saved
            val color = if (savedScore != null) Color.Gray else MaterialTheme.colors.background
            Column(
                modifier = Modifier
                    .padding(6.dp)
                    .border(
                        3.dp,
                        // Apply a border to the selected score
                        if (gameState.selectedScore?.first == type && index == gameState.currentPlayer) Color.Blue
                        else MaterialTheme.colors.onBackground
                    )
                    .background(color)
                    .weight(1f, true)
                    .fillMaxSize()
                    .clickable {
                        // Select score only if it's the current player turn and it's after the first roll
                        if (index == gameState.currentPlayer && savedScore == null && gameState.currentRoll > 0)
                            gameState.selectedScore = Pair(type, score)
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(score.toString())
            }
        }
    }
}

@Composable
fun BonusScore(players: List<Player>, currentPlayer: Int) {
    Row(
        Modifier
            .border(2.dp, MaterialTheme.colors.onBackground)
            .padding(4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Variable used to show a help dialog
        var tooltipVisible by remember { mutableStateOf(false) }
        val tooltipStyle = rememberTooltipStyle().apply {
            color = MaterialTheme.colors.background
            tipWidth = 0.dp
            tipHeight = 0.dp
        }
        Box(
            Modifier
                .weight(2f, true)
                .clickable {
                    tooltipVisible = true
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(3000)
                        tooltipVisible = false
                    }

                }) {
            Text(
                stringResource(R.string.bonus), modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.Center),
                textAlign = TextAlign.Center
            )
            if (tooltipVisible)
                Tooltip(
                    anchorEdge = AnchorEdge.Top,
                    tooltipStyle = tooltipStyle,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        stringResource(R.string.tutorial_bonus),
                        style = MaterialTheme.typography.caption
                    )
                }
        }
        players.forEachIndexed { index, player ->
            Column(
                modifier = Modifier
                    .padding(1.dp)
                    .weight(1f, true)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Showing a check icon if the bonus is reached, otherwise show the number of points scored
                if (player.bonusReached)
                    Icon(Icons.Outlined.Check, contentDescription = "Check")
                else Text(
                    "${player.lastSixScore}/63",
                    fontWeight = if (currentPlayer == index) FontWeight.Bold else FontWeight.Light
                )
            }
        }
    }
}

@Composable
fun Dice(
    dice: DiceModel,
    color: Color,
    referenceModel: RenderableInstance?,
    faces: List<Bitmap>,
    diceVelocity: DiceVelocity,
    transition: Transition<Boolean>
) {
    when (diceVelocity) {
        // Showing the 3d model if the velocity is slow
        DiceVelocity.Slow -> {
            // Creating the 3D model (that can be added to the SceneView)
            val modelNode by remember {
                mutableStateOf(
                    ModelNode().apply {
                        // Setting the referenceModel
                        setModel(referenceModel!!.renderable)
                        scaleModel(1.75f)
                        modelInstance?.getMaterial("background")?.filamentMaterialInstance?.setBaseColor(
                            colorOf(color.toArgb())
                        )
                        rotation =
                            Rotation(dice.rotation.first * 90f, dice.rotation.second * 90f, 90f)
                    }
                )
            }
            // State changed only if the "animate" variable (in Play) is true
            // The animation is based on changing the rotation of the model
            val rotation by transition.animateOffset(
                label = "Cube",
                transitionSpec = { tween(durationMillis = 1000) }) { state ->
                if (state) Offset(dice.rotation.first * 90f, dice.rotation.second * 90f)
                else Offset(modelNode.rotation.x, modelNode.rotation.y)
            }
            modelNode.rotation = Rotation(rotation.x, rotation.y, 90f)
            val bgColor = MaterialTheme.colors.background

            // Calling AndroidView to render a non-JetpackCompose layout
            AndroidView(
                modifier = Modifier.size(80.dp),
                factory = { context ->
                    SceneView(context).apply {
                        mainLight = null
                        backgroundColor =
                            colorOf(bgColor.red, bgColor.green, bgColor.blue)
                        addChild(modelNode)
                        // Disabling dice input
                        gestureDetector.moveGestureDetector = null
                        gestureDetector.rotateGestureDetector = null
                        gestureDetector.scaleGestureDetector = null
                        gestureDetector.cameraManipulator = null
                    }
                },
            )
        }
        // Showing the 2d images if the velocity is not slow
        else -> {
            // Animating only on medium velocity
            val value = if (diceVelocity == DiceVelocity.Medium) {
                // Animation is based on changing the index of the image to show
                transition.animateInt(
                    label = "Cube",
                    transitionSpec = { tween(durationMillis = 500) }) { state ->
                    if (state) dice.randomValue else dice.number
                }.value
            } else {
                // No animation on fast velocity
                dice.number
            }

            Image(
                faces[value % 6].asImageBitmap(),
                contentDescription = "Face",
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
fun GameFinished(gameState: GameState) {
    val context = LocalContext.current

    // Loading the database
    val db = GameDatabase.getInstance(context)
    val dao = db.gameDao()
    val repository = GameRepository(dao)

    // Calculate the final scores
    val player1Score =
        gameState.players[0].scores.values.sum() + if (gameState.players[0].bonusReached) 35 else 0
    var player2Score: Int? = null
    if (gameState.players.size == 2) {
        player2Score =
            gameState.players[1].scores.values.sum() + if (gameState.players[1].bonusReached) 35 else 0
    }

    AlertDialog(onDismissRequest = {},
        title = { Text(stringResource(R.string.game_finished)) },
        text = {
            // Showing different texts based on the number of players
            if (gameState.players.size == 1)
                Text(stringResource(R.string.your_score, player1Score))
            else {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.player_score, 1, player1Score))
                    player2Score?.let {
                        Text(stringResource(R.string.player_score, 2, it))
                        if (player1Score == it) Text(stringResource(R.string.draw))
                        else if (player1Score > it) Text(
                            stringResource(
                                R.string.player_won,
                                1,
                                player1Score
                            )
                        )
                        else Text(stringResource(R.string.player_won, 2, it))
                    }
                }
            }
        },
        // Saving the game in the database after clicking the buttons
        confirmButton = {
            // Play Again button
            Button(onClick = {
                repository.saveGame(player1Score, player2Score)
                // Reset the game board
                gameState.dices.forEach {
                    it.locked = false
                    it.rotation = Values3D[1]
                    it.randomValue = 1
                    it.number = 1
                }
                gameState.currentRound = 0
                gameState.currentRoll = 0
                gameState.currentPlayer = 0
                gameState.players.forEach {
                    it.bonusReached = false
                    it.lastSixScore = 0
                    it.scores = mutableMapOf()
                }
                gameState.selectedScore = null
                gameState.scores.forEach { it.clear() }
                File(context.cacheDir, "state").delete()
            }) {
                Text(stringResource(R.string.play_again))
            }
        },
        dismissButton = {
            // Go Back button
            Button(onClick = {
                repository.saveGame(player1Score, player2Score)
                // Return home
                ScreenRouter.navigateTo(Screens.Home)
                File(context.cacheDir, "state").delete()
            }) {
                Text(stringResource(R.string.go_back))
            }
        }
    )
}

enum class ScoreType(val text: Int, val helperText: Int) {
    One(R.string.one, R.string.tutorial_one_to_six),
    Two(R.string.two, R.string.tutorial_one_to_six),
    Three(R.string.three, R.string.tutorial_one_to_six),
    Four(R.string.four, R.string.tutorial_one_to_six),
    Five(R.string.five, R.string.tutorial_one_to_six),
    Six(R.string.six, R.string.tutorial_one_to_six),
    Tris(R.string.tris, R.string.tutorial_tris_poker),
    Poker(R.string.poker, R.string.tutorial_tris_poker),
    Full(R.string.full, R.string.tutorial_full),
    SmallStraight(R.string.small_straight, R.string.tutorial_small_straight),
    LargeStraight(R.string.large_straight, R.string.tutorial_large_straight),
    Yahtzee(R.string.yahtzee, R.string.tutorial_yahtzee),
    Chance(R.string.chance, R.string.tutorial_chance)
}

// Utility function to save the current game State
fun saveState(context: Context, gameState: GameState) {
    val state = LastGameState(
        gameState.dices.map { it.number },
        gameState.currentRoll,
        gameState.players
    )
    val gson = Gson()
    val jsonString = gson.toJson(state)

    CoroutineScope(Dispatchers.IO).launch {
        val file = File(context.cacheDir, "state")
        // Creating the new state file in the cache directory
        file.writeText(jsonString, Charset.defaultCharset())
    }
}

// Utility function used to calculate the score
fun calculateScore(dices: List<Int>, type: ScoreType, doubleYahtzee: Boolean): Int {
    if (doubleYahtzee && dices.distinct().size == 1) return 50
    when (type) {
        // The return value is the number of equal dice for the i-th face multiplied by i
        ScoreType.One -> return dices.filter { dice -> dice == 1 }.size
        ScoreType.Two -> return dices.filter { dice -> dice == 2 }.size * 2
        ScoreType.Three -> return dices.filter { dice -> dice == 3 }.size * 3
        ScoreType.Four -> return dices.filter { dice -> dice == 4 }.size * 4
        ScoreType.Five -> return dices.filter { dice -> dice == 5 }.size * 5
        ScoreType.Six -> return dices.filter { dice -> dice == 6 }.size * 6
        ScoreType.Tris -> {
            if (dices.distinct().size > 3) return 0 // it's not a tris
            // Check if the size of sublist of the same value is more or equal than 3
            dices.distinct().forEach { value ->
                val subList = dices.filter { it == value }
                if (subList.size >= 3) return dices.sum()
            }
            return 0
        }
        // Similar code to the tris
        ScoreType.Poker -> {
            if (dices.distinct().size > 2) return 0
            dices.distinct().forEach { value ->
                val subList = dices.filter { it == value }
                if (subList.size >= 4) return dices.sum()
            }
            return 0
        }
        ScoreType.Full -> {
            val first = dices.first()
            // subList1 is all the values equal to the first
            val subList1 = dices.filter { dice -> dice == first }
            // subList2 is dices - subList1
            val subList2 = dices.filter { dice -> dice != first }
            if (subList2.distinct().size != 1) return 0 // the values are different
            // The possibilities are:
            // subList1 has size 2 => subList2 has size 3
            // subList1 has size 3 => subList2 has size 2
            if (subList1.size >= 2 && subList2.size >= 2) return 25
            return 0
        }
        ScoreType.SmallStraight -> {
            // Checking all the three different cases
            // 1, 2, 3, 4
            // 2, 3, 4, 5
            // 3, 4, 5, 6
            if (dices.distinct().size < 4) return 0
            if (dices.contains(3) && dices.contains(4)) {
                if (dices.contains(2)) {
                    if (dices.contains(1) || dices.contains(5)) return 30
                } else if (dices.contains(5) && dices.contains(6)) {
                    return 30
                }
            }
            return 0
        }
        // Checking if the dices are all different and the sum is the 15 (case: 1, 2, 3, 4, 5) or 20 (case: 2, 3, 4, 5, 6)
        ScoreType.LargeStraight -> return if (dices.distinct().size == 5 && (dices.sum() == 15 || dices.sum() == 20)) 40 else 0
        // Returning the sum
        ScoreType.Chance -> return dices.sum()
        // Checking if all the dices are equal
        ScoreType.Yahtzee -> return if (dices.distinct().size == 1) 50 else 0
    }
}
