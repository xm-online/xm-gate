package com.icthh.xm.gate.service.file.download;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.AbstractSpringBootTest;
import com.icthh.xm.gate.service.file.download.strategy.AuthTokenFilePathResolver;
import com.icthh.xm.gate.service.file.download.strategy.DateFilePathResolver;
import com.icthh.xm.gate.service.file.download.strategy.FilePathResolver;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.gate.service.file.download.util.JwtParserUtilsUnitTest.createJwt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DownloadFileServiceIntTest extends AbstractSpringBootTest {

    XmAuthenticationContextHolder authenticationContextHolder;

    @Autowired
    private DownloadFileConfigService downloadFileConfigService;

    @Autowired
    private DateFilePathResolver dateFilePathResolver;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    private DownloadFileService downloadFileService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        authenticationContextHolder = mock(XmAuthenticationContextHolder.class);
        XmAuthenticationContext xmAuthenticationContext = mockContext(null);
        when(authenticationContextHolder.getContext()).thenReturn(xmAuthenticationContext);

        AuthTokenFilePathResolver authTokenFilePathResolver = new AuthTokenFilePathResolver(authenticationContextHolder);
        Set<FilePathResolver> filePathResolvers = Set.of(dateFilePathResolver, authTokenFilePathResolver);
        downloadFileService = new DownloadFileService(downloadFileConfigService, filePathResolvers);

        TenantContextUtils.setTenant(tenantContextHolder, "XM");
        downloadFileConfigService.onRefresh("/config/tenants/XM/file-download.yml", getDownloadFileSpec("file-download-xm"));
    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void shouldReplaceDatePlaceholders() {
        LocalDate now = LocalDate.now();
        String yyyy = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String mm = now.format(DateTimeFormatter.ofPattern("MM"));
        String dd = now.format(DateTimeFormatter.ofPattern("dd"));

        String fileName = "song.mp3";

        String full = downloadFileService.getFullFilePath("date", fileName);

        assertEquals("/files/" + yyyy + "/" + mm + "/" + dd + "/song.mp3", full);
    }

    @Test
    public void shouldReplaceTokenPlaceholders() {
        Map<String, Object> tokenValues = Map.of(
            "tenant", "XM",
            "userKey", "abc-123",
            "additionalDetails.testParam1", "998877",
            "additionalDetails.testParam2", "334455"
        );
        XmAuthenticationContext xmAuthenticationContext = mockContext(tokenValues);
        when(authenticationContextHolder.getContext()).thenReturn(xmAuthenticationContext);

        String full = downloadFileService.getFullFilePath("token", "avatar.png");

        assertEquals("/XM/abc-123/998877/avatar.png", full);
    }

    @Test
    public void shouldResolveBothDateAndTokenResolversInPipeline() {
        String file = "file.txt";

        LocalDate now = LocalDate.now();
        String yyyy = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String mm = now.format(DateTimeFormatter.ofPattern("MM"));

        Map<String, Object> tokenValues = Map.of(
            "tenant", "XM",
            "userKey", "XYZ"
        );
        XmAuthenticationContext xmAuthenticationContext = mockContext(tokenValues);
        when(authenticationContextHolder.getContext()).thenReturn(xmAuthenticationContext);

        String full = downloadFileService.getFullFilePath("mixed", file);

        assertEquals("/root/" + yyyy + "/XM/" + mm + "/XYZ/file.txt", full);
    }

    @Test
    public void shouldReturnStaticPathIfNoPlaceholders() {
        String full = downloadFileService.getFullFilePath("const", "movie.mp4");

        assertEquals("/static/media/movie.mp4", full);
    }

    @Test
    public void shouldApplyResolversInOrder() {
        Map<String, Object> tokenValues = Map.of("tenant", "ORDERED");
        XmAuthenticationContext xmAuthenticationContext = mockContext(tokenValues);
        when(authenticationContextHolder.getContext()).thenReturn(xmAuthenticationContext);

        LocalDate now = LocalDate.now();
        String yyyy = now.format(DateTimeFormatter.ofPattern("yyyy"));

        String full = downloadFileService.getFullFilePath("ordered", "app.bin");

        assertEquals("/" + yyyy + "/ORDERED/app.bin", full);
    }

    @SneakyThrows
    public static String getDownloadFileSpec(String fileName) {
        String configName = format("config/templates/%s.yml", fileName);
        InputStream cfgInputStream = new ClassPathResource(configName).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    private XmAuthenticationContext mockContext(Map<String, Object> claims) {
        if (claims == null) {
            claims = Map.of(
                "tenant", "XM",
                "userKey", "user-uuid",
                "additionalDetails", Map.of("param1", 1234567)
            );
        }
        String jwt = createJwt(claims);

        XmAuthenticationContext ctx = mock(XmAuthenticationContext.class);
        when(ctx.getTokenValue()).thenReturn(Optional.of(jwt));

        return ctx;
    }
}
