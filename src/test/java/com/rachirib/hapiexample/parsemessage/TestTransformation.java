package com.rachirib.hapiexample.parsemessage;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.rachirib.hapiexample.transform.VersionTwoToFHIR;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import ca.uhn.hl7v2.HL7Exception;

public class TestTransformation {

	private static final String MSG_ADTA05 = "MSH|^~\\&|REGADT|MCM|IFENG||199601061000||ADT^A05|000001|P|2.3|||\r"
			+ "EVN|A05|199601061000|199601101400|01|12312^SMITH^JHON^^^MD|199601061000\r"
			+ "PID|||191919^^^GENHOSP|253763|MASSIE^JAMES^A||19560129|M|||171 ZOBERLEIN^^ISHPEMING^MI^49849^\"\"^||(900)485-5344|(900)485-5344||S|C|10199925|371-66-9256||\r"
			+ "NK1|1|MASSIE^ELLEN|SPOUSE|171 ZOBERLEIN^^ISHPEMING^MI^49849^\"\"^|(900)485-5344|(900)545-1234~(900)545-1200|EC^EMERGENCY CONTACT\r"
			+ "NK1|2|MASSIE^MARYLOU|MOTHER|300 ZOBERLEIN^^ISHPEMING^MI^49849^\"\"^|(900)485-5344|(900)545-1234~(900)545-1200|EC^EMERGENCY CONTACT\r"
			+ "NK1|3|\r"
			+ "NK1|4|||123 INDUSTRY WAY^^ISHPEMING^MI^49849^\"\"^||(900)545-1200|EM^EMPLOYER|19940605||PROGRAMMER|||ACME SOFTWARE COMPANY\r"
			+ "PV1||O|PREOP^101^1^1^^^S|R|||0148^ADDISON,JAMES|0148^ADDISON,JAMES|0148^ADDISON,JAMES|AMB||||||||0148^ADDISON,JAMES|S|1400|A|||||||||||||||||GENHOSP||||||\r"
			+ "PV2||||||||199601101400||||||||||||||||||||||||||199601101400\r"
			+ "OBX||ST|1010.1^BODY WEIGHT||62|kg\r"
			+ "OBX||ST|1010.1^HEIGHT||190|cm\r"
			+ "DG1|1||309567004^Toe problem^SNM|||A\r"
			+ "GT1|1||MASSIE^JAMES^\"\"^\"\"^\"\"\"^\"\"^||171 ZOBERLEIN^^ISHPEMING^MI^49849^\"\"^|(900)485-5344|(900)485-5344||||SELF|371-66-925||||MOOSES AUTOCLINIC|171 ZOBERLEIN^^ISHPEMING^MI^49849^\"\"|(900)485-5344\r"
			+ "IN1|1|0|BC1|BLUE CROSS|171 ZOBERLEIN^^ISHPEMING^M149849^\"\"^||(900)485-5344|90||||||50 OK\r"
			+ "IN1|2|\"\"|\"\"\r";
	
	

	private static final String MSG_ADTA05_QLD = "MSH|^~\\&|REGADT|MCM|IFENG||199601061000||ADT^A05|000001|P|2.3|||\r"
			+ "EVN|A05|199601061000|199601101400|01|12312^SMITH^LINDSEY|199601061000\r"
			+ "PID|||191919^^^GENHOSP|253763|SPARROW^ROBBERT^A||19560129|M|||171 ZOBERLEIN^^ISHPEMING^MI^49849^\"\"||(900)485-5344|(900)583-1221||M|C|10199925|371-66-9256\r"
			+ "PV1||O||R|45676688||0148^ADDISON,JAMES|||AMB\r"
			+ "DG1|1||309567004^Toe problem^SNM|||A|\r";

	@Test
	public void test_transformation() throws HL7Exception {

		VersionTwoToFHIR transFHIR = new VersionTwoToFHIR();

		FhirContext ctx = new FhirContext();
		Bundle bundle = transFHIR.transformADTA05(MSG_ADTA05, ctx);
		assertNotNull(bundle);

		printAndValidate(ctx, bundle);

	}
	
	@Test
	public void test_transformation_qld() throws HL7Exception {

		VersionTwoToFHIR transFHIR = new VersionTwoToFHIR();

		FhirContext ctx = new FhirContext();
		Bundle bundle = transFHIR.transformADTA05(MSG_ADTA05_QLD, ctx);
		assertNotNull(bundle);

		printAndValidate(ctx, bundle);

	}

	private void printAndValidate(FhirContext ctx, Bundle bundle) {
		IParser parser = ctx.newJsonParser();
		// IParser parser = ctx.newXmlParser();
		parser.setPrettyPrint(true);
		String encoded = parser.encodeResourceToString(bundle);
		System.out.println(encoded);

		// Request a validator and apply it
		FhirValidator val = ctx.newValidator();

		ValidationResult result = val.validateWithResult(bundle);
		if (result.isSuccessful()) {

			System.out.println("Validation passed");

		} else {

			System.out.println("Validation failed");
			// The result contains an OperationOutcome outlining the failures
			String results = ctx.newXmlParser().setPrettyPrint(true)
					.encodeResourceToString(result.getOperationOutcome());
			System.out.println(results);

			// We failed validation!
			assertNotNull("Validation failed", null);
		}
	}
	
	

}
