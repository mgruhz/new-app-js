package com.quizaruim.chat.components.url

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeeplinkChecker {
    suspend fun getSubs(campaign: String) : HashMap<Int, String> = withContext(Dispatchers.IO){
        val map = hashMapOf<Int, String>()
        if(campaign.contains("_")){
            val char_array = campaign.split("_")
            if(char_array.size < 5) {
                for (item in char_array.indices){
                    map[item] = char_array[item]
                }
            }
        }
        return@withContext map
    }
}