/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */
package fish.payara.samples.jaxws.endpoint.ejb;

import static fish.payara.samples.PayaraVersion.PAYARA_5_193;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;

import fish.payara.samples.CliCommands;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.SincePayara;

/**
 * @author Arjan Tijms
 */
@RunWith(PayaraArquillianTestRunner.class)
@FixMethodOrder(NAME_ASCENDING)
@SincePayara(PAYARA_5_193)
public class JAXWSEndPointTest {
    
    @ArquillianResource
    private URL url;
    
    private URL rootUrl;
    
    @Inject
    private TraceMonitor traceMonitor;

    private static Service jaxwsEndPointService;

    @Deployment
    public static WebArchive createDeployment() {
        
        List<String> tracingCmd = new ArrayList<>();
        
        tracingCmd.add("set-requesttracing-configuration");
        tracingCmd.add("--thresholdValue=25");
        tracingCmd.add("--enabled=true");
        tracingCmd.add("--target=server-config");
        tracingCmd.add("--thresholdUnit=MICROSECONDS");
        tracingCmd.add("--dynamic=true");

        CliCommands.payaraGlassFish(tracingCmd);
        

        List<String> cdiCmd = new ArrayList<>();
        
        cdiCmd.add("notification-cdieventbus-configure");
        cdiCmd.add("--loopBack=true");
        cdiCmd.add("--dynamic=true");
        cdiCmd.add("--enabled=true");
        cdiCmd.add("--hazelcastEnabled=true");

        CliCommands.payaraGlassFish(cdiCmd);
        
        
        return ShrinkWrap.create(WebArchive.class).
            addPackage(JAXWSEndPointImplementation.class.getPackage());
    }

    @Before
    public void setupClass() throws MalformedURLException {
        rootUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "");
        
        jaxwsEndPointService = Service.create(
            // The WSDL file used to create this service is fetched from the application we deployed
            // above using the createDeployment() method.
                
            new URL(rootUrl, "JAXWSEndPointImplementationService/JAXWSEndPointImplementation?wsdl"),
            new QName("http://ejb.endpoint.jaxws.samples.payara.fish/", "JAXWSEndPointImplementationService"));
    }

    @Test
    @RunAsClient
    public void test1RequestFromClient() throws MalformedURLException {
        assertEquals(
          "Hi Payara!", 
            jaxwsEndPointService.getPort(JAXWSEndPointInterface.class).sayHi("Payara!"));
    }

    @Test
    // Runs on Server
    public void test2ServerCheck() throws MalformedURLException {
        assertEquals(true, traceMonitor.isObserverCalled());
    }
    
    @AfterClass
    public static void cleanup() {
        List<String> tracingCmd = new ArrayList<>();
        
        tracingCmd.add("set-requesttracing-configuration");
        tracingCmd.add("--enabled=false");
        tracingCmd.add("--dynamic=true");

        CliCommands.payaraGlassFish(tracingCmd);
        

        List<String> cdiCmd = new ArrayList<>();
        
        cdiCmd.add("notification-cdieventbus-configure");
        cdiCmd.add("--enabled=false");
        cdiCmd.add("--dynamic=true");

        CliCommands.payaraGlassFish(cdiCmd);
    }
   
}
