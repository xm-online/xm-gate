package com.icthh.xm.gate.service.file.download.strategy;

import com.icthh.xm.gate.service.file.download.DownloadFileSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DateFilePathResolver implements FilePathResolver {

    @Override
    public boolean supports(DownloadFileSpec.PathPatternStrategy strategy) {
        return true;
    }

    @Override
    public String resolvePath(String pathPrefixPattern) {
        log.info("Resolve download file path date for file path pattern: {}", pathPrefixPattern);

        LocalDate now = LocalDate.now();
        String yyyy = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String mm   = now.format(DateTimeFormatter.ofPattern("MM"));
        String dd   = now.format(DateTimeFormatter.ofPattern("dd"));

        return pathPrefixPattern
            .replace("{yyyy}", yyyy)
            .replace("{mm}", mm)
            .replace("{dd}", dd);
    }
}
