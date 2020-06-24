/*
 * (C) Copyright 2017 Netcentric AG.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package biz.netcentric.vlt.upgrade.groovy;

import org.apache.sling.testing.clients.ClientException;
import org.junit.Before;
import org.junit.Test;

public class GroovyPrepareFailedIT extends GroovyAbstractIT {

    private String testPropertyValue;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testPropertyValue = System.getProperty("vaultUpgradeHook.testpackage.groovy.preparefailed.testPropertyValue", "prepare_preparefailed_end");
    }

    @Test
    public void shouldExecuteFailedScript() throws ClientException, InterruptedException {
        assertTestPropertyValue(testResourcePath, testPropertyName, testPropertyValue);
        assertFailedStatus();
    }

    protected String getPackageName() {
        return System.getProperty("vaultUpgradeHook.testpackage.groovy.preparefailed", "it-groovy_prepare_failed");
    }
}
