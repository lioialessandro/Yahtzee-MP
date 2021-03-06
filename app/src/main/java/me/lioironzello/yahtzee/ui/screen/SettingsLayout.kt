package me.lioironzello.yahtzee.ui.screen

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.google.accompanist.pager.*
import me.lioironzello.yahtzee.R
import me.lioironzello.yahtzee.model.DiceColor
import me.lioironzello.yahtzee.model.DiceVelocity
import me.lioironzello.yahtzee.model.SettingsModel
import kotlin.math.absoluteValue

@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun SettingsLayout(settingsModel: SettingsModel) {
    val context = LocalContext.current

    // State of this layout with default value saved in the SharedPreferences (loaded in the MainActivity)
    val velocityExpanded = rememberSaveable { mutableStateOf(false) }
    val soundEnabled = rememberSaveable { mutableStateOf(settingsModel.soundEnabled) }
    val darkTheme = rememberSaveable { mutableStateOf(settingsModel.darkTheme) }
    val diceVelocity = rememberSaveable { mutableStateOf(settingsModel.diceVelocity) }
    val pagerState = rememberPagerState(settingsModel.diceColor.ordinal)

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = {
                IconButton(onClick = { ScreenRouter.navigateTo(Screens.Home) }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            backgroundColor = MaterialTheme.colors.background,
            elevation = 0.dp
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Dark Theme
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.dark_theme), style = MaterialTheme.typography.body1)
                Switch(
                    checked = darkTheme.value,
                    onCheckedChange = { darkTheme.value = it }
                )
            }
            // Sound Enabled
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.sound_effect), style = MaterialTheme.typography.body1)
                Switch(
                    checked = soundEnabled.value,
                    onCheckedChange = { soundEnabled.value = it }
                )
            }
            // Dice Color
            Text(stringResource(R.string.dice_color), style = MaterialTheme.typography.body1)
            // Code from https://google.github.io/accompanist/pager/#item-scroll-effects
            HorizontalPager(
                count = DiceColor.values().size,
                modifier = Modifier.padding(8.dp),
                state = pagerState
            ) { page ->
                Card(
                    Modifier
                        .graphicsLayer {
                            // Calculate the absolute offset for the current page from the
                            // scroll position. We use the absolute value which allows us to mirror
                            // any effects for both directions
                            val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue

                            // We animate the scaleX + scaleY, between 85% and 100%
                            lerp(
                                start = 0.85f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            ).also { scale ->
                                scaleX = scale
                                scaleY = scale
                            }

                            // We animate the alpha, between 50% and 100%
                            alpha = lerp(
                                start = 0.5f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(6.dp, Color.Gray))
                            .size(80.dp)
                            .background(DiceColor.values()[page].color)
                    )
                }
            }
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            // Dice Velocity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.roll_velocity), style = MaterialTheme.typography.body1)
                ExposedDropdownMenuBox(
                    expanded = velocityExpanded.value,
                    onExpandedChange = { velocityExpanded.value = !velocityExpanded.value }) {
                    // Read-only text field used to display the selected dice velocity
                    OutlinedTextField(
                        modifier = Modifier.width(192.dp),
                        value = stringResource(diceVelocity.value.text),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = velocityExpanded.value) },
                    )
                    ExposedDropdownMenu(
                        expanded = velocityExpanded.value,
                        onDismissRequest = { velocityExpanded.value = false }) {
                        // if the device supports OpenGL version 3, 3d dice rendering is enabled
                        if (settingsModel.glVersion == 3)
                            DropdownMenuItem(onClick = {
                                diceVelocity.value = DiceVelocity.Slow
                                velocityExpanded.value = false
                            }) { Text(stringResource(R.string.slow)) }
                        DropdownMenuItem(onClick = {
                            diceVelocity.value = DiceVelocity.Medium
                            velocityExpanded.value = false
                        }) { Text(stringResource(R.string.medium)) }
                        DropdownMenuItem(onClick = {
                            diceVelocity.value = DiceVelocity.Fast
                            velocityExpanded.value = false
                        }) { Text(stringResource(R.string.fast)) }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f, true))
        Button(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.End)
                .width(128.dp)
                .height(48.dp),
            onClick = {
                // Saving the settings in the SharedPreferences
                settingsModel.darkTheme = darkTheme.value
                settingsModel.diceColor = DiceColor.values()[pagerState.currentPage]
                settingsModel.diceVelocity = diceVelocity.value
                settingsModel.soundEnabled = soundEnabled.value
                val sharedPreferences =
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("darkTheme", settingsModel.darkTheme)
                editor.putInt("dice", settingsModel.diceColor.ordinal)
                editor.putInt("diceVelocity", settingsModel.diceVelocity.ordinal)
                editor.putBoolean("soundEnabled", settingsModel.soundEnabled)
                editor.apply()
                ScreenRouter.navigateTo(Screens.Home)
            }) {
            Text(stringResource(R.string.save), style = MaterialTheme.typography.body1)
        }
        // Credits
        Column(Modifier.padding(16.dp)) {
            Divider()
            Text(stringResource(R.string.libraries), fontWeight = FontWeight.Bold)
            LibraryRow(
                name = "Accompanist-Pager",
                url = "https://google.github.io/accompanist/pager/"
            )
            LibraryRow(name = "SceneView", url = "https://github.com/SceneView/sceneview-android")
            LibraryRow(name = "Coil", url = "https://github.com/coil-kt/coil")
            LibraryRow(name = "ComposeTooltip", url = "https://github.com/skgmn/ComposeTooltip")
        }
        BackHandler {
            ScreenRouter.navigateTo(Screens.Home)
        }
    }
}

// Utility function for code reusability
@Composable
fun LibraryRow(name: String, url: String) {
    val uriHandler = LocalUriHandler.current

    Row(Modifier.fillMaxWidth()) {
        Text(name, modifier = Modifier.weight(2f, true))
        Text(
            stringResource(R.string.link),
            modifier = Modifier
                .weight(1f, true)
                .clickable { uriHandler.openUri(url) },
            color = Color(0, 116, 204),
            textDecoration = TextDecoration.Underline
        )
    }
}
