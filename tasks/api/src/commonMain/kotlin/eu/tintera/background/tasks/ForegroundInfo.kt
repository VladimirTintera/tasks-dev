package eu.tintera.background.tasks

data class ForegroundInfo(
    val channelId: String,
    val channelName: String,
    val notificationId: Int,
    val notificationTitle: String,
    val notificationIcon: Int
)