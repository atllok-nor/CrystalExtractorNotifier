package com.crystalextractornotifier;

import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class CrystalExtractorNotifierOverlay extends Overlay
{
    private final CrystalExtractorNotifier plugin;
    private final CrystalExtractorNotifierConfig config;

    @Inject
    CrystalExtractorNotifierOverlay(CrystalExtractorNotifier plugin, CrystalExtractorNotifierConfig config)
    {
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        GameObject extractor = plugin.getExtractor();
        if (extractor == null)
        {
            return null;
        }

        Shape hull = extractor.getConvexHull();
        if (hull == null)
        {
            return null;
        }

        Color base = plugin.isReady() ? config.readyColor() : config.cooldownColor();
        Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), 60);

        g.setStroke(new BasicStroke(2));
        g.setColor(fill);
        g.fill(hull);

        g.setColor(base);
        g.draw(hull);

        return null;
    }
}