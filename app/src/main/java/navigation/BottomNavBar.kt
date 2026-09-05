package navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.example.carelink.R
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carelink.ui.theme.CareLinkTheme

enum class BottomNavDestination { Home, Medications, Appointments, CareTasks, Profile }

@Composable
fun BottomNavBar(
    selectedDestination: BottomNavDestination = BottomNavDestination.Home,
    onDestinationSelected: (BottomNavDestination) -> Unit = {}
) {
    Surface(
        modifier = Modifier.padding(3.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        NavigationBar() {

            NavigationBarItem(
                selected = selectedDestination == BottomNavDestination.Home,
                onClick = { onDestinationSelected(BottomNavDestination.Home) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.home_icon),
                        contentDescription = "Home"
                    )
                }
            )

            NavigationBarItem(
                selected = selectedDestination == BottomNavDestination.Medications,
                onClick = { onDestinationSelected(BottomNavDestination.Medications) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.medications_icon),
                        contentDescription = "Medications"
                    )
                }
            )

            NavigationBarItem(
                selected = selectedDestination == BottomNavDestination.Appointments,
                onClick = { onDestinationSelected(BottomNavDestination.Appointments) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.calendar_icon),
                        contentDescription = "Appointments"
                    )
                }
            )

            NavigationBarItem(
                selected = selectedDestination == BottomNavDestination.CareTasks,
                onClick = { onDestinationSelected(BottomNavDestination.CareTasks) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.care_tasks_icon),
                        contentDescription = "Care tasks"
                    )
                }
            )

            NavigationBarItem(
                selected = selectedDestination == BottomNavDestination.Profile,
                onClick = { onDestinationSelected(BottomNavDestination.Profile) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.profile_icon),
                        contentDescription = "Profile"
                    )
                }
            )
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
