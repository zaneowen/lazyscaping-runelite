package com.lazyscaping;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Things in a player-owned house that can replace a teleport in a gear setup's inventory. */
@Getter
@RequiredArgsConstructor
public enum HouseTeleport
{
	FAIRY_RING("Fairy ring", "fairy_ring"),
	SPIRIT_TREE("Spirit tree", "spirit_tree"),
	JEWELLERY_BOX("Jewellery box", "jewellery_box"),
	XERICS_TALISMAN("Mounted Xeric's talisman", "xerics_talisman"),
	DIGSITE_PENDANT("Mounted digsite pendant", "digsite_pendant"),
	VARROCK("Varrock portal", "varrock_portal"),
	FALADOR("Falador portal", "falador_portal"),
	BARROWS("Barrows portal", "barrows_portal"),
	WATERBIRTH("Waterbirth portal", "waterbirth_portal"),
	TROLLHEIM("Trollheim portal", "trollheim_portal"),
	ANNAKARL("Annakarl portal", "annakarl_portal"),
	CARRALLANGER("Carrallanger portal", "carrallanger_portal"),
	GHORROCK("Ghorrock portal", "ghorrock_portal"),
	LASSAR("Lassar portal", "lassar_portal");

	private final String label;
	/** The name lazyscaping.app's API uses. */
	private final String apiName;

	@Override
	public String toString()
	{
		return label;
	}
}
