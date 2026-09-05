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

@Composable
fun BottomNavBar() {
    Surface(
        modifier = Modifier.padding(3.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        NavigationBar() {

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
}

@Preview
@Composable
private fun BottomNavPreview() {
    CareLinkTheme {
        BottomNavBar()
    }
}