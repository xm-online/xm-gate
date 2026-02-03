package com.icthh.xm.gate.service.file.download.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileNameValidationUtilsUnitTest {

    @Test
    public void testSimpleClaims() {
        assertEquals("file-5678.txt", FileNameValidationUtils.validateFileName("../../file-5678.txt"));
        assertEquals("sdas.bin", FileNameValidationUtils.validateFileName("\\..\\..\\sdas.bin"));
    }
}
