package com.mustime

import com.mustime.features.rooms.BuildingLevel
import com.mustime.features.rooms.UniversityDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FloorIndicatorResolutionTest {

    @Test
    fun testDirectoryRoomResolutionAllFloors() {
        // Ground floor rooms from authoritative directory
        assertEquals(BuildingLevel.GROUND, UniversityDirectory.resolveLevel("SG05"))
        assertEquals(BuildingLevel.GROUND, UniversityDirectory.resolveLevel("SG06"))
        assertEquals(BuildingLevel.GROUND, UniversityDirectory.resolveLevel("IMSG01"))
        assertEquals(BuildingLevel.GROUND, UniversityDirectory.resolveLevel("IMSG04"))
        assertEquals(BuildingLevel.GROUND, UniversityDirectory.resolveLevel("PLT"))
        assertEquals(BuildingLevel.GROUND, UniversityDirectory.resolveLevel("MLT"))
        assertEquals(BuildingLevel.GROUND, UniversityDirectory.resolveLevel("Pharmacy L1"))

        // First floor rooms
        assertEquals(BuildingLevel.FIRST, UniversityDirectory.resolveLevel("S103"))
        assertEquals(BuildingLevel.FIRST, UniversityDirectory.resolveLevel("IMS102"))
        assertEquals(BuildingLevel.FIRST, UniversityDirectory.resolveLevel("IMS103"))
        assertEquals(BuildingLevel.FIRST, UniversityDirectory.resolveLevel("Pharmacy L2"))

        // Second floor rooms
        assertEquals(BuildingLevel.SECOND, UniversityDirectory.resolveLevel("S204"))
        assertEquals(BuildingLevel.SECOND, UniversityDirectory.resolveLevel("S205"))
        assertEquals(BuildingLevel.SECOND, UniversityDirectory.resolveLevel("S210"))
        assertEquals(BuildingLevel.SECOND, UniversityDirectory.resolveLevel("S211"))
        assertEquals(BuildingLevel.SECOND, UniversityDirectory.resolveLevel("Pharmacy L3"))

        // Third floor rooms
        assertEquals(BuildingLevel.THIRD, UniversityDirectory.resolveLevel("S310"))
        assertEquals(BuildingLevel.THIRD, UniversityDirectory.resolveLevel("S311"))
        assertEquals(BuildingLevel.THIRD, UniversityDirectory.resolveLevel("Postgraduate Room 1"))

        // Fourth floor room
        assertEquals(BuildingLevel.FOURTH, UniversityDirectory.resolveLevel("S403"))
    }

    @Test
    fun testSmartHeuristicFallbackForCustomRooms() {
        // Ground heuristics
        assertEquals(BuildingLevel.GROUND, UniversityDirectory.resolveLevel("Block A Ground Floor"))
        assertEquals(BuildingLevel.GROUND, UniversityDirectory.resolveLevel("Grnd Hall"))
        assertEquals(BuildingLevel.GROUND, UniversityDirectory.resolveLevel("GF Lab"))

        // First floor heuristics
        assertEquals(BuildingLevel.FIRST, UniversityDirectory.resolveLevel("Room 101"))
        assertEquals(BuildingLevel.FIRST, UniversityDirectory.resolveLevel("1st Floor Conference"))
        assertEquals(BuildingLevel.FIRST, UniversityDirectory.resolveLevel("Level 1 Room"))

        // Second floor heuristics
        assertEquals(BuildingLevel.SECOND, UniversityDirectory.resolveLevel("Room 205"))
        assertEquals(BuildingLevel.SECOND, UniversityDirectory.resolveLevel("2nd Floor Seminar"))

        // Third floor heuristics
        assertEquals(BuildingLevel.THIRD, UniversityDirectory.resolveLevel("Room 304"))
        assertEquals(BuildingLevel.THIRD, UniversityDirectory.resolveLevel("3rd Floor Lab"))

        // Fourth floor heuristics
        assertEquals(BuildingLevel.FOURTH, UniversityDirectory.resolveLevel("Room 402"))
        assertEquals(BuildingLevel.FOURTH, UniversityDirectory.resolveLevel("4th Floor"))
    }

    @Test
    fun testInvalidAndBlankVenueReturnsNull() {
        assertNull(UniversityDirectory.resolveLevel(null))
        assertNull(UniversityDirectory.resolveLevel(""))
        assertNull(UniversityDirectory.resolveLevel("   "))
        assertNull(UniversityDirectory.resolveLevel("TBA"))
        assertNull(UniversityDirectory.resolveLevel("TBD"))
        assertNull(UniversityDirectory.resolveLevel("NONE"))
        assertNull(UniversityDirectory.resolveLevel("N/A"))
        assertNull(UniversityDirectory.resolveLevel("Off Campus Grounds"))
    }

    @Test
    fun testBuildingLevelEnumValues() {
        assertEquals("Ground Floor", BuildingLevel.GROUND.displayName)
        assertEquals("Ground", BuildingLevel.GROUND.shortName)
        assertEquals("1st Floor", BuildingLevel.FIRST.shortName)
        assertEquals("2nd Floor", BuildingLevel.SECOND.shortName)
        assertEquals("3rd Floor", BuildingLevel.THIRD.shortName)
        assertEquals("4th Floor", BuildingLevel.FOURTH.shortName)
    }
}
