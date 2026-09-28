package com.orynex.bolosaathi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.orynex.bolosaathi.ui.BoloTheme
import com.orynex.bolosaathi.ui.C
import com.orynex.bolosaathi.ui.HomeScreen
import com.orynex.bolosaathi.ui.DiaryPage
import com.orynex.bolosaathi.ui.LegalViewer
import com.orynex.bolosaathi.ui.MainViewModel
import com.orynex.bolosaathi.ui.Motion
import com.orynex.bolosaathi.ui.PeopleScreen
import com.orynex.bolosaathi.ui.ScamScreen
import com.orynex.bolosaathi.ui.SettingsScreen
import com.orynex.bolosaathi.ui.WelcomeScreen

enum class Tab(val label: String, val icon: ImageVector, val iconOn: ImageVector) {
    HOME("घर", Icons.Outlined.Home, Icons.Filled.Home),
    SCAM("धोखा जाँच", Icons.Outlined.Shield, Icons.Filled.Shield),
    PEOPLE("मेरे लोग", Icons.Outlined.People, Icons.Filled.People),
    SETTINGS("सेटिंग", Icons.Outlined.Settings, Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoloTheme {
                var ready by rememberSaveable { mutableStateOf(BoloApp.instance.prefs.onboarded) }
                var tab by rememberSaveable { mutableStateOf(Tab.HOME) }

                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
                }

                AnimatedContent(
                    targetState = ready,
                    transitionSpec = { (fadeIn(tween(420)) + scaleIn(tween(420), initialScale = 0.96f)) togetherWith fadeOut(tween(250)) },
                    label = "gate",
                ) { isReady ->
                    if (!isReady) {
                        WelcomeScreen(vm) { ready = true }
                    } else {
                        Scaffold(
                            containerColor = C.Bg,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                NavigationBar(containerColor = C.Surface, tonalElevation = 0.dp) {
                                    Tab.entries.forEach { t ->
                                        val on = tab == t
                                        NavigationBarItem(
                                            selected = on,
                                            onClick = { tab = t },
                                            icon = { Icon(if (on) t.iconOn else t.icon, contentDescription = t.label) },
                                            label = { Text(t.label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = C.Brand,
                                                selectedTextColor = C.Brand,
                                                indicatorColor = C.BrandSoft,
                                                unselectedIconColor = C.Muted,
                                                unselectedTextColor = C.Muted,
                                            ),
                                        )
                                    }
                                }
                            },
                        ) { pad ->
                            AnimatedContent(
                                targetState = tab,
                                modifier = Modifier.fillMaxSize().padding(pad),
                                transitionSpec = {
                                    (fadeIn(tween(260, easing = Motion.EaseOut)) + slideInVertically(tween(320, easing = Motion.EaseOut)) { it / 28 }) togetherWith
                                        fadeOut(tween(120))
                                },
                                label = "tabs",
                            ) { t ->
                                when (t) {
                                    Tab.HOME -> HomeScreen(vm, Modifier, openScam = { tab = Tab.SCAM }, openSettings = { tab = Tab.SETTINGS })
                                    Tab.SCAM -> ScamScreen(vm, Modifier)
                                    Tab.PEOPLE -> PeopleScreen(vm, Modifier)
                                    Tab.SETTINGS -> SettingsScreen(vm, Modifier, onLogout = { tab = Tab.HOME; ready = false })
                                }
                            }
                        }
                    }
                }
                val legalPage by vm.legal.collectAsState()
                legalPage?.let { (p, t) -> LegalViewer(p, t) { vm.legal.value = null } }
                val diary by vm.diaryOpen.collectAsState()
                if (diary) DiaryPage { vm.diaryOpen.value = false }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
        if (intent?.getBooleanExtra("listen", false) == true) {
            intent.removeExtra("listen")
            vm.micTap()
        }
    }
}
