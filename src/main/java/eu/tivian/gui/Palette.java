package eu.tivian.gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Commodore 16 palette.
 * <br>Based on <a href="www.colodore.com">Colodore</a>.
 *
 * @author Paweł Kania
 * @author Pepto
 * @since 2019-12-03
 * @see <a href="view-source:https://www.colodore.com/">Colodore source code</a>
 * @see <a href="https://www.pagetable.com/docs/ted/TED%20System%20Hardware%20Manual.pdf#page=16">
 *     TED System Hardware Manual about video output</a>
 */
public class Palette implements Iterable<Color> {
    /**
     * Brightness of the palette.
     */
    public double brightness = 50.0;
    /**
     * Contrast of the palette.
     */
    public double contrast = 100.0;
    /**
     * Saturation of the palette.
     */
    public double saturation = 50.0;

    /**
     * Designed for better 1802 monitor emulation.
     */
    private final double contrastBoost = 0.2;
    /**
     * Gamma correction of PAL.
     */
    private final double gammasrc = 2.8;
    /**
     * Gamma correction of sRGB.
     */
    private final double gammatgt = 2.2;
    /**
     * Actual palette of colors.
     */
    private final List<Color> palette = new ArrayList<>();

    /**
     * Luminance levels of the TED chip.
     * @see <a href="https://www.pagetable.com/docs/ted/TED%20System%20Hardware%20Manual.pdf#page=16">
     *     TED System Hardware Manual</a>
     */
    private final int[] lumas = new int[] {
        32, 48, 64, 80, 120, 144, 192, 256
    };

    /**
     * Color phase angles.
     * @see <a href="https://www.pagetable.com/docs/ted/TED%20System%20Hardware%20Manual.pdf#page=17">
     *     TED System Hardware Manual</a>
     */
    private final int[] angles = new int[] {
        //101, 281, 56, 236, 348, 169, 124, 146, 191, 79, 259, 326, 11, 214 // Colodore PAL
        103, 283, 53, 241, 347, 167, 129, 148, 195, 83, 265, 323, 23, 213 // PAL
        //70, 250, 20, 208, 314, 134, 90, 115, 162, 50, 232, 290, 350, 180 // NTSC
    };

    /**
     * Initializes the palette.
     */
    public Palette() {
        render();
    }

    /**
     * Returns i-th color in the palette.
     *
     * @param i index of the color in the palette
     * @return {@code Color} object of the requested palette color
     * @throws ArrayIndexOutOfBoundsException if the {@code i} parameter is out of range (i &le; 0 || i &ge; 121)
     */
    public Color get(int i) {
        return palette.get(i);
    }

    /**
     * Returns the size of the palette.
     * @return the size of the palette.
     */
    public int size() {
        return palette.size();
    }

    /**
     * Calculates the gamma from PAL to sRGB value.
     *
     * @param value gamma value suitable for PAL signal
     * @return gamma value suitable for sRGB spectrum
     */
    private double gamma(double value) {
        value = Math.min(Math.max(value, 0), 255);

        double factor = Math.pow(255, 1.0 - gammasrc);
        value = Math.min(Math.max(factor * Math.pow(value, gammasrc), 0), 255);

        factor = Math.pow(255, 1.0 - 1.0 / gammatgt);
        value = Math.min(Math.max(factor * Math.pow(value, 1.0 / gammatgt), 0), 255);

        return value;
    }

    /**
     * Converts the YUV color to the red component of RGB format.
     *
     * @param y luminance
     * @param v red projection
     * @return red component of RGB
     * @see <a href="https://www.itu.int/dms_pubrec/itu-r/rec/bt/R-REC-BT.601-7-201103-I!!PDF-E.pdf#page=5">
     *     BT.601 Standard</a>
     */
    private double yuv2r(double y, double v) {
        return Math.min(Math.max(y + 1.140 * v, 0), 255);
    }

    /**
     * Converts the YUV color to the green component of RGB format.
     *
     * @param y luminance
     * @param u blue projection
     * @param v red projection
     * @return green component of RGB
     * @see <a href="https://www.itu.int/dms_pubrec/itu-r/rec/bt/R-REC-BT.601-7-201103-I!!PDF-E.pdf#page=5">
     *     BT.601 Standard</a>
     */
    private double yuv2g(double y, double u, double v) {
        return Math.min(Math.max(y - 0.396 * u - 0.581 * v, 0), 255);
    }

    /**
     * Converts the YUV color to the blue component of RGB format.
     *
     * @param y luminance
     * @param u blue projection
     * @return blue component of RGB
     * @see <a href="https://www.itu.int/dms_pubrec/itu-r/rec/bt/R-REC-BT.601-7-201103-I!!PDF-E.pdf#page=5">
     *     BT.601 Standard</a>
     */
    private double yuv2b(double y, double u) {
        return Math.min(Math.max(y + 2.029 * u, 0), 255);
    }

    /**
     * Creates the palette according to the values of {@link #contrast}, {@link #saturation} and {@link #brightness}.
     */
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

    /**
     * Returns the iterator for this palette.
     *
     * @return the iterator for this palette.
     */
    @Override
    public Iterator<Color> iterator() {
        return palette.iterator();
    }
}
