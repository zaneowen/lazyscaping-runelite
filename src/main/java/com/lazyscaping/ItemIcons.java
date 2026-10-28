package com.lazyscaping;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Item pictures for the gear setup: the version you own if you have one (like
 * an ornament kit), else the game's picture by item id, else lazyscaping.app's
 * picture for untradeable items. Items you don't have yet are faded.
 */
@Singleton
class ItemIcons implements LazyScapingPanel.Icons
{
	private static final int WIDTH = 36;
	private static final int HEIGHT = 32;
	private static final float FADED = 0.45f;

	@Inject
	private ItemManager itemManager;

	@Inject
	private LazyScapingApi api;

	@Inject
	private OwnedItems owned;

	/** The site's pictures by path, each fetched once. */
	private final Map<String, CompletableFuture<BufferedImage>> siteImages = new ConcurrentHashMap<>();

	@Override
	public void load(LazyScapingApi.GearItem item, boolean dimmed, JLabel target)
	{
		Integer ownedId = owned.itemId(item.id);
		Integer itemId = ownedId != null ? ownedId : item.itemId;
		if (itemId != null)
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			image.onLoaded(() -> show(image, dimmed, target));
			return;
		}
		if (item.icon != null)
		{
			siteImages.computeIfAbsent(item.icon, this::fetch).thenAccept(image ->
			{
				if (image != null)
				{
					show(image, dimmed, target);
				}
			});
		}
	}

	void clear()
	{
		siteImages.clear();
	}

	private CompletableFuture<BufferedImage> fetch(String path)
	{
		CompletableFuture<BufferedImage> future = new CompletableFuture<>();
		api.image(path, image -> future.complete(fit(image)), () ->
		{
			// Try again next time it's shown.
			siteImages.remove(path);
			future.complete(null);
		});
		return future;
	}

	/** Wiki pictures vary in size; fit them to an inventory slot. */
	private static BufferedImage fit(BufferedImage image)
	{
		return image.getWidth() > WIDTH || image.getHeight() > HEIGHT ? ImageUtil.resizeImage(image, WIDTH, HEIGHT, true) : image;
	}

	private static void show(BufferedImage image, boolean dimmed, JLabel target)
	{
		ImageIcon icon = new ImageIcon(dimmed ? faded(image) : image);
		SwingUtilities.invokeLater(() -> target.setIcon(icon));
	}

	private static BufferedImage faded(BufferedImage image)
	{
		BufferedImage faded = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = faded.createGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, FADED));
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return faded;
	}
}
