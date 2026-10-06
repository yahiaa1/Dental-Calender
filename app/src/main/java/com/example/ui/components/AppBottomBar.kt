package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ClinicPrimary

@Composable
fun AppBottomBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onFabClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        color = Color.White,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Tab 0: التقويم (Calendar)
            BottomNavItem(
                icon = Icons.Default.CalendarMonth,
                label = "التقويم",
                isSelected = selectedIndex == 0,
                onClick = { onItemSelected(0) },
                modifier = Modifier.weight(1f)
            )

            // Tab 1: المرضى (Patients)
            BottomNavItem(
                icon = Icons.Default.PeopleOutline,
                label = "المرضى",
                isSelected = selectedIndex == 1,
                onClick = { onItemSelected(1) },
                modifier = Modifier.weight(1f)
            )

            // Center FAB (+)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ClinicPrimary)
                        .clickable(onClick = onFabClicked)
                        .testTag("bottom_bar_fab_add"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة موعد جديد",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Tab 2: التقارير (Reports)
            BottomNavItem(
                icon = Icons.Default.BarChart,
                label = "التقارير",
                isSelected = selectedIndex == 2,
                onClick = { onItemSelected(2) },
                modifier = Modifier.weight(1f)
            )

            // Tab 3: الإعدادات (Settings)
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "الإعدادات",
                isSelected = selectedIndex == 3,
                onClick = { onItemSelected(3) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ClinicPrimary else Color(0xFF90A4AE),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) ClinicPrimary else Color(0xFF90A4AE),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
