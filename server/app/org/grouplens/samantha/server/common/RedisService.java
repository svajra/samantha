package org.grouplens.samantha.server.common;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.ImplementedBy;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@ImplementedBy(RedisLettuceService.class)
public interface RedisService {
    static String composeKey(String prefix, String key) {
        return prefix + "\1" + key;
    }

    static String composeKey(JsonNode entity, List<String> keyAttrs) {
        List<String> keys = new ArrayList<>();
        for (String attr : keyAttrs) {
            keys.add(composeKey(attr, entity.get(attr).asText()));
        }
        return StringUtils.join(keys, "\t");
    }

    void watch(String prefix, String key);
    void multi(boolean lock);
    List<Object> exec();
    String get(String prefix, String key);
    Long incre(String prefix, String key);
    Long increWithoutLock(String prefix, String key);
    String set(String prefix, String key, String value);
    String setWithoutLock(String prefix, String key, String value);
    void del(String prefix, String key);
    void delWithKey(String key);
    JsonNode getValue(String prefix, String key);
    void setValue(String prefix, String key, JsonNode value);
    List<String> keysWithPrefixPattern(String prefix, String key);
    List<JsonNode> bulkGet(List<String> keys);
    void indexIntoSortedSet(String prefix, String key, String scoreAttr, JsonNode data);
    void bulkIndexIntoSortedSet(String prefix, List<String> keyAttrs, String scoreAttr, JsonNode data);
    void bulkIndexIntoHashSet(String prefix, List<String> keyAttrs, List<String> hashAttrs, JsonNode data);
    List<JsonNode> bulkGetFromHashSet(String prefix, List<String> keyAttrs, JsonNode data);
    List<JsonNode> bulkUniqueGetFromHashSet(String prefix, List<String> keyAttrs, List<ObjectNode> data);
}
