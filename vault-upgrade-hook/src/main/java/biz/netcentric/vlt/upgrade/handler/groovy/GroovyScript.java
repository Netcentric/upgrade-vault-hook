/*
 * (C) Copyright 2016 Netcentric AG.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package biz.netcentric.vlt.upgrade.handler.groovy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallContext.Phase;
import org.apache.sling.api.SlingHttpServletRequest;

import com.icfolson.aem.groovy.console.GroovyConsoleService;
import com.icfolson.aem.groovy.console.response.RunScriptResponse;

import biz.netcentric.vlt.upgrade.UpgradeAction;
import biz.netcentric.vlt.upgrade.handler.OsgiUtil.ServiceWrapper;
import biz.netcentric.vlt.upgrade.handler.SlingUtils;
import biz.netcentric.vlt.upgrade.util.PackageInstallLogger;
import org.apache.sling.api.SlingHttpServletResponse;

public class GroovyScript extends UpgradeAction {

    private static final PackageInstallLogger LOG = PackageInstallLogger.create(GroovyScript.class);

    SlingUtils sling = new SlingUtils();
    private final Node script;

    public GroovyScript(final Node script, final Phase defaultPhase) throws RepositoryException {
        super(script.getName(), UpgradeAction.getPhaseFromPrefix(defaultPhase, script.getName()), getDataMd5(script));
        this.script = script;
    }

    @Override
    public void execute(final InstallContext ctx) throws RepositoryException {
        final SlingHttpServletRequest request = getRequestForScript();
        if (request != null) {
            LOG.debug(ctx, "Executing [{}]", getName());
            final RunScriptResponse scriptResponse = run(request);
            LOG.debug(ctx, "Executed script [{}]: [{}]\n{}\n---\n", getName(), scriptResponse.getRunningTime(),
                    scriptResponse.getExceptionStackTrace());
            if (scriptResponse.getExceptionStackTrace() != null && scriptResponse.getExceptionStackTrace().trim().length() > 0) {
                throw new RuntimeException(
                        "Error executing script " + getName() + "\n" + scriptResponse.getExceptionStackTrace());
            } else {
                LOG.info(ctx, "Executed [{}]: [{}]\n{}\n---\n", getName(), scriptResponse.getRunningTime(),
                        scriptResponse.getOutput().trim());
            }
        }
    }

    protected RunScriptResponse run(final SlingHttpServletRequest request) {
        try (ServiceWrapper<GroovyConsoleService> serviceWrapper = sling.getService(GroovyConsoleService.class)) {
            Class consoleServiceClass = GroovyConsoleService.class;

            try {
                Method runScript = consoleServiceClass.getDeclaredMethod("runScript", SlingHttpServletRequest.class, SlingHttpServletResponse.class);
                if (Modifier.isPublic(runScript.getModifiers()) && runScript.getReturnType().equals(RunScriptResponse.class)) {
                    return (RunScriptResponse) runScript.invoke(serviceWrapper.getService(), request, FakeResponse.INSTANCE);
                }
            } catch (NoSuchMethodException nsm) {
                // do nothing, this is an expected case
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                Method runScript = consoleServiceClass.getDeclaredMethod("runScript", SlingHttpServletRequest.class);
                if (Modifier.isPublic(runScript.getModifiers()) && runScript.getReturnType().equals(RunScriptResponse.class)) {
                    return (RunScriptResponse) runScript.invoke(serviceWrapper.getService(), request);
                }
            } catch (NoSuchMethodException nsm) {
                // do nothing, this is an expected case
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return null;
        }
    }

    protected SlingHttpServletRequest getRequestForScript() throws RepositoryException {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("script", getScriptContent(script));
        parameters.put("scriptPath", script.getPath());
        parameters.put("data", getData());
        return new FakeRequest(sling.getResourceResolver(script.getSession()), "GET", "/bin/groovyconsole/post.json",
                parameters);
    }

    private String getData() throws RepositoryException {
        Node parent = script.getParent();
        return parent.hasProperty("data") ? parent.getProperty("data").getString() : "";
    }

}
