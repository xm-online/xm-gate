package com.icthh.xm.gate.service.file.download.util;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for parsing JWT (JSON Web Token) strings and flattening their claims.
 * This class provides a method to convert the hierarchical claims structure of a JWT
 * into a flat map of string key-value pairs. Nested maps are represented using dot notation,
 * and lists are indexed using square brackets.
 * Example:
 * <pre>
 * JWT claims:
 * {
 *   "user": {
 *     "id": 123,
 *     "roles": ["ADMIN", "USER"]
 *   },
 *   "exp": 1680000000
 * }
 *
 * Flattened map:
 * {
 *   "user.id": "123",
 *   "user.roles[0]": "ADMIN",
 *   "user.roles[1]": "USER",
 *   "exp": "1680000000"
 * }
 * </pre>
 */
@Slf4j
public class JwtParserUtils {

    public Map<String, String> flattenToken(Optional<String> tokenValue) {
        try {
            JWT jwt = JWTParser.parse(tokenValue.get());
            Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();
            Map<String, String> flatMap = new HashMap<>();
            flatten("", claims, flatMap);
            return flatMap;

        } catch (ParseException e) {
            log.error("Error during parsing jwt", e);
            throw new RuntimeException(e);
        }
    }

    private void flatten(String prefix, Map<String, Object> map, Map<String, String> out) {
        map.forEach((k, v) -> {
            String key = prefix.isEmpty() ? k : prefix + "." + k;
            if (v instanceof Map) {
                flatten(key, (Map<String, Object>) v, out);
            } else if (v instanceof List) {
                List<?> list = (List<?>) v;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        flatten(key + "[" + i + "]", (Map<String, Object>) item, out);
                    } else {
                        out.put(key + "[" + i + "]", item.toString());
                    }
                }
            } else {
                out.put(key, v != null ? v.toString() : "");
            }
        });
    }
}
