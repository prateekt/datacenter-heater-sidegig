package com.heater.acoustic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Minimal PCM WAV writer (16-bit mono). */
public final class WavExporter {

    private WavExporter() {}

    public static void writeMono(Path path, double[] samples, int sampleRateHz) throws IOException {
        Files.createDirectories(path.getParent());
        byte[] pcm = toPcm16(samples);
        byte[] header = buildHeader(pcm.length, sampleRateHz, 1, 16);
        byte[] file = new byte[header.length + pcm.length];
        System.arraycopy(header, 0, file, 0, header.length);
        System.arraycopy(pcm, 0, file, header.length, pcm.length);
        Files.write(path, file);
    }

    public static byte[] toPcm16(double[] samples) {
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int v = (int) Math.round(Math.max(-1.0, Math.min(1.0, samples[i])) * 32767);
            out[i * 2] = (byte) (v & 0xff);
            out[i * 2 + 1] = (byte) ((v >> 8) & 0xff);
        }
        return out;
    }

    public static byte[] buildHeader(int dataBytes, int sampleRate, int channels, int bitsPerSample) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        ByteBuffer buf = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        buf.put("RIFF".getBytes());
        buf.putInt(36 + dataBytes);
        buf.put("WAVE".getBytes());
        buf.put("fmt ".getBytes());
        buf.putInt(16);
        buf.putShort((short) 1);
        buf.putShort((short) channels);
        buf.putInt(sampleRate);
        buf.putInt(byteRate);
        buf.putShort((short) blockAlign);
        buf.putShort((short) bitsPerSample);
        buf.put("data".getBytes());
        buf.putInt(dataBytes);
        return buf.array();
    }

    public static boolean isValidWavHeader(byte[] header) {
        if (header.length < 44) return false;
        return header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'A' && header[10] == 'V' && header[11] == 'E';
    }
}
