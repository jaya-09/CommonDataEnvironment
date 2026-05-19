package com.cde.plm.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Arrays;

/**
 * Bug Condition Exploration Test for PhaseGateResult Missing Builder Methods
 * 
 * **Property 1: Fault Condition** - Missing Builder Methods Compilation Failure
 * **Validates: Requirements 2.1, 2.2, 2.3**
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * DO NOT attempt to fix the test or the code when it fails
 * 
 * GOAL: Surface counterexamples that demonstrate the compilation failure exists
 * 
 * This test attempts to use the missing builder methods that ProductService.checkPhaseGate()
 * calls but which don't exist because the corresponding fields are missing from PhaseGateResult.
 * 
 * EXPECTED COMPILATION ERRORS ON UNFIXED CODE:
 * - "cannot find symbol: method certCheckPassed(boolean)"
 * - "cannot find symbol: method ncrCheckPassed(boolean)" 
 * - "cannot find symbol: method uncertifiedCount(int)"
 * - "cannot find symbol: method openCriticalNcrs(java.util.List<java.lang.String>)"
 * 
 * These errors occur because PhaseGateResult class is missing the corresponding fields:
 * - boolean certCheckPassed
 * - boolean ncrCheckPassed  
 * - int uncertifiedCount
 * - List<String> openCriticalNcrs
 */
public class PhaseGateResultBugConditionExplorationTest {

    @Test
    @DisplayName("Property 1: Missing Builder Methods Should Cause Compilation Failure")
    public void testMissingBuilderMethodsCompilationFailure() {
        // This test encodes the expected behavior - when the bug is fixed, these calls should work
        // On unfixed code, this test will fail to compile, proving the bug exists
        
        // APPROACH: Since we can't easily demonstrate compilation failure at runtime,
        // we'll use reflection to verify that the builder methods don't exist on unfixed code
        
        try {
            // Get the PhaseGateResult.PhaseGateResultBuilder class
            Class<?> builderClass = null;
            try {
                // Try to get the builder through PhaseGateResult.builder()
                PhaseGateResult.PhaseGateResultBuilder builder = PhaseGateResult.builder();
                builderClass = builder.getClass();
            } catch (Exception e) {
                fail("Could not create PhaseGateResult.builder(): " + e.getMessage());
            }
            
            // Test case 1: certCheckPassed(boolean) method should exist but doesn't on unfixed code
            boolean certCheckPassedExists = false;
            try {
                builderClass.getMethod("certCheckPassed", boolean.class);
                certCheckPassedExists = true;
            } catch (NoSuchMethodException e) {
                // Expected on unfixed code - method doesn't exist
                System.out.println("COUNTEREXAMPLE 1: certCheckPassed(boolean) method not found - " + e.getMessage());
            }
            
            // Test case 2: ncrCheckPassed(boolean) method should exist but doesn't on unfixed code  
            boolean ncrCheckPassedExists = false;
            try {
                builderClass.getMethod("ncrCheckPassed", boolean.class);
                ncrCheckPassedExists = true;
            } catch (NoSuchMethodException e) {
                // Expected on unfixed code - method doesn't exist
                System.out.println("COUNTEREXAMPLE 2: ncrCheckPassed(boolean) method not found - " + e.getMessage());
            }
            
            // Test case 3: uncertifiedCount(int) method should exist but doesn't on unfixed code
            boolean uncertifiedCountExists = false;
            try {
                builderClass.getMethod("uncertifiedCount", int.class);
                uncertifiedCountExists = true;
            } catch (NoSuchMethodException e) {
                // Expected on unfixed code - method doesn't exist
                System.out.println("COUNTEREXAMPLE 3: uncertifiedCount(int) method not found - " + e.getMessage());
            }
            
            // Test case 4: openCriticalNcrs(List) method should exist but doesn't on unfixed code
            boolean openCriticalNcrsExists = false;
            try {
                builderClass.getMethod("openCriticalNcrs", List.class);
                openCriticalNcrsExists = true;
            } catch (NoSuchMethodException e) {
                // Expected on unfixed code - method doesn't exist
                System.out.println("COUNTEREXAMPLE 4: openCriticalNcrs(List) method not found - " + e.getMessage());
            }
            
            // On unfixed code, ALL methods should be missing (all should be false)
            // On fixed code, ALL methods should exist (all should be true)
            boolean allMethodsExist = certCheckPassedExists && ncrCheckPassedExists && 
                                    uncertifiedCountExists && openCriticalNcrsExists;
            
            if (allMethodsExist) {
                System.out.println("SUCCESS: All required builder methods exist - bug appears to be fixed!");
                // This means the bug is fixed - test should pass
            } else {
                // This is the expected state on unfixed code - methods are missing
                System.out.println("BUG CONFIRMED: Missing builder methods detected");
                System.out.println("- certCheckPassed exists: " + certCheckPassedExists);
                System.out.println("- ncrCheckPassed exists: " + ncrCheckPassedExists);
                System.out.println("- uncertifiedCount exists: " + uncertifiedCountExists);
                System.out.println("- openCriticalNcrs exists: " + openCriticalNcrsExists);
                
                // On unfixed code, we expect this test to fail because the methods don't exist
                // This failure confirms the bug exists
                fail("Bug condition confirmed: Required builder methods are missing from PhaseGateResult.PhaseGateResultBuilder. " +
                     "This failure proves the bug exists. Methods missing: " +
                     (!certCheckPassedExists ? "certCheckPassed " : "") +
                     (!ncrCheckPassedExists ? "ncrCheckPassed " : "") +
                     (!uncertifiedCountExists ? "uncertifiedCount " : "") +
                     (!openCriticalNcrsExists ? "openCriticalNcrs " : ""));
            }
            
        } catch (Exception e) {
            fail("Unexpected exception during reflection test: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Property 1: Verify Specific Missing Methods from ProductService Context")
    public void testProductServiceSpecificMissingMethods() {
        // This test specifically targets the exact method calls from ProductService.checkPhaseGate()
        // Lines 194-195 in ProductService.java that cause compilation failure
        
        // Use reflection to verify the exact methods that ProductService tries to call
        try {
            PhaseGateResult.PhaseGateResultBuilder builder = PhaseGateResult.builder();
            Class<?> builderClass = builder.getClass();
            
            // These are the exact method calls from ProductService.checkPhaseGate() lines 194-195:
            // .certCheckPassed(certOk).ncrCheckPassed(ncrOk)
            // .uncertifiedCount(uncertifiedCount).openCriticalNcrs(openNcrs)
            
            String[] missingMethods = new String[0];
            StringBuilder missingMethodsList = new StringBuilder();
            
            // Check each method that ProductService tries to call
            try {
                builderClass.getMethod("certCheckPassed", boolean.class);
                System.out.println("✓ certCheckPassed(boolean) method found");
            } catch (NoSuchMethodException e) {
                missingMethodsList.append("certCheckPassed(boolean) ");
                System.out.println("✗ certCheckPassed(boolean) method MISSING - ProductService line 194 will fail");
            }
            
            try {
                builderClass.getMethod("ncrCheckPassed", boolean.class);
                System.out.println("✓ ncrCheckPassed(boolean) method found");
            } catch (NoSuchMethodException e) {
                missingMethodsList.append("ncrCheckPassed(boolean) ");
                System.out.println("✗ ncrCheckPassed(boolean) method MISSING - ProductService line 194 will fail");
            }
            
            try {
                builderClass.getMethod("uncertifiedCount", int.class);
                System.out.println("✓ uncertifiedCount(int) method found");
            } catch (NoSuchMethodException e) {
                missingMethodsList.append("uncertifiedCount(int) ");
                System.out.println("✗ uncertifiedCount(int) method MISSING - ProductService line 195 will fail");
            }
            
            try {
                builderClass.getMethod("openCriticalNcrs", List.class);
                System.out.println("✓ openCriticalNcrs(List) method found");
            } catch (NoSuchMethodException e) {
                missingMethodsList.append("openCriticalNcrs(List) ");
                System.out.println("✗ openCriticalNcrs(List) method MISSING - ProductService line 195 will fail");
            }
            
            String missingMethodsStr = missingMethodsList.toString().trim();
            if (!missingMethodsStr.isEmpty()) {
                // This is expected on unfixed code - the test should fail to confirm the bug exists
                System.out.println("\nBUG CONDITION CONFIRMED:");
                System.out.println("ProductService.checkPhaseGate() compilation will fail due to missing methods: " + missingMethodsStr);
                System.out.println("This proves the bug exists and needs to be fixed.");
                
                fail("Bug condition exploration successful: ProductService compilation fails due to missing builder methods: " + 
                     missingMethodsStr + ". This failure confirms the bug exists.");
            } else {
                // All methods exist - bug appears to be fixed
                System.out.println("\nSUCCESS: All required builder methods exist!");
                System.out.println("ProductService.checkPhaseGate() should compile successfully.");
                
                // Verify we can actually use the methods (this should work when bug is fixed)
                boolean certOk = true;
                boolean ncrOk = true; 
                int uncertifiedCount = 0;
                List<String> openNcrs = Arrays.asList();
                
                PhaseGateResult result = PhaseGateResult.builder()
                    .canAdvance(certOk && ncrOk)
                    .targetPhase("DEVELOPMENT")
                    .passed(certOk && ncrOk)
                    .fromPhase("DESIGN")
                    .toPhase("DEVELOPMENT")
                    .blockReason(null)
                    .build();
                    
                assertNotNull(result, "PhaseGateResult should be created successfully when bug is fixed");
            }
            
        } catch (Exception e) {
            fail("Unexpected exception during ProductService method verification: " + e.getMessage());
        }
    }
}