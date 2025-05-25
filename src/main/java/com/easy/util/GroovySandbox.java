package com.easy.util;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.stereotype.Component;

import java.io.File;
import java.security.AccessControlException;
import java.util.Map;

@Component
public class GroovySandbox {

    private final CompilerConfiguration compilerConfiguration;

    public GroovySandbox() {
        this.compilerConfiguration = new CompilerConfiguration();
        // Set basic script base class if you want to inject common methods/objects
        // compilerConfiguration.setScriptBaseClass(MyScriptBaseClass.class.getName());

        // Important: Limit access to dangerous classes/methods
        // This is a minimal example. A real sandbox needs to be much more restrictive.
        // For example, disallow System.exit(), file I/O, network I/O, reflection.
        // Groovy's SecureASTCustomizer can be used for more fine-grained control.
        // For production, consider using a custom SecurityManager.
        // compilerConfiguration.addCompilationCustomizer(new SecureASTCustomizer() {
        //     // Configure rules to disallow certain imports, methods, etc.
        // });
    }

    /**
     * Runs a Groovy script in a (basic) sandboxed environment.
     *
     * @param scriptContent The Groovy code to execute.
     * @param params A map of parameters to bind to the script.
     * @return The result of the script execution.
     * @throws RuntimeException if the script encounters an error or violates sandbox rules.
     */
    public Object runScript(String scriptContent, Map<String, Object> params) {
        Binding binding = new Binding();
        if (params != null) {
            params.forEach(binding::setVariable);
        }

        GroovyShell shell = new GroovyShell(binding, compilerConfiguration);

        try {
            // Set a temporary SecurityManager to restrict permissions
            // This is a global setting and might affect other parts of the application.
            // A more isolated approach is needed for robust production use.
            SecurityManager originalSecurityManager = System.getSecurityManager();
            try {
                // A very restrictive policy. You'll need to define a custom policy file
                // and grant specific permissions if your scripts need access to benign resources.
                System.setSecurityManager(new SecurityManager() {
                    @Override
                    public void checkExit(int status) {
                        throw new SecurityException("System exit forbidden.");
                    }

                    @Override
                    public void checkPermission(java.security.Permission perm) {
                        // Allow basic permissions for Groovy itself
                        if (perm instanceof java.lang.RuntimePermission && "exitVM".equals(perm.getName())) {
                            throw new AccessControlException("Exit VM forbidden.");
                        }
                        // Disallow file I/O, network I/O unless explicitly granted.
                        // For a real sandbox, you'd define a policy file with allowed operations.
                        // For now, this is very restrictive.
                        if (perm instanceof java.io.FilePermission || perm instanceof java.net.SocketPermission) {
                            throw new AccessControlException("Forbidden operation: " + perm.getName());
                        }
                        // Default to allowing everything else if not explicitly restricted.
                        // THIS IS DANGEROUS IN PRODUCTION.
                        // You should either use a custom SecurityManager with fine-grained control
                        // or better, a library specifically designed for sandboxing.
                    }

                    @Override
                    public void checkExec(String cmd) {
                        throw new AccessControlException("Process execution forbidden.");
                    }
                });

                // Evaluate the script
                return shell.evaluate(scriptContent);
            } finally {
                // Restore original security manager
                System.setSecurityManager(originalSecurityManager);
            }
        } catch (AccessControlException e) {
            throw new SecurityException("Script tried to perform a forbidden operation: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing custom Groovy script: " + e.getMessage(), e);
        }
    }
}
