package com.lazyscaping.ui;

import java.awt.Color;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Builds the small HTML snippets Swing labels render. Text from the site is always escaped. */
final class Html
{
	static final Color GREEN = new Color(0x7bc96f);
	static final Color GOLD = new Color(0xd9a23a);
	static final Color RED = new Color(0xe0605a);
	static final Color MUTED = new Color(0xa8a29e);

	private Html()
	{
	}

	static String escape(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** Plain text that's never split across lines; it moves to the next line whole instead. */
	static String keep(String text)
	{
		return escape(text).replace(" ", "&nbsp;");
	}

	/** Short plain-text parts separated by dots, wrapping only between parts. Empty parts are skipped. */
	static String parts(String... parts)
	{
		return Arrays.stream(parts)
			.filter(part -> part != null && !part.isEmpty())
			.map(Html::keep)
			.collect(Collectors.joining(" · "));
	}

	static String color(Color color, String html)
	{
		return String.format("<font color='#%06x'>%s</font>", color.getRGB() & 0xFFFFFF, html);
	}

	static String muted(String html)
	{
		return color(MUTED, html);
	}

	static String white(String html)
	{
		return color(Color.WHITE, html);
	}

	static String underline(String html)
	{
		return "<u>" + html + "</u>";
	}

	/** Wraps text to a width in CSS pixels. */
	static String wrap(String html, int width)
	{
		return "<html><body style='width:" + width + "px'>" + html + "</body></html>";
	}
}
