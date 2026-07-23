package com.example.koiskapp

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.koiskapp.ui.theme.KoiskAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(packageName)) {
            setupKiosk()
        }

        enableEdgeToEdge()
        setContent {
            KoiskAppTheme {
                AppRoot(
                    isDeviceOwner = dpm.isDeviceOwnerApp(packageName),
                    isLocked = { isInLockTask() },
                    onExitKiosk = { exitKiosk() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (dpm.isDeviceOwnerApp(packageName)) {
            startLockTask()
        }
    }

    private fun setupKiosk() {
        dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))

        val filter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        dpm.addPersistentPreferredActivity(
            adminComponent,
            filter,
            ComponentName(packageName, MainActivity::class.java.name)
        )

        dpm.setStatusBarDisabled(adminComponent, true)
        dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
        dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
        dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
    }

    /** kiosk থেকে বেরিয়ে অ্যাপ বন্ধ করে */
    private fun exitKiosk() {
        if (dpm.isDeviceOwnerApp(packageName)) {
            dpm.setStatusBarDisabled(adminComponent, false)
            dpm.clearPackagePersistentPreferredActivities(adminComponent, packageName)
        }
        stopLockTask()
        finishAffinity()
    }

    private fun isInLockTask(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }
}

/* ---------------- UI ---------------- */

@Composable
fun AppRoot(
    isDeviceOwner: Boolean,
    isLocked: () -> Boolean,
    onExitKiosk: () -> Unit
) {
    var screen by remember { mutableStateOf("home") }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        when (screen) {
            "home" -> HomeScreen(
                isDeviceOwner = isDeviceOwner,
                isLocked = isLocked,
                onGoSecond = { screen = "second" },
                onExitKiosk = onExitKiosk,
                modifier = Modifier.padding(padding)
            )
            "second" -> SecondScreen(
                onBack = { screen = "home" },
                onExitKiosk = onExitKiosk,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun HomeScreen(
    isDeviceOwner: Boolean,
    isLocked: () -> Boolean,
    onGoSecond: () -> Unit,
    onExitKiosk: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Screen 1 — Home", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(24.dp))

        StatusCard(isDeviceOwner = isDeviceOwner, locked = isLocked())

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onGoSecond,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Screen 2 তে যান →")
        }

        Spacer(Modifier.height(12.dp))

        ExitButton(onExitKiosk)
    }
}

@Composable
fun SecondScreen(
    onBack: () -> Unit,
    onExitKiosk: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Screen 2 — Details", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        Text(
            "এখানে ফিরে যাওয়ার বাটন কাজ করে, কিন্তু ডিভাইসের Back বাটন অ্যাপ থেকে বের করতে পারবে না।",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Home এ ফিরুন")
        }

        Spacer(Modifier.height(12.dp))

        ExitButton(onExitKiosk)
    }
}

@Composable
fun StatusCard(isDeviceOwner: Boolean, locked: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (isDeviceOwner) "✅ Device Owner: হ্যাঁ" else "❌ Device Owner: না",
                color = if (isDeviceOwner) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (locked) "🔒 Kiosk (Lock Task): চালু" else "🔓 Kiosk (Lock Task): বন্ধ",
                color = if (locked) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}

@Composable
fun ExitButton(onExitKiosk: () -> Unit) {
    Button(
        onClick = onExitKiosk,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("🚪 EXIT KIOSK (অ্যাপ বন্ধ)")
    }
}