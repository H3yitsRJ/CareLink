package navigation

import androidx.compose.ui.res.painterResource
import com.example.carelink.R
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.carelink.screens.MedicationsScreen
import com.example.carelink.ui.theme.CareLinkTheme

@Composable
fun BottomNavBar() {

    NavigationBar {

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.home_icon),
                    contentDescription = "Home"
                )
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.medications_icon),
                    contentDescription = "Medications"
                )
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.calendar_icon),
                    contentDescription = "Calendar"
                )
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.care_tasks_icon),
                    contentDescription = "Tasks"
                )
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.profile_icon),
                    contentDescription = "Profile"
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavPreview() {
    CareLinkTheme {
        BottomNavBar()
    }
}