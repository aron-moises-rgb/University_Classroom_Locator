package com.example.oopproject.backEnd.constants

data class BuildingConstant(
    val id: String,
    val name: String,
    val code: String,
    val totalFloors: Int = 2
)

object CampusBuildings {
    val HEB = BuildingConstant("bldg_heb", "HEB Building", "HEB", 3)
    val CTEB = BuildingConstant("bldg_cteb", "CTEB Building", "CTEB", 4)
    val TAAL = BuildingConstant("bldg_taal", "TAAL Building", "TAAL", 4)
    val LEMERY = BuildingConstant("bldg_lemery", "LEMERY Building", "LEMERY", 2)
    val CAS = BuildingConstant("bldg_cas", "CAS Building", "CAS", 2)

    val ALL = listOf(HEB, CTEB, TAAL, LEMERY, CAS)

    val COUNT = ALL.size

    fun names(): List<String> = ALL.map { it.name }

    fun findByName(name: String): BuildingConstant? {
        val clean = name.trim().lowercase()
        return ALL.find { 
            it.name.lowercase() == clean || 
            it.code.lowercase() == clean ||
            clean.contains(it.code.lowercase()) ||
            it.name.lowercase().contains(clean)
        }
    }
}
