package com.icthh.xm.gate.service.file.download.util;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class JwtParserUtilsUnitTest {

    private final JwtParserUtils utils = new JwtParserUtils();

    @Test
    public void testSimpleClaims() {
        Map<String, Object> claims = Map.of(
            "sub", "123",
            "role", "admin"
        );

        String jwt = createJwt(claims);
        Map<String, String> flat = utils.flattenToken(Optional.of(jwt));

        assertEquals("123", flat.get("sub"));
        assertEquals("admin", flat.get("role"));
    }

    @Test
    public void testNestedObjects() {
        Map<String, Object> claims = Map.of(
            "user", Map.of(
                "id", 10,
                "name", "Alice"
            )
        );

        String jwt = createJwt(claims);
        Map<String, String> flat = utils.flattenToken(Optional.of(jwt));

        assertEquals("10", flat.get("user.id"));
        assertEquals("Alice", flat.get("user.name"));
    }

    @Test
    public void testListOfPrimitives() {
        Map<String, Object> claims = Map.of(
            "permissions", List.of("READ", "WRITE")
        );

        String jwt = createJwt(claims);
        Map<String, String> flat = utils.flattenToken(Optional.of(jwt));

        assertEquals("READ", flat.get("permissions[0]"));
        assertEquals("WRITE", flat.get("permissions[1]"));
    }

    @Test
    public void testListOfObjects() {
        Map<String, Object> claims = Map.of(
            "groups", List.of(
                Map.of("id", 1, "name", "A"),
                Map.of("id", 2, "name", "B")
            )
        );

        String jwt = createJwt(claims);
        Map<String, String> flat = utils.flattenToken(Optional.of(jwt));

        assertEquals("1", flat.get("groups[0].id"));
        assertEquals("A", flat.get("groups[0].name"));
        assertEquals("2", flat.get("groups[1].id"));
        assertEquals("B", flat.get("groups[1].name"));
    }

    @Test
    public void testInvalidTokenThrows() {
        assertThrows(RuntimeException.class,
            () -> utils.flattenToken(Optional.of("not-a-jwt"))
        );
    }

    @Test
    public void testEmptyOptionalThrowsNoSuchElement() {
        assertThrows(NoSuchElementException.class,
            () -> utils.flattenToken(Optional.empty())
        );
    }

    public static String createJwt(Map<String, Object> claims) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
        claims.forEach(builder::claim);
        builder.expirationTime(Date.from(Instant.now().plusSeconds(3600)));

        return new PlainJWT(builder.build()).serialize();
    }
}
