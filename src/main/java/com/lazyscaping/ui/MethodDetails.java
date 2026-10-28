package com.lazyscaping.ui;

import com.lazyscaping.api.Material;
import com.lazyscaping.api.MethodGuide;
import com.lazyscaping.api.TrainingMethod;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** A training method's effort, cost, notes, materials and how-to guide, shown when its row is expanded. */
final class MethodDetails
{
	private static final int INDENT = 8;
	static final int WIDTH = Components.TEXT_WIDTH - INDENT;

	private final JPanel panel = Components.column();
	private final IconLoader icons;

	private MethodDetails(IconLoader icons)
	{
		this.icons = icons;
		panel.setBorder(new EmptyBorder(4, INDENT, 2, 0));
	}

	/** {@code button} picks or unpicks the method, and may be null. */
	static JPanel create(TrainingMethod method, PanelActions actions, IconLoader icons, JButton button)
	{
		MethodDetails details = new MethodDetails(icons);
		details.addSummary(method);
		if (method.getMaterials() != null)
		{
			details.addMaterials("Uses each time", method.getMaterials().getInputs());
			details.addMaterials("Makes each time", method.getMaterials().getOutputs());
		}
		if (method.getGuide() != null)
		{
			details.addGuide(method.getGuide(), actions);
		}
		if (button != null)
		{
			details.panel.add(Components.gap(4));
			details.panel.add(Components.fullWidth(button));
		}
		return details.panel;
	}

	private void addSummary(TrainingMethod method)
	{
		addText(Format.effort(method.getAfk()) + " · " + Format.gpPerXp(method.getGpPerXp())
			+ (method.isMembers() ? " · Members" : "")
			+ (method.isIronFriendly() ? "" : " · Hard for ironmen"), 0);
		if (method.getNotes() != null)
		{
			addText(Html.escape(method.getNotes()), 4);
		}
	}

	private void addMaterials(String title, List<Material> materials)
	{
		if (materials.isEmpty())
		{
			return;
		}

		addText(Html.white(title + ":"), 4);
		for (Material material : materials)
		{
			JLabel picture = Components.picture();
			icons.loadItem(material, material.getQty(), false, picture);
			String text = (material.getQty() > 1 ? material.getQty() + " x " : "") + Html.escape(material.getName());
			panel.add(Components.pictureRow(picture, text));
		}
	}

	private void addGuide(MethodGuide guide, PanelActions actions)
	{
		if (guide.getWhere() != null)
		{
			addText(Html.white("Where:") + " " + Html.escape(guide.getWhere()), 4);
		}
		addList("Getting there", guide.getTravel(), false);
		addList("Bring", guide.getBring(), false);
		addList("Needs", guide.getNeeds(), false);
		addList("How to", guide.getSteps(), true);
		if (guide.getWiki() != null)
		{
			panel.add(Components.spaced(Components.link("Full guide on the OSRS Wiki", () -> actions.openWiki(guide.getWiki())), 4));
		}
	}

	private void addList(String title, List<String> items, boolean numbered)
	{
		if (items.isEmpty())
		{
			return;
		}

		StringBuilder html = new StringBuilder(Html.white(title + ":"));
		for (int i = 0; i < items.size(); i++)
		{
			html.append("<br>").append(numbered ? (i + 1) + ". " : "- ").append(Html.escape(items.get(i)));
		}
		addText(html.toString(), 4);
	}

	private void addText(String html, int top)
	{
		panel.add(Components.spaced(Components.label(html, WIDTH), top));
	}
}
