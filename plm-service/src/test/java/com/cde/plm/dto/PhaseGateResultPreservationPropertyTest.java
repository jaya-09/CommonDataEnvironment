package com.cde.plm.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * Preservation Property Tests for PhaseGateResult Existing Fields
 * 
 * **Property 2: Preservation** - Existing Field Behavior Unchanged
 * **Validates: Requirements 3.1, 3.2, 3.3**
 * 
 * IMPORTANT: Follow observation-first methodology
 * These tests observe and capture behavior on UNFIXED code for existing PhaseGateResult fields
 * 
 * GOAL: Establish baseline behavior that must be preserved when adding missing fields
 * 
 * EXPECTED OUTCOME: Tests PASS on unfixed code (confirms baseline behavior to preserve)
 * 
 * Existing fields to test:
 * - canAdvance (boolean)
 * - blockReason (String)  
 * - targetPhase (String)
 * - passed (boolean)
 * - fromPhase (String)
 * - toPhase (String)
 * - blockers (Map<String, String>)
 * - warnings (List<String>)
 * - message (String)
 */
public class PhaseGateResultPreservationPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random(42); // Fixed seed for reproducible tests

    @Test
    @DisplayName("Property 2: Existing Builder Methods Work on Unfixed Code")
    public void testExistingBuilderMethodsWork() {
        // Observe: PhaseGateResult.builder().canAdvance(true) works on unfixed code
        // Observe: PhaseGateResult.builder().blockReason("reason") works on unfixed code
        
        try {
            // Test that all existing builder methods work correctly
            Map<String, String> blockers = new HashMap<>();
            blockers.put("cert", "Missing certification");
            blockers.put("ncr", "Open NCR");
            
            List<String> warnings = Arrays.asList("Warning 1", "Warning 2");
            
            PhaseGateResult result = PhaseGateResult.builder()
                .canAdvance(true)
                .blockReason("Test block reason")
                .targetPhase("DEVELOPMENT")
                .passed(false)
                .fromPhase("DESIGN")
                .toPhase("DEVELOPMENT")
                .blockers(blockers)
                .warnings(warnings)
                .message("Test message")
                .build();
            
            // Verify all fields are set correctly
            assertTrue(result.canAdvance, "canAdvance should be true");
            assertEquals("Test block reason", result.blockReason, "blockReason should match");
            assertEquals("DEVELOPMENT", result.targetPhase, "targetPhase should match");
            assertFalse(result.passed, "passed should be false");
            assertEquals("DESIGN", result.fromPhase, "fromPhase should match");
            assertEquals("DEVELOPMENT", result.toPhase, "toPhase should match");
            assertEquals(blockers, result.blockers, "blockers should match");
            assertEquals(warnings, result.warnings, "warnings should match");
            assertEquals("Test message", result.message, "message should match");
            
            System.out.println("✓ All existing builder methods work correctly on unfixed code");
            
        } catch (Exception e) {
            fail("Existing builder methods should work on unfixed code: " + e.getMessage());
        }
    }

    @RepeatedTest(10)
    @DisplayName("Property 2: Boolean Fields Preserve Behavior Across Many Values")
    public void testBooleanFieldsPreservation() {
        // Property-based test: For any boolean values, existing boolean fields should work
        boolean canAdvanceValue = random.nextBoolean();
        boolean passedValue = random.nextBoolean();
        
        PhaseGateResult result = PhaseGateResult.builder()
            .canAdvance(canAdvanceValue)
            .passed(passedValue)
            .build();
        
        assertEquals(canAdvanceValue, result.canAdvance, 
            "canAdvance field should preserve value: " + canAdvanceValue);
        assertEquals(passedValue, result.passed, 
            "passed field should preserve value: " + passedValue);
    }

    @RepeatedTest(10)
    @DisplayName("Property 2: String Fields Preserve Behavior Across Many Values")
    public void testStringFieldsPreservation() {
        // Property-based test: For any string values, existing string fields should work
        String[] phases = {"DESIGN", "DEVELOPMENT", "TESTING", "PRODUCTION", null};
        String[] reasons = {"Missing cert", "Open NCR", "Incomplete docs", "", null};
        String[] messages = {"Success", "Failed validation", "Warning issued", "", null};
        
        String blockReason = reasons[random.nextInt(reasons.length)];
        String targetPhase = phases[random.nextInt(phases.length)];
        String fromPhase = phases[random.nextInt(phases.length)];
        String toPhase = phases[random.nextInt(phases.length)];
        String message = messages[random.nextInt(messages.length)];
        
        PhaseGateResult result = PhaseGateResult.builder()
            .blockReason(blockReason)
            .targetPhase(targetPhase)
            .fromPhase(fromPhase)
            .toPhase(toPhase)
            .message(message)
            .build();
        
        assertEquals(blockReason, result.blockReason, "blockReason should preserve value");
        assertEquals(targetPhase, result.targetPhase, "targetPhase should preserve value");
        assertEquals(fromPhase, result.fromPhase, "fromPhase should preserve value");
        assertEquals(toPhase, result.toPhase, "toPhase should preserve value");
        assertEquals(message, result.message, "message should preserve value");
    }

    @RepeatedTest(5)
    @DisplayName("Property 2: Collection Fields Preserve Behavior Across Many Values")
    public void testCollectionFieldsPreservation() {
        // Property-based test: For any collection values, existing collection fields should work
        
        // Generate random blockers map
        Map<String, String> blockers = new HashMap<>();
        int blockerCount = random.nextInt(4); // 0-3 blockers
        String[] blockerKeys = {"cert", "ncr", "docs", "approval"};
        String[] blockerValues = {"Missing certification", "Open NCR", "Incomplete documentation", "Pending approval"};
        
        for (int i = 0; i < blockerCount; i++) {
            blockers.put(blockerKeys[i], blockerValues[i]);
        }
        
        // Generate random warnings list
        List<String> warnings = Arrays.asList();
        int warningCount = random.nextInt(4); // 0-3 warnings
        if (warningCount > 0) {
            String[] warningMessages = {"Warning 1", "Warning 2", "Warning 3"};
            warnings = Arrays.asList(Arrays.copyOf(warningMessages, warningCount));
        }
        
        PhaseGateResult result = PhaseGateResult.builder()
            .blockers(blockers.isEmpty() ? null : blockers)
            .warnings(warnings.isEmpty() ? null : warnings)
            .build();
        
        if (blockers.isEmpty()) {
            assertNull(result.blockers, "Empty blockers should be null");
        } else {
            assertEquals(blockers, result.blockers, "blockers should preserve values");
        }
        
        if (warnings.isEmpty()) {
            assertNull(result.warnings, "Empty warnings should be null");
        } else {
            assertEquals(warnings, result.warnings, "warnings should preserve values");
        }
    }

    @Test
    @DisplayName("Property 2: JSON Serialization of Existing Fields Works on Unfixed Code")
    public void testJsonSerializationPreservation() throws Exception {
        // Observe: JSON serialization of existing fields works on unfixed code
        
        Map<String, String> blockers = new HashMap<>();
        blockers.put("certification", "Staff member lacks required certification");
        blockers.put("ncr", "Critical NCR #12345 is open");
        
        List<String> warnings = Arrays.asList(
            "Minor documentation issue detected",
            "Recommended additional testing"
        );
        
        PhaseGateResult original = PhaseGateResult.builder()
            .canAdvance(false)
            .blockReason("Multiple issues prevent advancement")
            .targetPhase("PRODUCTION")
            .passed(false)
            .fromPhase("TESTING")
            .toPhase("PRODUCTION")
            .blockers(blockers)
            .warnings(warnings)
            .message("Phase gate check completed with issues")
            .build();
        
        // Test serialization to JSON
        String json = objectMapper.writeValueAsString(original);
        assertNotNull(json, "JSON serialization should work");
        assertTrue(json.contains("canAdvance"), "JSON should contain canAdvance field");
        assertTrue(json.contains("blockReason"), "JSON should contain blockReason field");
        assertTrue(json.contains("targetPhase"), "JSON should contain targetPhase field");
        assertTrue(json.contains("passed"), "JSON should contain passed field");
        assertTrue(json.contains("fromPhase"), "JSON should contain fromPhase field");
        assertTrue(json.contains("toPhase"), "JSON should contain toPhase field");
        assertTrue(json.contains("blockers"), "JSON should contain blockers field");
        assertTrue(json.contains("warnings"), "JSON should contain warnings field");
        assertTrue(json.contains("message"), "JSON should contain message field");
        
        System.out.println("✓ JSON serialization works: " + json);
        
        // Test deserialization from JSON
        PhaseGateResult deserialized = objectMapper.readValue(json, PhaseGateResult.class);
        assertNotNull(deserialized, "JSON deserialization should work");
        
        // Verify all fields are preserved through serialization/deserialization
        assertEquals(original.canAdvance, deserialized.canAdvance, "canAdvance should be preserved");
        assertEquals(original.blockReason, deserialized.blockReason, "blockReason should be preserved");
        assertEquals(original.targetPhase, deserialized.targetPhase, "targetPhase should be preserved");
        assertEquals(original.passed, deserialized.passed, "passed should be preserved");
        assertEquals(original.fromPhase, deserialized.fromPhase, "fromPhase should be preserved");
        assertEquals(original.toPhase, deserialized.toPhase, "toPhase should be preserved");
        assertEquals(original.blockers, deserialized.blockers, "blockers should be preserved");
        assertEquals(original.warnings, deserialized.warnings, "warnings should be preserved");
        assertEquals(original.message, deserialized.message, "message should be preserved");
        
        System.out.println("✓ JSON deserialization preserves all existing fields correctly");
    }

    @RepeatedTest(5)
    @DisplayName("Property 2: JSON Serialization Preserves Behavior Across Many Field Combinations")
    public void testJsonSerializationPropertyBased() throws Exception {
        // Property-based test: For any combination of existing field values, 
        // JSON serialization/deserialization should preserve all values exactly
        
        // Generate random field values
        boolean canAdvance = random.nextBoolean();
        boolean passed = random.nextBoolean();
        String blockReason = random.nextBoolean() ? "Random block reason " + random.nextInt(100) : null;
        String targetPhase = random.nextBoolean() ? "PHASE_" + random.nextInt(5) : null;
        String fromPhase = random.nextBoolean() ? "FROM_" + random.nextInt(5) : null;
        String toPhase = random.nextBoolean() ? "TO_" + random.nextInt(5) : null;
        String message = random.nextBoolean() ? "Message " + random.nextInt(100) : null;
        
        // Generate random collections
        Map<String, String> blockers = null;
        if (random.nextBoolean()) {
            blockers = new HashMap<>();
            int count = random.nextInt(3) + 1;
            for (int i = 0; i < count; i++) {
                blockers.put("key" + i, "value" + i);
            }
        }
        
        List<String> warnings = null;
        if (random.nextBoolean()) {
            int count = random.nextInt(3) + 1;
            warnings = Arrays.asList();
            String[] warningArray = new String[count];
            for (int i = 0; i < count; i++) {
                warningArray[i] = "Warning " + i;
            }
            warnings = Arrays.asList(warningArray);
        }
        
        PhaseGateResult original = PhaseGateResult.builder()
            .canAdvance(canAdvance)
            .blockReason(blockReason)
            .targetPhase(targetPhase)
            .passed(passed)
            .fromPhase(fromPhase)
            .toPhase(toPhase)
            .blockers(blockers)
            .warnings(warnings)
            .message(message)
            .build();
        
        // Serialize and deserialize
        String json = objectMapper.writeValueAsString(original);
        PhaseGateResult deserialized = objectMapper.readValue(json, PhaseGateResult.class);
        
        // Property: All existing fields should be preserved exactly
        assertEquals(original.canAdvance, deserialized.canAdvance, "canAdvance preservation failed");
        assertEquals(original.blockReason, deserialized.blockReason, "blockReason preservation failed");
        assertEquals(original.targetPhase, deserialized.targetPhase, "targetPhase preservation failed");
        assertEquals(original.passed, deserialized.passed, "passed preservation failed");
        assertEquals(original.fromPhase, deserialized.fromPhase, "fromPhase preservation failed");
        assertEquals(original.toPhase, deserialized.toPhase, "toPhase preservation failed");
        assertEquals(original.blockers, deserialized.blockers, "blockers preservation failed");
        assertEquals(original.warnings, deserialized.warnings, "warnings preservation failed");
        assertEquals(original.message, deserialized.message, "message preservation failed");
    }

    @Test
    @DisplayName("Property 2: Null Field Handling Preserves Behavior")
    public void testNullFieldHandlingPreservation() throws Exception {
        // Test that null values for existing fields are handled correctly (baseline behavior)
        
        PhaseGateResult result = PhaseGateResult.builder()
            .canAdvance(true)
            .passed(false)
            // All other fields left as null
            .build();
        
        // Verify null fields are handled correctly
        assertTrue(result.canAdvance, "canAdvance should be true");
        assertFalse(result.passed, "passed should be false");
        assertNull(result.blockReason, "blockReason should be null");
        assertNull(result.targetPhase, "targetPhase should be null");
        assertNull(result.fromPhase, "fromPhase should be null");
        assertNull(result.toPhase, "toPhase should be null");
        assertNull(result.blockers, "blockers should be null");
        assertNull(result.warnings, "warnings should be null");
        assertNull(result.message, "message should be null");
        
        // Test JSON serialization with nulls (should exclude null fields due to @JsonInclude(NON_NULL))
        String json = objectMapper.writeValueAsString(result);
        assertNotNull(json, "JSON serialization with nulls should work");
        
        // Verify null fields are excluded from JSON (baseline behavior)
        assertFalse(json.contains("blockReason"), "Null blockReason should be excluded from JSON");
        assertFalse(json.contains("targetPhase"), "Null targetPhase should be excluded from JSON");
        assertFalse(json.contains("fromPhase"), "Null fromPhase should be excluded from JSON");
        assertFalse(json.contains("toPhase"), "Null toPhase should be excluded from JSON");
        assertFalse(json.contains("blockers"), "Null blockers should be excluded from JSON");
        assertFalse(json.contains("warnings"), "Null warnings should be excluded from JSON");
        assertFalse(json.contains("message"), "Null message should be excluded from JSON");
        
        // But non-null fields should be included
        assertTrue(json.contains("canAdvance"), "Non-null canAdvance should be included in JSON");
        assertTrue(json.contains("passed"), "Non-null passed should be included in JSON");
        
        System.out.println("✓ Null field handling works correctly: " + json);
    }

    @Test
    @DisplayName("Property 2: Edge Case Values Preserve Behavior")
    public void testEdgeCaseValuesPreservation() throws Exception {
        // Test edge cases for existing fields to establish baseline behavior
        
        Map<String, String> emptyBlockers = new HashMap<>();
        List<String> emptyWarnings = Arrays.asList();
        
        PhaseGateResult result = PhaseGateResult.builder()
            .canAdvance(false)
            .blockReason("") // Empty string
            .targetPhase("") // Empty string
            .passed(true)
            .fromPhase("") // Empty string
            .toPhase("") // Empty string
            .blockers(emptyBlockers) // Empty map
            .warnings(emptyWarnings) // Empty list
            .message("") // Empty string
            .build();
        
        // Verify edge case values are preserved
        assertFalse(result.canAdvance, "canAdvance should be false");
        assertEquals("", result.blockReason, "Empty blockReason should be preserved");
        assertEquals("", result.targetPhase, "Empty targetPhase should be preserved");
        assertTrue(result.passed, "passed should be true");
        assertEquals("", result.fromPhase, "Empty fromPhase should be preserved");
        assertEquals("", result.toPhase, "Empty toPhase should be preserved");
        assertEquals(emptyBlockers, result.blockers, "Empty blockers should be preserved");
        assertEquals(emptyWarnings, result.warnings, "Empty warnings should be preserved");
        assertEquals("", result.message, "Empty message should be preserved");
        
        // Test JSON serialization with edge cases
        String json = objectMapper.writeValueAsString(result);
        PhaseGateResult deserialized = objectMapper.readValue(json, PhaseGateResult.class);
        
        // Verify edge cases survive serialization/deserialization
        assertEquals(result.canAdvance, deserialized.canAdvance, "canAdvance edge case failed");
        assertEquals(result.blockReason, deserialized.blockReason, "blockReason edge case failed");
        assertEquals(result.targetPhase, deserialized.targetPhase, "targetPhase edge case failed");
        assertEquals(result.passed, deserialized.passed, "passed edge case failed");
        assertEquals(result.fromPhase, deserialized.fromPhase, "fromPhase edge case failed");
        assertEquals(result.toPhase, deserialized.toPhase, "toPhase edge case failed");
        assertEquals(result.blockers, deserialized.blockers, "blockers edge case failed");
        assertEquals(result.warnings, deserialized.warnings, "warnings edge case failed");
        assertEquals(result.message, deserialized.message, "message edge case failed");
        
        System.out.println("✓ Edge case values preserved correctly through JSON serialization");
    }
}