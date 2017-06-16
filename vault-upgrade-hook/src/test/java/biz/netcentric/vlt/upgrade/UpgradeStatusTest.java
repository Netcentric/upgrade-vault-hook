/*
 * (C) Copyright 2016 Netcentric AG.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package biz.netcentric.vlt.upgrade;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallContext.Phase;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeStatusTest {

    @Rule
    public final SlingContext sling = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InstallContext ctx;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UpgradeInfo info;

    @Mock
    private UpgradeAction action;

    private Session session;

    private UpgradeStatus status;

    @Before
    public void setup() throws Exception {
        session = sling.resourceResolver().adaptTo(Session.class);
        Mockito.when(ctx.getSession()).thenReturn(session);

        sling.build().resource("/test", JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE, //
                UpgradeStatus.PN_VERSION, "1");
        status = new UpgradeStatus(ctx, "/test");
    }

    @Test
    public void testConstructor() throws Exception {
        Assert.assertEquals("test", status.getNode().getName());
        Assert.assertEquals("1", status.getVersion().toString());

        status = new UpgradeStatus(ctx, "/create-test");
        Assert.assertTrue(session.nodeExists("/create-test"));
        Assert.assertEquals("create-test", status.getNode().getName());
        Assert.assertNull(status.getVersion());
    }

    @Test
    public void testIsExecuted() throws Exception {
        Mockito.when(info.getNode().getName()).thenReturn("testInfo");

        Assert.assertFalse(status.isExecuted(ctx, info, "test"));

        sling.build().resource("/test/testInfo");
        Assert.assertFalse(status.isExecuted(ctx, info, "test"));

        sling.build().resource("/test/testInfo", UpgradeStatus.PN_ACTIONS, new String[] { "test1", "test2" });
        Assert.assertFalse(status.isExecuted(ctx, info, "test"));
        Assert.assertTrue(status.isExecuted(ctx, info, "test1"));
    }

    @Test
    public void testGeneralUpdate() throws Exception {
        Mockito.when(ctx.getPackage().getId().getVersionString()).thenReturn("2.1.0");
        status.update(ctx);
        Assert.assertEquals("2.1.0",
                JcrUtils.getStringProperty(session, "/test/" + UpgradeStatus.PN_VERSION, "failed"));
        Assert.assertNotNull(JcrUtils.getDateProperty(session, "/test/" + UpgradeStatus.PN_UPGRADE_TIME, null));
    }

    @Test
    public void testInfoUpdate() throws Exception {
        UpgradeAction action1 = Mockito.mock(UpgradeAction.class);
        Mockito.when(action1.getName()).thenReturn("test1");
        UpgradeAction action2 = Mockito.mock(UpgradeAction.class);
        Mockito.when(action2.getName()).thenReturn("test2");
        Map<Phase, List<UpgradeAction>> actions = new LinkedHashMap<>();
        actions.put(Phase.INSTALL_FAILED, Arrays.asList(action1));
        actions.put(Phase.END, Arrays.asList(action2));
        Mockito.when(info.getActions()).thenReturn(actions);
        Mockito.when(info.getNode().getName()).thenReturn("testInfo");
        status.update(ctx, info);
        Assert.assertArrayEquals(new String[] { "test1", "test2" },
                toStringArray(session.getProperty("/test/testInfo/" + UpgradeStatus.PN_ACTIONS).getValues()));
    }

    private String[] toStringArray(Value[] values) throws Exception {
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strings[i] = values[i].getString();
        }
        return strings;
    }

}
