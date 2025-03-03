/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.jaxb.unit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>Test for JAXB using a System Property. The test will try using the
 * default implementation inside the module.</p>
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@ServerSetup({JAXBContextSystemPropInternalTestCase.SystemPropertiesSetup.class})
@RunAsClient
public class JAXBContextSystemPropInternalTestCase extends JAXBContextTestBase {

   /**
     * Setup the system property to configure internal jaxb implementation.
     */
    static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {

        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[] {
                new DefaultSystemProperty(JAXB_FACTORY_PROP_NAME, DEFAULT_JAXB_FACTORY_CLASS),
                new DefaultSystemProperty(JAKARTA_FACTORY_PROP_NAME, JAKARTA_JAXB_FACTORY_CLASS)
            };
        }
    }

    @Deployment(name = "app-internal", testable = false)
    public static WebArchive createInternalDeployment() {
        return JAXBContextTestBase.createInternalDeployment();
    }

    @OperateOnDeployment("app-internal")
    @Test
    public void testInternal() throws Exception {
        testDeafultImplementation(url);
    }
}
