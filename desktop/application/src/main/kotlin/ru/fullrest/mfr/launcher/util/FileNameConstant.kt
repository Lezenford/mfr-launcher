package ru.fullrest.mfr.launcher.util

import java.util.*

object FileNameConstant {
    val esmFileList: List<Pair<String, Long>> = listOf(
        "Morrowind.esm" to convertDateToLong(2002, 9, 23),
        "Tribunal.esm" to convertDateToLong(2003, 7, 15),
        "Bloodmoon.esm" to convertDateToLong(2003, 7, 28),
        "Tamriel_Data.esm" to convertDateToLong(2011, 7, 18),
        "TR_Mainland.esm" to convertDateToLong(2012, 7, 20),
        "TR_Preview.esm" to convertDateToLong(2013, 7, 21),
        "Sky_Main.esm" to convertDateToLong(2014, 7, 21),
        "MFR.esm" to convertDateToLong(2020, 7, 24),
        "MFR_patch.esp" to convertDateToLong(2030, 12, 12),
        "TR_patch.esp" to convertDateToLong(2030, 12, 12),
        "MFR_TR_Patch.esp" to convertDateToLong(2022, 12, 12),
        "MFR_Update.esp" to convertDateToLong(2030, 12, 12)
    )

    private fun convertDateToLong(year: Int, month: Int, day: Int): Long {
        Calendar.getInstance().also {
            it.set(year, month - 1, day)
            return it.timeInMillis
        }
    }

}