package com.example.tts

import com.example.model.ClockLanguage
import java.util.Calendar

object NaturalTimeFormatter {

    fun formatSpokenTime(
        calendar: Calendar,
        language: ClockLanguage,
        is24Hour: Boolean,
        announceAmPm: Boolean
    ): String {
        return when (language) {
            ClockLanguage.ENGLISH -> formatEnglish(calendar, is24Hour, announceAmPm)
            ClockLanguage.BENGALI -> formatBengali(calendar, is24Hour, announceAmPm)
            ClockLanguage.HINDI -> formatHindi(calendar, is24Hour, announceAmPm)
        }
    }

    // ==========================================
    // ENGLISH
    // ==========================================
    private fun formatEnglish(calendar: Calendar, is24Hour: Boolean, announceAmPm: Boolean): String {
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val hour12 = (hour24 % 12).let { if (it == 0) 12 else it }
        val minute = calendar.get(Calendar.MINUTE)
        val isAm = hour24 < 12

        if (is24Hour) {
            val hourStr = String.format("%02d", hour24)
            val minStr = String.format("%02d", minute)
            return "The time is now $hourStr $minStr hours."
        }

        val hourWords = arrayOf(
            "", "one", "two", "three", "four", "five", "six", "seven",
            "eight", "nine", "ten", "eleven", "twelve"
        )
        val hourWord = hourWords.getOrElse(hour12) { "$hour12" }
        val amPmStr = if (announceAmPm) (if (isAm) "AM" else "PM") else ""

        return when (minute) {
            0 -> "The time is now $hourWord o'clock $amPmStr".trim()
            in 1..9 -> "The time is now $hourWord oh ${numberToEnglish(minute)} $amPmStr".trim()
            else -> "The time is now $hourWord ${numberToEnglish(minute)} $amPmStr".trim()
        }
    }

    private fun numberToEnglish(num: Int): String {
        if (num <= 0) return "zero"
        val units = arrayOf(
            "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
            "seventeen", "eighteen", "nineteen"
        )
        val tens = arrayOf("", "", "twenty", "thirty", "forty", "fifty")

        return when {
            num < 20 -> units[num]
            num % 10 == 0 -> tens[num / 10]
            else -> "${tens[num / 10]} ${units[num % 10]}"
        }
    }

    // ==========================================
    // BENGALI (বাংলা)
    // ==========================================
    private fun formatBengali(calendar: Calendar, is24Hour: Boolean, announceAmPm: Boolean): String {
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val hour12 = (hour24 % 12).let { if (it == 0) 12 else it }
        val minute = calendar.get(Calendar.MINUTE)

        if (is24Hour) {
            val hWord = bengaliNumber(hour24)
            val mWord = bengaliNumber(minute)
            return "এখন সময় $hWord টা $mWord মিনিট।"
        }

        // Time of day in Bengali
        val period = if (announceAmPm) {
            when (hour24) {
                in 4..11 -> "সকাল"
                in 12..15 -> "দুপুর"
                in 16..17 -> "বিকেল"
                in 18..19 -> "সন্ধ্যা"
                else -> "রাত"
            }
        } else {
            ""
        }

        val bengaliHours = arrayOf(
            "", "একটা", "দুটো", "তিনটে", "চারটে", "পাঁচটা", "ছটা",
            "সাতটা", "আটটা", "নটা", "দশটা", "এগারোটা", "বারোটা"
        )
        val hourWord = bengaliHours.getOrElse(hour12) { "${bengaliNumber(hour12)} টা" }

        val prefix = if (period.isNotEmpty()) "এখন সময় $period $hourWord" else "এখন সময় $hourWord"

        return if (minute == 0) {
            "$prefix বাজে।"
        } else {
            val minWord = bengaliNumber(minute)
            "$prefix $minWord মিনিট।"
        }
    }

    private fun bengaliNumber(n: Int): String {
        val words = arrayOf(
            "শূন্য", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়", "দশ",
            "এগারো", "বারো", "তেরো", "চোদ্দ", "পনেরো", "ষোল", "সতেরো", "আঠারো", "উনিশ", "কুড়ি",
            "একুশ", "বাইশ", "তেইশ", "চব্বিশ", "পঁচিশ", "ছাব্বিশ", "সাতাশ", "আঠাশ", "ঊনত্রিশ", "ত্রিশ",
            "একত্রিশ", "বত্রিশ", "তেত্রিশ", "চৌত্রিশ", "পঁয়ত্রিশ", "ছত্রিশ", "সাঁইত্রিশ", "আটত্রিশ", "ঊনচল্লিশ", "চল্লিশ",
            "একচল্লিশ", "বিয়াল্লিশ", "তেতাল্লিশ", "চুয়াল্লিশ", "পঁয়তাল্লিশ", "ছেচল্লিশ", "সাতচল্লিশ", "আটচল্লিশ", "ঊনপঞ্চাশ", "পঞ্চাশ",
            "একান্ন", "বায়ান্ন", "তিপ্পান্ন", "চুয়ান্ন", "পঞ্চান্ন", "ছাপ্পান্ন", "সাতান্ন", "আটান্ন", "ঊনষাট", "ষাট"
        )
        return if (n in 0..60) words[n] else "$n"
    }

    // ==========================================
    // HINDI (हिन्दी)
    // ==========================================
    private fun formatHindi(calendar: Calendar, is24Hour: Boolean, announceAmPm: Boolean): String {
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val hour12 = (hour24 % 12).let { if (it == 0) 12 else it }
        val minute = calendar.get(Calendar.MINUTE)

        if (is24Hour) {
            val hWord = hindiNumber(hour24)
            val mWord = hindiNumber(minute)
            return "अभी $hWord बजकर $mWord मिनट हुए हैं।"
        }

        // Time of day in Hindi
        val period = if (announceAmPm) {
            when (hour24) {
                in 4..11 -> "सुबह"
                in 12..15 -> "दोपहर"
                in 16..19 -> "शाम"
                else -> "रात"
            }
        } else {
            ""
        }

        val hindiHours = arrayOf(
            "", "एक", "दो", "तीन", "चार", "पाँच", "छह",
            "सात", "आठ", "नौ", "दस", "ग्यारह", "बारह"
        )
        val hourWord = hindiHours.getOrElse(hour12) { hindiNumber(hour12) }

        val periodPrefix = if (period.isNotEmpty()) " $period" else ""

        return if (minute == 0) {
            "अभी$periodPrefix पूरे $hourWord बजे हैं।"
        } else {
            val minWord = hindiNumber(minute)
            "अभी$periodPrefix $hourWord बजकर $minWord मिनट हुए हैं।"
        }
    }

    private fun hindiNumber(n: Int): String {
        val words = arrayOf(
            "शून्य", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस",
            "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस", "बीस",
            "इक्कीस", "बाईस", "तेईस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताईस", "अट्ठाईस", "उनतीस", "तीस",
            "इकतीस", "बत्तीस", "तैंतीस", "चौंतीस", "पैंतीस", "छत्तीस", "सैंतीस", "अड़तीस", "उनतालीस", "चालीस",
            "इकतालीस", "बयालीस", "तैंतालीस", "चौवालीस", "पैंतालीस", "छियालीस", "सैंतालीस", "अड़तालीस", "उनचास", "पचास",
            "इक्यावन", "बावन", "तिरेपन", "चौवन", "पचपन", "छप्पन", "सत्तावन", "अट्ठावन", "उनसठ", "साठ"
        )
        return if (n in 0..60) words[n] else "$n"
    }
}
