package com.icthh.xm.gate.web.rest;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.config.ApplicationProperties;
import com.icthh.xm.gate.service.file.download.DownloadFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
public class DownloadResource {

    private final TenantContextHolder tenantContextHolder;
    private final ApplicationProperties applicationProperties;

    private final DownloadFileService downloadFileService;

    @GetMapping("/{recordType}/{fileName}")
    @PreAuthorize("hasPermission({'fileName': #fileName}, 'RESOURCE.FILE.DOWNLOAD')")
    public ResponseEntity<Resource> streamDownload(@PathVariable String recordType,
                                                   @PathVariable String fileName) throws IOException {
        String fullFilePath = downloadFileService.getFullFilePath(recordType, fileName);
        Path downloadPath = getFilePath(fullFilePath);
        Resource resource = new PathResource(downloadPath);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(Files.size(downloadPath))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    private Path getFilePath(String filePath) {
        return Paths.get(applicationProperties.getObjectStorageFileRoot())
            .resolve(tenantContextHolder.getTenantKey().toLowerCase())
            .resolve(filePath)
            .normalize();
    }
}
