import java.lang.reflect.Method;
import java.util.List;

/**
 * Standalone Bug Condition Exploration Test Runner
 * 
 * This demonstrates the missing PhaseGateResult builder methods that cause
 * ProductService compilation to fail.
 */
public class RunBugTest {
    
    public static void main(String[] args) {
        System.out.println("=== PhaseGateResult Bug Condition Exploration Test ===");
        System.out.println("Property 1: Fault Condition - Missing Builder Methods Compilation Failure");
        System.out.println("Validates: Requirements 2.1, 2.2, 2.3\n");
        
        runBugConditionExploration();
    }
    
    public static void runBugConditionExploration() {
        System.out.println("Testing for missing builder methods that cause ProductService.checkPhaseGate() to fail...\n");
        
        try {
            // Load the PhaseGateResult class
            Class<?> phaseGateResultClass = Class.forName("com.cde.plm.dto.PhaseGateResult");
            
            // Get a builder instance to check its methods
            Method builderMethod = phaseGateResultClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            Class<?> builderClass = builder.getClass();
            
            System.out.println("PhaseGateResult.PhaseGateResultBuilder class: " + builderClass.getName());
            System.out.println("Checking for missing methods that ProductService tries to call...\n");
            
            // Track missing methods
            int missingCount = 0;
            StringBuilder missingMethods = new StringBuilder();
            
            // Test case 1: certCheckPassed(boolean) - ProductService line 194
            try {
                Method certCheckPassed = builderClass.getMethod("certCheckPassed", boolean.class);
                System.out.println("✓ certCheckPassed(boolean) method found");
            } catch (NoSuchMethodException e) {
                missingCount++;
                missingMethods.append("certCheckPassed(boolean) ");
                System.out.println("✗ MISSING: certCheckPassed(boolean) - ProductService line 194 will fail");
                System.out.println("   Error: " + e.getMessage());
            }
            
            // Test case 2: ncrCheckPassed(boolean) - ProductService line 194  
            try {
                Method ncrCheckPassed = builderClass.getMethod("ncrCheckPassed", boolean.class);
                System.out.println("✓ ncrCheckPassed(boolean) method found");
            } catch (NoSuchMethodException e) {
                missingCount++;
                missingMethods.append("ncrCheckPassed(boolean) ");
                System.out.println("✗ MISSING: ncrCheckPassed(boolean) - ProductService line 194 will fail");
                System.out.println("   Error: " + e.getMessage());
            }
            
            // Test case 3: uncertifiedCount(int) - ProductService line 195
            try {
                Method uncertifiedCount = builderClass.getMethod("uncertifiedCount", int.class);
                System.out.println("✓ uncertifiedCount(int) method found");
            } catch (NoSuchMethodException e) {
                missingCount++;
                missingMethods.append("uncertifiedCount(int) ");
                System.out.println("✗ MISSING: uncertifiedCount(int) - ProductService line 195 will fail");
                System.out.println("   Error: " + e.getMessage());
            }
            
            // Test case 4: openCriticalNcrs(List) - ProductService line 195
            try {
                Method openCriticalNcrs = builderClass.getMethod("openCriticalNcrs", List.class);
                System.out.println("✓ openCriticalNcrs(List) method found");
            } catch (NoSuchMethodException e) {
                missingCount++;
                missingMethods.append("openCriticalNcrs(List) ");
                System.out.println("✗ MISSING: openCriticalNcrs(List) - ProductService line 195 will fail");
                System.out.println("   Error: " + e.getMessage());
            }
            
            System.out.println("\n=== BUG CONDITION EXPLORATION RESULTS ===");
            
            if (missingCount == 0) {
                System.out.println("STATUS: ✓ ALL METHODS FOUND - Bug appears to be FIXED!");
                System.out.println("ProductService.checkPhaseGate() should compile successfully.");
                System.out.println("Test Result: PASS (bug is fixed)");
            } else {
                System.out.println("STATUS: ✗ BUG CONFIRMED - " + missingCount + " methods missing");
                System.out.println("Missing methods: " + missingMethods.toString().trim());
                System.out.println("ProductService.checkPhaseGate() compilation will FAIL with errors:");
                System.out.println("- cannot find symbol: method certCheckPassed(boolean)");
                System.out.println("- cannot find symbol: method ncrCheckPassed(boolean)");
                System.out.println("- cannot find symbol: method uncertifiedCount(int)");
                System.out.println("- cannot find symbol: method openCriticalNcrs(java.util.List)");
                System.out.println("\nROOT CAUSE: PhaseGateResult class missing required fields:");
                System.out.println("- public boolean certCheckPassed;");
                System.out.println("- public boolean ncrCheckPassed;");
                System.out.println("- public int uncertifiedCount;");
                System.out.println("- public List<String> openCriticalNcrs;");
                System.out.println("\nTest Result: FAIL (confirms bug exists - this is EXPECTED on unfixed code)");
            }
            
        } catch (Exception e) {
            System.out.println("ERROR: Could not run bug condition exploration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}