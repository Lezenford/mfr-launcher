package com.lezenford.mfr.launcher.extension

import java.util.*

object ModifyFiles {
    const val fileDirectory = "Data Files"
    val esmFileList: List<Pair<String, Long>> = listOf(
        "Morrowind.esm" to convertDateToLong(2002, 1, 1),
        "Tribunal.esm" to convertDateToLong(2003, 1, 1),
        "Bloodmoon.esm" to convertDateToLong(2004, 1, 1),
        "Tamriel_Data.esm" to convertDateToLong(2005, 1, 1),
        "MFR.esm" to convertDateToLong(2006, 1, 1),
        "Cyr_Main.esm" to convertDateToLong(2011, 1, 1),
        "Sky_Main.esm" to convertDateToLong(2012, 1, 1),
        "TR_Mainland.esm" to convertDateToLong(2020, 1, 1),
        "TR_Factions.esp" to convertDateToLong(2030, 1, 1),
        "MFR_TR_patch.esp" to convertDateToLong(2050, 1, 1),
    )

    private fun convertDateToLong(year: Int, month: Int, day: Int): Long {
        Calendar.getInstance().also {
            it.set(year, month - 1, day)
            return it.timeInMillis
        }
    }

}