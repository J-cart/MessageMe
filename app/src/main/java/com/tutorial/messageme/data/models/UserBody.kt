package com.tutorial.messageme.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserBody(
    val uid:String = "",
    val userName:String = "",
    val fName:String = "",
    val lName:String = "",
    val phoneNo:String = "",
    val email:String = "",
    val displayImg:String = "",
    val userStatus:String = "",
    val dob:String = "",
    val gender:String = "",
    val deviceToken: List<String> = emptyList()
):Parcelable