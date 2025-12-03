package engine;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.IOException;

public final class FontPack {
    private final Font fontRegular;
    private final Font fontBig;
    private final Font fontSmall;
    private final Font fontSmallBig;

    private final FontMetrics regularMetrics;
    private final FontMetrics bigMetrics;
    private final FontMetrics smallMetrics;
    private final FontMetrics small1Metrics;

    public FontPack(Graphics graphics, FileManager fm) {
        try {
            fontRegular = fm.loadFont(18f);
            fontBig = fm.loadFont(30f);
            fontSmall = fm.loadFont(9f);
            fontSmallBig = fm.loadFont(13f);

            regularMetrics = graphics.getFontMetrics(fontRegular);
            bigMetrics = graphics.getFontMetrics(fontBig);
            smallMetrics = graphics.getFontMetrics(fontSmall);
            small1Metrics = graphics.getFontMetrics(fontSmallBig);
        } catch (IOException | FontFormatException e) {
            throw new RuntimeException("[FontPack] Failed to load fonts", e);
        }
    }
    public Font getRegular() {return fontRegular;}
    public Font getFontBig() {return fontBig;}
    public Font getFontSmall() {return fontSmall;}
    public Font getFontSmallBig() {return fontSmallBig;}

    public FontMetrics getRegularMetrics() {return regularMetrics;}
    public FontMetrics getBigMetrics() {return bigMetrics;}
    public FontMetrics getSmallMetrics() {return smallMetrics;}
    public FontMetrics getSmall1Metrics() {return small1Metrics;}
}
