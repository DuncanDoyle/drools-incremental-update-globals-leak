package org.jboss.ddoyle.drools.demo;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.drools.compiler.CommonTestMethodBase;
import org.drools.core.ClockType;
import org.drools.core.base.MapGlobalResolver;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.runtime.Globals;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.builder.conf.RuleEngineOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieSessionIncrementalUpdateGlobalsLeakTest extends CommonTestMethodBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(KieSessionIncrementalUpdateGlobalsLeakTest.class);

	private static final String KMODULE_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<kmodule xmlns=\"http://jboss.org/kie/6.0.0/kmodule\">"
			+ "<kbase name=\"rules\" equalsBehavior=\"equality\" eventProcessingMode=\"stream\" default=\"true\">"
			+ "<ksession name=\"ksession-rules\" default=\"true\" type=\"stateful\" clockType=\"pseudo\"/>" + "</kbase>" + "</kmodule>";

	/**
	 * Tests the original rules.
	 */
	@Test
	public void testOriginalRules() throws Exception {

		System.setProperty(RuleEngineOption.PROPERTY_NAME, RuleEngineOption.PHREAK.name());
		System.setProperty(EventProcessingOption.PROPERTY_NAME, EventProcessingOption.STREAM.getMode());
		System.setProperty(ClockTypeOption.PROPERTY_NAME, ClockType.PSEUDO_CLOCK.toString());
		System.setProperty("drools.dateformat", "dd/MM/yyyy HH:mm:ss");

		KieServices kieServices = KieServices.Factory.get();
		ReleaseId releaseId = kieServices.newReleaseId("org.kie", "test-globals", "1.0.0");

		// @formatter:off
		String drlOne = "package org.jboss.ddoyle.drools.cep.sample;\n " + 
				"import org.jboss.ddoyle.drools.demo.model.v1.SimpleEvent;\n"+ 
				"import org.jboss.ddoyle.drools.demo.model.v1.AnotherEvent;\n" +
				
				"declare SimpleEvent\n" + 
					"@role( event )\n" + 
					"@timestamp( timestamp )\n" + 
					"@expires( 2d )\n" + 
				"end\n" + 
				
				"declare AnotherEvent\n" + 
					"@role( event )\n" + 
					"@timestamp( timestamp )\n" + 
					"@expires( 2d )\n" + 
				"end\n" +

				"global java.util.Map myGlobalMap\n" +

				"rule \"SimpleTestRule-One\"\n" + 
				"when\n" + "$a:AnotherEvent(code==\"MY_CODE\")\n" + 
					"not SimpleEvent(this after [0,10s] $a)\n" + 
					"not SimpleEvent()\n" + 
				"then\n" + 
					"System.out.println(\"Rule Two: There's no event matching the following event within 10 seconds: \" + $a);\n" + 
				"end\n";

		String drlTwo = "package org.jboss.ddoyle.drools.cep.sample;\n " + 
				"import org.jboss.ddoyle.drools.demo.model.v1.SimpleEvent;\n" + 
				"import org.jboss.ddoyle.drools.demo.model.v1.AnotherEvent;\n" +

				"declare SimpleEvent\n" + 
					"@role( event )\n" + 
					"@timestamp( timestamp )\n" + 
					"@expires( 2d )\n" + 
				"end\n" +

				"declare AnotherEvent\n" + 
					"@role( event )\n" + 
					"@timestamp( timestamp )\n" + 
					"@expires( 2d )\n" + 
				"end\n" +

				"global java.util.Map myOtherGlobalMap\n" +

				"rule \"SimpleTestRule-One\"\n" + 
				"when\n" + 
					"$a:AnotherEvent(code==\"MY_CODE\")\n" + 
					"not SimpleEvent(this after [0,10s] $a)\n" + 
				"then\n" + 
					"System.out.println(\"Rule Two: There's no event matching the following event within 10 seconds: \" + $a);\n" + 
				"end\n";

		// @formatter:on

		Resource drlOneResource = kieServices.getResources().newReaderResource(new StringReader(drlOne));
		drlOneResource.setTargetPath("rules.drl");

		createAndDeployJar(kieServices, KMODULE_CONTENT, releaseId, drlOneResource);

		KieContainer kieContainer = kieServices.newKieContainer(releaseId);

		KieSession kieSession = kieContainer.newKieSession();
		try {

			Map<String, String> myGlobalMap = new HashMap<>();
			kieSession.setGlobal("myGlobalMap", myGlobalMap);

			Globals globals = kieSession.getGlobals();
			// We should only have one global.
			assertEquals(1, globals.getGlobalKeys().size());

			// Add new KJAR and update the KieContainer to incrementally update the KieSession.
			Resource drlTwoResource = kieServices.getResources().newReaderResource(new StringReader(drlTwo));
			drlTwoResource.setTargetPath("rules.drl");

			createAndDeployJar(kieServices, KMODULE_CONTENT, releaseId, drlTwoResource);

			kieContainer.updateToVersion(releaseId);
			kieSession.fireAllRules();

			// Insert the new global
			Map<String, String> myOtherGlobalMap = new HashMap<>();
			kieSession.setGlobal("myOtherGlobalMap", myOtherGlobalMap);

			/*
			 * The updated kieBase does no longer have the 'myGlobalMap' global defined, so it should also have been removed from the
			 * Globals to prevent memory leakage on incremental updates.
			 */
			globals = kieSession.getGlobals();
			assertEquals(1, globals.getGlobalKeys().size());

			// And as a last test, test that the old globals has been removed.
			assertNull(kieSession.getGlobal("myGlobalMap"));

		} finally {
			kieSession.dispose();
		}
	}

}
