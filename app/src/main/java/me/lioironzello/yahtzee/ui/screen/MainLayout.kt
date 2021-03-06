package me.lioironzello.yahtzee.ui.screen

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import me.lioironzello.yahtzee.R
import me.lioironzello.yahtzee.model.SettingsModel
import java.io.File

// Main Router
@ExperimentalAnimationApi
@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun MainLayout(settingsModel: SettingsModel) {
    // Variable set in Home that needs to be passed to PlayLayout
    val numberOfPlayers = rememberSaveable { mutableStateOf(1) }
    val continueGame = rememberSaveable { mutableStateOf(false) }

    when (ScreenRouter.currentScreen) {
        Screens.Home -> Home { num, con -> numberOfPlayers.value = num; continueGame.value = con }
        Screens.Settings -> SettingsLayout(settingsModel)
        Screens.PreviousGames -> PreviousGames()
        Screens.Tutorial -> Tutorial()
        Screens.Play -> PlayLayout(settingsModel, numberOfPlayers.value, continueGame.value)
    }
}

// Home Screen
@Composable
fun Home(setPlaySettings: (players: Int, continueGame: Boolean) -> Unit) {
    val context = LocalContext.current

    val playDialog = rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { ScreenRouter.navigateTo(Screens.Tutorial) }) {
                Icon(Icons.Outlined.HelpOutline, contentDescription = "Tutorial")
            }
            IconButton(onClick = { ScreenRouter.navigateTo(Screens.Settings) }) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        }
        Spacer(modifier = Modifier.weight(3f, true))
        Text(
            stringResource(R.string.yahtzee),
            style = MaterialTheme.typography.h1,
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(3f, true))
        Button(
            onClick = { playDialog.value = true }, modifier = Modifier
                .width(192.dp)
                .height(64.dp), elevation = ButtonDefaults.elevation(8.dp)
        ) {
            Text(stringResource(R.string.play), style = MaterialTheme.typography.h5)
        }
        if (File(context.cacheDir, "state").exists())
            Button(onClick = {
                setPlaySettings(1, true)
                ScreenRouter.navigateTo(Screens.Play)
            }, modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.continue_game))
            }
        Spacer(modifier = Modifier.weight(1f, true))
        OutlinedButton(onClick = { ScreenRouter.navigateTo(Screens.PreviousGames) }) {
            Text(stringResource(R.string.previous_games), style = MaterialTheme.typography.button)
        }
        Spacer(modifier = Modifier.weight(1f, true))

        // Dialog to select the number of players
        if (playDialog.value) {
            AlertDialog(onDismissRequest = { playDialog.value = false },
                title = { Text(stringResource(R.string.select_num_players)) },
                text = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = {
                            setPlaySettings(1, false) // Updating the variable in the router
                            playDialog.value = false
                            ScreenRouter.navigateTo(Screens.Play)
                        }) {
                            Text(stringResource(R.string.single_player))
                        }
                        Button(onClick = {
                            setPlaySettings(2, false) // Updating the variable in the router
                            playDialog.value = false
                            ScreenRouter.navigateTo(Screens.Play)
                        }) {
                            Text(stringResource(R.string.local))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    Button(onClick = { playDialog.value = false }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }
    }
}
