package com.tutorial.messageme.data.models

data class PushNotifierBody<T> (val to :String, val body:T)