package com.heater.acoustic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WavExporterTest {

    @Test
    void writesValidWavHeader(@TempDir Path temp) throws Exception {
        Path wav = temp.resolve("test.wav");
        double[] samples = {0.0, 0.5, -0.5, 0.25};
        WavExporter.writeMono(wav, samples, 44100);
        assertTrue(Files.exists(wav));
        byte[] bytes = Files.readAllBytes(wav);
        assertTrue(bytes.length > 44);
        byte[] header = new byte[44];
        System.arraycopy(bytes, 0, header, 0, 44);
        assertTrue(WavExporter.isValidWavHeader(header));
    }

    @Test
    void pcm16ClampsSamples() {
        byte[] pcm = WavExporter.toPcm16(new double[] {2.0, -2.0, 0.0});
        assertEquals(6, pcm.length);
    }
}
