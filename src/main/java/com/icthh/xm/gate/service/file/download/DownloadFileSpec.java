package com.icthh.xm.gate.service.file.download;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

import static com.icthh.xm.gate.service.file.download.DownloadFileSpec.PathPatternStrategy.XM_TOKEN_MATCHER;

/**
 * Represents a specification for downloading a file.
 * - key - a unique identifier for spec during download
 * - strategy - the strategy used to resolve the file path prefix
 * - pathPrefix - a filepath prefix template
 */
@Getter
@AllArgsConstructor
public class DownloadFileSpec {

    private String key;
    private PathPatternStrategy strategy;
    private String pathPrefix;

    public DownloadFileSpec() {
        this.strategy = XM_TOKEN_MATCHER;
        this.pathPrefix = "";
    }

    public enum PathPatternStrategy {
        XM_TOKEN_MATCHER
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadPatternsList {
        private Set<DownloadFileSpec> patterns;
    }
}
