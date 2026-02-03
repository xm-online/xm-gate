package com.icthh.xm.gate.web.rest.file;

import com.icthh.xm.gate.service.file.upload.UploadFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import static com.icthh.xm.gate.config.Constants.UPLOAD_PREFIX;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Endpoint for proxying multipart form uploads.
 *
 * Example usage:
 * http://<host>:<port>/upload/entity/api/functions/FUNCTION-NAME/upload
 * This request will be proxy multipart form to entity to /api/functions/FUNCTION-NAME/upload
 *
 * Reason for create this class:
 * 1) FormBodyWrapperFilter read file to memory
 * 2) LoadBalancerRequestFactory operate with byte array (and read file to memory)
 * 3) If content length of multipart resource specified and > -1 than rest template use ByteArrayOutputStream (and read file to memory)
 *
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UploadResource {

    private final UploadFileService uploadFileService;

    @RequestMapping(value = UPLOAD_PREFIX + "**", method = {POST, PUT})
    public ResponseEntity<Object> upload(MultipartHttpServletRequest request) {
        return uploadFileService.upload(request);
    }
}
