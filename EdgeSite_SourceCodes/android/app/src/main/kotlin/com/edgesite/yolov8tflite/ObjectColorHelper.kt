package com.edgesite.yolov8tflite

import android.graphics.Color

object ObjectColorHelper {

    
    fun colorForLabel(label: String): Int = when (label.lowercase().trim()) {
        "person"       -> Color.RED
        "car"          -> Color.BLUE
        "truck"        -> Color.parseColor("#1565C0") 
        "bus"          -> Color.parseColor("#0288D1") 
        "motorcycle"   -> Color.parseColor("#6A1B9A") 
        "bicycle"      -> Color.parseColor("#AD1457") 
        "dog"          -> Color.YELLOW
        "cat"          -> Color.parseColor("#FF9800")
        "bird"         -> Color.parseColor("#76FF03")
        "horse"        -> Color.parseColor("#8D6E63")
        else           -> Color.GRAY
    }

    fun markerHueForLabel(label: String): Float = when (label.lowercase().trim()) {
        "person"       -> 0f        
        "car"          -> 240f      
        "truck"        -> 210f      
        "bus"          -> 190f      
        "motorcycle"   -> 290f      
        "bicycle"      -> 330f      
        "dog"          -> 60f      
        "cat"          -> 30f      
        "bird"         -> 100f    
        "horse"        -> 20f     
        else           -> 180f    
    }

    fun emojiForLabel(label: String): String = when (label.lowercase().trim()) {
        "person"       -> "🔴 Person"
        "car"          -> "🔵 Car"
        "truck"        -> "🔵 Truck"
        "bus"          -> "🔵 Bus"
        "motorcycle"   -> "🟣 Motorcycle"
        "bicycle"      -> "🟣 Bicycle"
        "dog"          -> "🟡 Dog"
        "cat"          -> "🟠 Cat"
        "bird"         -> "🟢 Bird"
        else           -> "⚪ ${label.replaceFirstChar { it.uppercase() }}"
    }
}
