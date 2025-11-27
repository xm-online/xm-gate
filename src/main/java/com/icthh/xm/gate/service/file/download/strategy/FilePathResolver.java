package com.icthh.xm.gate.service.file.download.strategy;

import com.icthh.xm.gate.service.file.download.DownloadFileSpec;

public interface FilePathResolver {

    boolean supports(DownloadFileSpec.PathPatternStrategy strategy);
    String resolvePath(String pathPrefixPattern);
}
