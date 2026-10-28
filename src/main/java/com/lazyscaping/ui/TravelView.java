package com.lazyscaping.ui;

import com.lazyscaping.api.Teleport;
import com.lazyscaping.api.Travel;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** How to get to a boss: requirements, the plan's teleports, other routes and the wiki strategy guide. */
final class TravelView
{
	private TravelView()
	{
	}

	static void addTo(JPanel parent, Travel travel, PanelActions actions, IconLoader icons)
	{
		if (travel == null || travel.isEmpty())
		{
			return;
		}

		parent.add(Components.subheading("Getting there"));
		if (travel.getQuest() != null)
		{
			parent.add(Components.spaced(Components.label(Html.white("Quest:") + " " + Html.escape(travel.getQuest())), 4));
		}
		if (travel.getAccess() != null)
		{
			parent.add(Components.spaced(Components.label(Html.white("Needs:") + " " + Html.escape(travel.getAccess())), 4));
		}

		for (Teleport teleport : travel.getTeleports())
		{
			JLabel picture = Components.picture();
			icons.loadItem(teleport, 1, false, picture);
			parent.add(Components.pictureRow(picture, Html.escape(teleport.getName()) + "<br>" + Html.muted(Html.escape(teleport.getWhere()))));
		}

		if (!travel.getRoutes().isEmpty())
		{
			StringBuilder html = new StringBuilder(Html.white("Other ways:"));
			travel.getRoutes().forEach(route -> html.append("<br>- ").append(Html.escape(route)));
			parent.add(Components.spaced(Components.label(html.toString()), 6));

			String tip = (travel.isFairyRings() ? "Fairy rings need a dramen staff until you finish the elite Lumbridge & Draynor Diary. " : "")
				+ "In your house, a portal nexus or jewellery box" + (travel.isFairyRings() ? " (or fairy ring)" : "")
				+ " turns one house tablet into most of these teleports.";
			parent.add(Components.spaced(Components.label(Html.muted(Html.escape(tip))), 4));
		}

		if (travel.getWiki() != null)
		{
			parent.add(Components.spaced(Components.link("Strategy guide on the OSRS Wiki", () -> actions.openWiki(travel.getWiki())), 6));
		}
	}
}
