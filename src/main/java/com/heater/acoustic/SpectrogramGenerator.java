package com.heater.acoustic;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple spectrogram PNG from waveform (STFT magnitude cartoon). */
public final class SpectrogramGenerator {

    private SpectrogramGenerator() {}

    public static void write(Path path, double[] samples, int sampleRateHz, String title) throws IOException {
        int fftSize = 512;
        int hop = 256;
        int frames = Math.max(1, (samples.length - fftSize) / hop);
        int bins = fftSize / 2;

        BufferedImage img = new BufferedImage(frames, bins, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, frames, bins);
        g.dispose();

        for (int f = 0; f < frames; f++) {
            int start = f * hop;
            double[] frame = new double[fftSize];
            for (int i = 0; i < fftSize && start + i < samples.length; i++) {
                frame[i] = samples[start + i] * hanning(i, fftSize);
            }
            double[] mag = magnitudeSpectrum(frame);
            for (int b = 0; b < bins; b++) {
                double db = 10.0 * Math.log10(Math.max(1e-12, mag[b]));
                int intensity = (int) Math.max(0, Math.min(255, (db + 60) * 4));
                img.setRGB(f, bins - 1 - b, new Color(intensity, intensity / 2, intensity / 3).getRGB());
            }
        }

        Files.createDirectories(path.getParent());
        ImageIO.write(img, "png", path.toFile());
    }

    private static double hanning(int i, int n) {
        return 0.5 * (1 - Math.cos(2 * Math.PI * i / Math.max(1, n - 1)));
    }

    private static double[] magnitudeSpectrum(double[] frame) {
        int n = frame.length;
        double[] re = new double[n / 2];
        double[] im = new double[n / 2];
        for (int k = 0; k < n / 2; k++) {
            for (int t = 0; t < n; t++) {
                double angle = -2 * Math.PI * k * t / n;
                re[k] += frame[t] * Math.cos(angle);
                im[k] += frame[t] * Math.sin(angle);
            }
        }
        double[] mag = new double[n / 2];
        for (int k = 0; k < n / 2; k++) {
            mag[k] = re[k] * re[k] + im[k] * im[k];
        }
        return mag;
    }
}
