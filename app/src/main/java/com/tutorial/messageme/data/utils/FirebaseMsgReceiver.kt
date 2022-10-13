package com.tutorial.messageme.data.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tutorial.messageme.R
import com.tutorial.messageme.data.arch.ChatsRepository
import com.tutorial.messageme.data.models.UserBody
import com.tutorial.messageme.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@JvmField
val VERBOSE_NOTIFICATION_CHANNEL_NAME: CharSequence =
    "Verbose FCM Notifications"
const val VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION =
    "Shows notifications whenever FCM sends messages"

@JvmField
val NOTIFICATION_TITLE: CharSequence = "FCM TESTING"
const val CHANNEL_ID = "VERBOSE_NOTIFICATION"

private val fStoreUsers = Firebase.firestore.collection(USERS)

@AndroidEntryPoint
class FirebaseMsgReceiver : FirebaseMessagingService() {

    @Inject
    lateinit var repository:ChatsRepository

   private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        //TODO ...FIX..1 notification gets sent whether or not the user is logged in
        // FIX..2 find a way to add pending intent for FCM default notification
         makeStatusNotification(message.data.toString(), this)
        Log.d("CLOUD_MSG", "${message.data}")
        Log.d("CLOUD_MSG", "messageType ${message.data["messageType"]}")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        updateToken(token)
       /*
        scope.launch {
            try {
                val msgToken = FirebaseMessaging.getInstance().token.await()
                updateToken(msgToken)
                withContext(Dispatchers.Main){
                    this@FirebaseMsgReceiver.showToast("Token Update Successful(FCM-Instance(try-catch))")
                }
                Log.d("me_updateToken","Token Update Successful(FCM-Instance(try-catch))" )
            }catch (e:Exception){
                Log.d("me_updateToken","Token Update Successful(FCM-Instance(try-catch))" )
                withContext(Dispatchers.Main){
                    this@FirebaseMsgReceiver.showToast("Token Update FAILED {$e} (FCM-Instance(try-catch))")
                }
            }
        }
*/
        //repository.updateToken(token)
    }


  private fun updateToken(token: String) {
        Firebase.auth.currentUser?.let { currentUser ->
            currentUser.email?.let { email ->
                fStoreUsers.document(email).get().addOnCompleteListener { taskBody ->
                    when {
                        taskBody.isSuccessful -> {
                            taskBody.result.toObject<UserBody>()?.let { userBody ->
                                val tokenList = mutableListOf<String>()
                                tokenList.addAll(userBody.deviceToken)
                                tokenList.add(token)
                                fStoreUsers.document(email).update("deviceToken", tokenList)
                                    .addOnCompleteListener {
                                        if (it.isSuccessful) {
                                            this.showToast("Token Updated Successfully")
                                            Log.d("me_updateToken", " ${it.result}")
                                        } else {
                                            this.showToast("Error:Token Not Updated:->${it.exception}")
                                            Log.d("me_updateToken", " ${it.exception}")
                                        }

                                    }
                            }
                        }
                        else -> {
                            this.showToast("Error:Token Not Updated:->${taskBody.exception}")
                            Log.d("me_updateToken", " ${taskBody.exception}")
                        }
                    }
                }
            }
        }
    }

    private fun suspendUpdate(token: String) {
        scope.launch {
            try {
                Firebase.auth.currentUser?.let { currentUser ->
                    currentUser.email?.let { email ->
                        val usersDoc = fStoreUsers.document(email).get().await()
                        usersDoc.toObject<UserBody>()?.let { userBody ->
                            val tokenList = mutableListOf<String>()
                            tokenList.addAll(userBody.deviceToken)
                            tokenList.add(token)
                            fStoreUsers.document(email).update("deviceToken", tokenList).await()
                        }

                    }
                }
                withContext(Dispatchers.Main) {
                    this@FirebaseMsgReceiver.showToast("Token Updated Successfully")
                    Log.d("me_updateToken", " Token Update Successful")
                }


            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    this@FirebaseMsgReceiver.showToast("Error:Token Not Updated:->$e")
                    Log.d("me_updateToken", " $e")
                }

            }

        }

    }


    private fun makeStatusNotification(message: String, context: Context) {

        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
            val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description

            // Add the channel
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

            notificationManager?.createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).also {
                it.action = VIEW_CHAT
            },
            FLAG_UPDATE_CURRENT
        )

        // Create the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(LongArray(0))

        // Show the notification
        NotificationManagerCompat.from(context).notify(123, builder.build())
    }

}