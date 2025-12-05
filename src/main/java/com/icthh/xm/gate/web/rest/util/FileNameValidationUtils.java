package com.icthh.xm.gate.web.rest.util;

import com.icthh.xm.commons.exceptions.BusinessException;
import org.apache.commons.io.FilenameUtils;

import java.util.regex.Pattern;

public class FileNameValidationUtils {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[^/\\\\]+\\.[^/\\\\]+$");

    public static String validateFileName(String fileName) {
        fileName = FilenameUtils.getName(fileName);

        if (fileName == null || fileName.isEmpty() || !FILENAME_PATTERN.matcher(fileName).matches()) {
            throw new BusinessException("Invalid file name: " + fileName);
        }
        return fileName;
    }
}
