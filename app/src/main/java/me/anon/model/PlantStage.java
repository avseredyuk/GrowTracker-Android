package me.anon.model;

import lombok.Getter;

/**
 * // TODO: Add class description
 *
 * @author 7LPdWcaW
 * @documentation // TODO Reference flow doc
 * @project GrowTracker
 */
public enum PlantStage
{
	GERMINATION("Germination"),
	VEGETATION("Vegetation"),
	FLOWER("Flower"),
	CURING("Curing");

	@Getter private String printString;

		private PlantStage(String name)
		{
			this.printString = name;
		}

		public static String[] names()
		{
			String[] names = new String[values().length];
			for (int index = 0; index < names.length; index++)
			{
				names[index] = values()[index].getPrintString();
			}

			return names;
		}
}
