package com.quizaruim.chat.notification
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.quizaruim.chat.R
import com.quizaruim.chat.activities.splash.SplashActivity

class PushNotification(private val context: Context) {
    private val logTag = "pushNotification"
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    fun inAppNotification(title: String, description: String, image: String) {
        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, context.resources.getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("text")
            .setContentText("content")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        if (image.isEmpty()) {
            with(NotificationManagerCompat.from(context)) {
                notify(997, builder.build())
            }
            return
        }
        val loader = ImageLoader(context)
        val modelClass = DataModel(title, description, image, "")
        val req = ImageRequest.Builder(context)
            .data(modelClass.image)
            .target { result ->
                val bitmap = (result as BitmapDrawable).bitmap
                builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                with(NotificationManagerCompat.from(context)) {
                    notify(997, builder.build())
                }
            }
            .build()
        loader.enqueue(req)
    }
    fun startNotification(dataModelClass: DataModel, builder: NotificationCompat.Builder, image: Bitmap, id: Int) {
        val notification = createNotification(
            builder,
            dataModelClass,
            image,
            id
        )
        Log.d(logTag, "Notifying the manager about notification: $id")
        notificationManager.notify(id, notification)
    }
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createNotification(builder: NotificationCompat.Builder, payLoad: DataModel, image: Bitmap, id: Int): Notification {
        val i = pushIntent(payLoad.link)
        val pendingActionIntent = PendingIntent.getActivity(
            context,
            id,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(false)
            .setAutoCancel(true)
        if (id == 0) {
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setVibrate(longArrayOf(1000, 1000, 1000))
        }
        builder.setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentTitle(payLoad.title)
            .setContentText(payLoad.description)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(image))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingActionIntent)
        Log.d(logTag, "Now builds a notification with id: $id and link: ${payLoad.link}")
        return builder.build()
    }
    private fun pushIntent(link: String): Intent {
        val packageName = "com.android.chrome"
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        i.setPackage(packageName)
        return i
    }
}