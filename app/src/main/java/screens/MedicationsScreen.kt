package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carelink.ui.theme.CareLinkTheme
import navigation.BottomNavBar

@Composable
fun MedicationsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp)
    ){
        Text(
            text = "Medications",
            fontSize = 43.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Manage and schedule your medications"
        )

        Spacer(modifier = Modifier.height(50.dp))

        Button(
            onClick = { },
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .height(63.dp)
                .width(197.dp)
        ){
            Text(
                text = "Add Medication",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(43.dp))

        Text(
            text = "Today",
            fontSize = 32.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {

            items(6) {

                MedicationCard()

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

            }
        }

        BottomNavBar()
   }
}

@Composable
fun MedicationCard() {
    Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Spacer(modifier = Modifier.height(97.dp))
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun MedicationsScreenPreview() {
    CareLinkTheme {
        MedicationsScreen()
    }
}

