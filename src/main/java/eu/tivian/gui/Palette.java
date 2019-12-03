package eu.tivian.gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// based on https://www.colodore.com/
public class Palette implements Iterable<Color> {
    public double brightness = 50.0;
    public double contrast = 100.0;
    public double saturation = 50.0;

    private final double contrastBoost = 0.2; // for better contrast 1802 emulation
    private final double gammasrc = 2.8; // gamma correction of PAL
    private final double gammatgt = 2.2; // gamma correction of sRGB
    private final List<Color> palette = new ArrayList<>();

    private final int[] lumas = new int[] {
        32, 48, 64, 80, 120, 144, 192, 256
    };

    // color phase angles for PAL and NTSC taken from TED manual
    private final int[] angles = new int[] {
        //101, 281, 56, 236, 348, 169, 124, 146, 191, 79, 259, 326, 11, 214 // Colodore PAL
        103, 283, 53, 241, 347, 167, 129, 148, 195, 83, 265, 323, 23, 213 // PAL
        //70, 250, 20, 208, 314, 134, 90, 115, 162, 50, 232, 290, 350, 180 // NTSC
    };

    public Palette() {
        render();
    }

    public Color get(int i) {
        return palette.get(i);
    }

    public int size() {
        return palette.size();
    }

    private double gamma(double value) {
        value = Math.min(Math.max(value, 0), 255);

        double factor = Math.pow(255, 1.0 - gammasrc);
        value = Math.min(Math.max(factor * Math.pow(value, gammasrc), 0), 255);

        factor = Math.pow(255, 1.0 - 1.0 / gammatgt);
        value = Math.min(Math.max(factor * Math.pow(value, 1.0 / gammatgt), 0), 255);

        return value;
    }

    private double yuv2r(double y, double v) {
        return Math.min(Math.max(y + 1.140 * v, 0), 255);
    }

    private double yuv2g(double y, double u, double v) {
        return Math.min(Math.max(y - 0.396 * u - 0.581 * v, 0), 255);
    }

    private double yuv2b(double y, double u) {
        return Math.min(Math.max(y + 2.029 * u, 0), 255);
    }

    private void render() {
        double con = contrast / 100 + contrastBoost;
        double sat = saturation / 1.25;
        double bri = brightness - 50;
        double y, u, v;

        palette.clear();

        for (int luma : lumas) {
            for (int k = -2; k < angles.length; k++) {
                y = (k == -2) ? 0 : luma;
                u = (k  <  0) ? 0 : sat * Math.cos(Math.toRadians(angles[k]));
                v = (k  <  0) ? 0 : sat * Math.sin(Math.toRadians(angles[k]));

                y = y * con + bri;
                u *= con;
                v *= con;

                int r = (int) Math.round(gamma(yuv2r(y, v)));
                int g = (int) Math.round(gamma(yuv2g(y, u, v)));
                int b = (int) Math.round(gamma(yuv2b(y, u)));

                palette.add(new Color((r << 16) | (g << 8) | b));
            }
        }
    }

    @Override
    public Iterator<Color> iterator() {
        return palette.iterator();
    }
}
