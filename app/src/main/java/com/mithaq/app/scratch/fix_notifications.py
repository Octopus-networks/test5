import re
import os

# 1. Modify MainActivity.kt
main_activity_path = r'c:\New folder (2)\app\src\main\java\com\mithaq\app\MainActivity.kt'
with open(main_activity_path, 'r', encoding='utf-8') as f:
    main_content = f.read()

channel_code = """        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "mithaq_alerts_channel_v4",
                "Mithaq Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }"""
main_content = main_content.replace('        super.onCreate(savedInstanceState)', channel_code, 1)

with open(main_activity_path, 'w', encoding='utf-8') as f:
    f.write(main_content)
print("MainActivity updated.")

# 2. Modify AndroidManifest.xml
manifest_path = r'c:\New folder (2)\app\src\main\AndroidManifest.xml'
with open(manifest_path, 'r', encoding='utf-8') as f:
    manifest_content = f.read()

receiver_code = """        <receiver
            android:name=".receiver.AdhanReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>"""
manifest_content = re.sub(
    r'<receiver\s*android:name="\.receiver\.AdhanReceiver"[^>]*/>',
    receiver_code,
    manifest_content,
    flags=re.MULTILINE
)
with open(manifest_path, 'w', encoding='utf-8') as f:
    f.write(manifest_content)
print("AndroidManifest updated.")

# 3. Modify AdhanScheduler.kt
scheduler_path = r'c:\New folder (2)\app\src\main\java\com\mithaq\app\util\AdhanScheduler.kt'
with open(scheduler_path, 'r', encoding='utf-8') as f:
    scheduler_content = f.read()

scheduler_save_loc = """        val coordinates = Coordinates(lat, lng)
        val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("adhan_lat", lat.toFloat()).putFloat("adhan_lng", lng.toFloat()).apply()
"""
scheduler_content = scheduler_content.replace('        val coordinates = Coordinates(lat, lng)', scheduler_save_loc, 1)
with open(scheduler_path, 'w', encoding='utf-8') as f:
    f.write(scheduler_content)
print("AdhanScheduler updated.")

# 4. Modify AdhanReceiver.kt
receiver_path = r'c:\New folder (2)\app\src\main\java\com\mithaq\app\receiver\AdhanReceiver.kt'
with open(receiver_path, 'r', encoding='utf-8') as f:
    receiver_content = f.read()

receiver_logic = """    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed. Rescheduling Adhan.")
            val prefs = context.getSharedPreferences("mithaq_prefs", Context.MODE_PRIVATE)
            val lat = prefs.getFloat("adhan_lat", 0.0f).toDouble()
            val lng = prefs.getFloat("adhan_lng", 0.0f).toDouble()
            if (lat != 0.0 && lng != 0.0) {
                AdhanScheduler.scheduleNextAdhan(context, lat, lng)
            }
            return
        }

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer\""""

receiver_content = receiver_content.replace('    override fun onReceive(context: Context, intent: Intent) {\n        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"', receiver_logic, 1)

with open(receiver_path, 'w', encoding='utf-8') as f:
    f.write(receiver_content)
print("AdhanReceiver updated.")
