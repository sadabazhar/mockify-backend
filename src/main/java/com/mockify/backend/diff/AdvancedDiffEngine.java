package com.mockify.backend.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.mockify.backend.exception.DiffGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AdvancedDiffEngine {

    private final ObjectMapper objectMapper;

    /**
     * Generates an RFC 6902 JSON Patch representing the changes
     * required to transform the source object into the target object.
     *
     * @param source original object
     * @param target updated object
     * @return RFC 6902 JSON Patch
     */
    public JsonNode calculateRfc6902Patch(Object source, Object target) {

        try {
            JsonNode sourceNode = (source == null)
                    ? objectMapper.createObjectNode()
                    : objectMapper.valueToTree(source);

            JsonNode targetNode = (target == null)
                    ? objectMapper.createObjectNode()
                    : objectMapper.valueToTree(target);

            return JsonDiff.asJson(sourceNode, targetNode);

        } catch (IllegalArgumentException ex) {
            throw new DiffGenerationException(
                    "Failed to generate RFC 6902 JSON Patch.",
                    ex
            );
        }
    }
}