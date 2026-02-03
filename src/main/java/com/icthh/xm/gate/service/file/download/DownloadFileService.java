package com.icthh.xm.gate.service.file.download;

import com.icthh.xm.gate.service.file.download.strategy.FilePathResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadFileService {

    private final DownloadFileConfigService downloadFileConfigService;
    private final Set<FilePathResolver> filePathResolvers;

    public String getFullFilePath(String recordType, String fileName) {
        DownloadFileSpec specByKey = downloadFileConfigService.getSpecByKey(recordType);
        String pathPrefix = filePathResolvers.stream()
            .filter(r -> r.supports(specByKey.getStrategy()))
            .reduce(specByKey.getPathPrefix(), (p, resolver) -> resolver.resolvePath(p), (p1, p2) -> p2);
        return pathPrefix + fileName;
    }
}
