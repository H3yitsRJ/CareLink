package navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.example.carelink.R
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carelink.ui.theme.CareLinkTheme
import androidx.compose.material3.Text

enum class BottomNavDestination { Home, Medications, Appointments, CareTasks, Profile }

@Composable
fun BottomNavBar(
    selectedDestination: BottomNavDestination = BottomNavDestination.Home,
    onDestinationSelected: (BottomNavDestination) -> Unit = {}
) {
    BoxWithConstraints {
        val showLabels = maxWidth / LocalDensity.current.fontScale >= 300.dp
        Surface(
            modifier = Modifier.padding(3.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {

                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    selected = selectedDestination == BottomNavDestination.Home,
                    onClick = { onDestinationSelected(BottomNavDestination.Home) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.home_icon),
                            contentDescription = "Home", modifier = Modifier.size(24.dp)
                        )
                    },
                    label = if (showLabels) { { Text("Home", maxLines = 1) } } else null,
                    alwaysShowLabel = true
                )

                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    selected = selectedDestination == BottomNavDestination.Medications,
                    onClick = { onDestinationSelected(BottomNavDestination.Medications) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.medications_icon),
                            contentDescription = "Medications", modifier = Modifier.size(24.dp)
                        )
                    },
                    label = if (showLabels) { { Text("Meds", maxLines = 1) } } else null,
                    alwaysShowLabel = true
                )

                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    selected = selectedDestination == BottomNavDestination.Appointments,
                    onClick = { onDestinationSelected(BottomNavDestination.Appointments) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.calendar_icon),
                            contentDescription = "Appointments", modifier = Modifier.size(24.dp)
                        )
                    },
                    label = if (showLabels) { { Text("Visits", maxLines = 1) } } else null,
                    alwaysShowLabel = true
                )

                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    selected = selectedDestination == BottomNavDestination.CareTasks,
                    onClick = { onDestinationSelected(BottomNavDestination.CareTasks) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.care_tasks_icon),
                            contentDescription = "Care tasks", modifier = Modifier.size(24.dp)
                        )
                    },
                    label = if (showLabels) { { Text("Tasks", maxLines = 1) } } else null,
                    alwaysShowLabel = true
                )

                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    selected = selectedDestination == BottomNavDestination.Profile,
                    onClick = { onDestinationSelected(BottomNavDestination.Profile) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.profile_icon),
                            contentDescription = "Profile", modifier = Modifier.size(24.dp)
                        )
                    },
                    label = if (showLabels) { { Text("Profile", maxLines = 1) } } else null,
                    alwaysShowLabel = true
                )
            }
        }
    }
}

@Preview
@Composable
private fun BottomNavPreview() {
    CareLinkTheme {
        BottomNavBar(BottomNavDestination.Home) {}
    }
}
