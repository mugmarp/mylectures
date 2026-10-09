package com.mustime

import com.mustime.features.rooms.Campus
import com.mustime.features.rooms.ui.campusForProgramme
import com.mustime.features.rooms.ui.programmeDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProgrammeLocationDefaultsTest {

    @Test
    fun `FCI programs default to Kihumuro Campus and FCI Building`() {
        val fciPrograms = listOf("BCS", "BIT", "BSE", "BCS I", "BCS II", "BIT III", "BSE IV")
        for (prog in fciPrograms) {
            val def = programmeDefaults(prog)
            assertEquals("Programme $prog must default to Kihumuro Campus", Campus.KIHUMURO.displayName, def.campus)
            assertEquals("Programme $prog must default to FCI Building", "FCI", def.buildingCode)
            assertEquals("Programme $prog buildingName must be FCI Building", "FCI Building", def.buildingName)
        }
    }

    @Test
    fun `FAST programs default to Kihumuro Campus and FAST Building`() {
        val fastPrograms = listOf("BME", "EEE", "PEEM", "CVE", "MIE", "BME I", "EEE II", "CVE III")
        for (prog in fastPrograms) {
            val def = programmeDefaults(prog)
            assertEquals("Programme $prog must default to Kihumuro Campus", Campus.KIHUMURO.displayName, def.campus)
            assertEquals("Programme $prog must default to FAST Building", "FAST", def.buildingCode)
            assertEquals("Programme $prog buildingName must be FAST Building", "FAST Building", def.buildingName)
        }
    }

    @Test
    fun `Science programs default to Town Campus and Science Block`() {
        val sciencePrograms = listOf("BS", "DLT", "BS I", "BS II", "DLT I", "BS BIOLOGICAL I", "DLT CHEMISTRY II", "BS PHYSICS III")
        for (prog in sciencePrograms) {
            val def = programmeDefaults(prog)
            assertEquals("Programme $prog must default to Town Campus", Campus.TOWN.displayName, def.campus)
            assertEquals("Programme $prog must default to Science Block", "SCI", def.buildingCode)
            assertEquals("Programme $prog buildingName must be Science Block", "Science Block", def.buildingName)
        }
    }

    @Test
    fun `Medicine and Pharmacy programs default to Town Campus and Pharmacy Complex`() {
        val medPrograms = listOf("PHA", "PHS", "MBR", "BNS", "MLS", "BSP", "DCM", "DEM", "DCAM", "MBR III", "PHA I")
        for (prog in medPrograms) {
            val def = programmeDefaults(prog)
            assertEquals("Programme $prog must default to Town Campus", Campus.TOWN.displayName, def.campus)
            assertEquals("Programme $prog must default to Pharmacy / Health Complex", "PHA", def.buildingCode)
            assertEquals("Programme $prog buildingName must be Pharmacy / Health Complex", "Pharmacy / Health Complex", def.buildingName)
        }
    }

    @Test
    fun `Business programs default to Town Campus and IMS Building`() {
        val businessPrograms = listOf("BBA", "BSAF", "ECO", "BPSM", "BBA I", "BSAF II", "ECO III")
        for (prog in businessPrograms) {
            val def = programmeDefaults(prog)
            assertEquals("Programme $prog must default to Town Campus", Campus.TOWN.displayName, def.campus)
            assertEquals("Programme $prog must default to IMS / Business Building", "IMS", def.buildingCode)
        }
    }

    @Test
    fun `Interdisciplinary programs default to Town Campus and IMS Building`() {
        val fisPrograms = listOf("BSAL", "BGWH", "BPCD", "BSAL I", "BGWH III")
        for (prog in fisPrograms) {
            val def = programmeDefaults(prog)
            assertEquals("Programme $prog must default to Town Campus", Campus.TOWN.displayName, def.campus)
            assertEquals("Programme $prog must default to IMS / Business Building", "IMS", def.buildingCode)
        }
    }

    @Test
    fun `Null or empty programme falls back safely without crash`() {
        val defNull = programmeDefaults(null)
        assertNotNull(defNull)
        assertEquals(Campus.KIHUMURO.displayName, defNull.campus)
        assertNull(defNull.buildingCode)

        val defBlank = programmeDefaults("   ")
        assertNotNull(defBlank)
        assertEquals(Campus.KIHUMURO.displayName, defBlank.campus)
        assertNull(defBlank.buildingCode)
    }

    @Test
    fun `campusForProgramme helper matches default campus`() {
        assertEquals(Campus.KIHUMURO.displayName, campusForProgramme("BIT I"))
        assertEquals(Campus.KIHUMURO.displayName, campusForProgramme("BME II"))
        assertEquals(Campus.TOWN.displayName, campusForProgramme("MBR III"))
        assertEquals(Campus.TOWN.displayName, campusForProgramme("BS I"))
    }
}
