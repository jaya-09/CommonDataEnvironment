import java.lang.reflect.Method;

public class SimpleTest {
    public static void main(String[] args) {
        System.out.println("Checking PhaseGateResult builder methods...");
        
        // This will work because we're not trying to use the missing methods,
        // just checking if they exist via reflection
        
        try {
            // Create a PhaseGateResult builder
            Object builder = com.cde.plm.dto.PhaseGateResult.builder();
            Class<?> builderClass = builder.getClass();
            
            System.out.println("Builder class: " + builderClass.getName());
            
            // Check for missing methods
            String[] methodsToCheck = {
                "certCheckPassed", "ncrCheckPassed", "uncertifiedCount", "openCriticalNcrs"
            };
            
            for (String methodName : methodsToCheck) {
                try {
                    if (methodName.equals("uncertifiedCount")) {
                        builderClass.getMethod(methodName, int.class);
                    } else if (methodName.equals("openCriticalNcrs")) {
                        builderClass.getMethod(methodName, java.util.List.class);
                    } else {
                        builderClass.getMethod(methodName, boolean.class);
                    }
                    System.out.println("✓ " + methodName + " method exists");
                } catch (NoSuchMethodException e) {
                    System.out.println("✗ " + methodName + " method MISSING");
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}