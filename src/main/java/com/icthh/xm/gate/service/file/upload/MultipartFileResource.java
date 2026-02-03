package com.icthh.xm.gate.service.file.upload;

import lombok.SneakyThrows;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public class MultipartFileResource extends InputStreamResource {

    private final String filename;

    public MultipartFileResource(MultipartFile file) {
        super(getInputStream(file));
        filename = file.getOriginalFilename();
    }

    @SneakyThrows
    private static InputStream getInputStream(MultipartFile file) {
        return file.getInputStream();
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long contentLength() throws IOException {
        return -1; // for avoid read file to memory
    }
}
