package com.rachirib.hapiexample.transform;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.DurationDt;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Encounter.Hospitalization;
import ca.uhn.fhir.model.dstu2.resource.MessageHeader;
import ca.uhn.fhir.model.dstu2.resource.MessageHeader.Destination;
import ca.uhn.fhir.model.dstu2.resource.Organization;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.Patient.Contact;
import ca.uhn.fhir.model.dstu2.resource.Practitioner;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionClinicalStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.EncounterClassEnum;
import ca.uhn.fhir.model.dstu2.valueset.EncounterStateEnum;
import ca.uhn.fhir.model.dstu2.valueset.PractitionerRoleEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.CodeDt;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.datatype.CN;
import ca.uhn.hl7v2.model.v23.datatype.CX;
import ca.uhn.hl7v2.model.v23.datatype.ID;
import ca.uhn.hl7v2.model.v23.datatype.XAD;
import ca.uhn.hl7v2.model.v23.datatype.XCN;
import ca.uhn.hl7v2.model.v23.datatype.XPN;
import ca.uhn.hl7v2.model.v23.message.ADT_A05;
import ca.uhn.hl7v2.model.v23.segment.DG1;
import ca.uhn.hl7v2.model.v23.segment.EVN;
import ca.uhn.hl7v2.model.v23.segment.MSH;
import ca.uhn.hl7v2.model.v23.segment.NK1;
import ca.uhn.hl7v2.model.v23.segment.PID;
import ca.uhn.hl7v2.model.v23.segment.PV1;
import ca.uhn.hl7v2.model.v23.segment.PV2;
import ca.uhn.hl7v2.parser.Parser;

public class VersionTwoToFHIR {

	private static final String CONDITION_ENCOUNTER = "diagnosis";
	private static final String SYSTEM_FHIR_CONDITION_CATEGORY = "http://hl7.org/fhir/condition-category";
	private static final String SYSTEM_SNOMED_NAMESPACE = "http://snomed.info/sct";
	private static final String SYSTEM_FHIR_PHYSICAL_TYPE = "http://hl7.org/fhir/vs/location-physical-type";
	private static final String SYSTEM_V2_EVENT = "http://hl7.org/fhir/v2/0003";
	private static final String SYSTEM_V2_RELATIONSHIP = "http://hl7.org/fhir/v2/0063";
	private static final String SYSTEM_V2_MARITAL_STATUS = "http://hl7.org/fhir/v2/0002";
	private static final String SYSTEM_V2_DIET = "http://hl7.org/fhir/v2/0159";
	private static final String SYSTEM_V2_IDENTIFIER = "http://hl7.org/fhir/v2/0203";
	private static final String SYSTEM_V2_ADMISSION_TYPE = "http://hl7.org/fhir/v2/0007";

	/**
	 * Method transform ADTA05 v2 Messages to FHIR
	 * 
	 * @param msg
	 * @return
	 * @throws HL7Exception
	 */
	public Bundle transformADTA05(String msg, FhirContext ctx)
			throws HL7Exception {

		@SuppressWarnings("resource")
		HapiContext context = new DefaultHapiContext();

		Parser p = context.getGenericParser();

		Message hapiMsg;

		hapiMsg = p.parse(msg);

		ADT_A05 adtMsg = (ADT_A05) hapiMsg;

		Bundle bundle = new Bundle();
		bundle.setId("201501013000");
		bundle.setType(BundleTypeEnum.MESSAGE);

		Practitioner operator = transformOperator(adtMsg.getEVN());
		Patient patient = transformPatient(adtMsg.getPID(), adtMsg.getNK1All());
		
		MessageHeader header = transformHeader(adtMsg.getMSH(), adtMsg.getEVN()
				.getEventReasonCode(), operator);
		

		List<Practitioner> listParticipants = new ArrayList<Practitioner>();
		listParticipants.add(transformPractitioner(adtMsg.getPV1()
				.getAttendingDoctor(0)));

		Encounter encounter = transformEncounter(adtMsg.getPV1(),
				adtMsg.getPV2(), adtMsg.getEVN().getEventReasonCode(), patient,
				listParticipants);
		
		Condition condition = transformCondition(encounter, patient,
				adtMsg.getDG1());

		bundle.addEntry().setResource(header);
		bundle.addEntry().setResource(condition);

		return bundle;
	}


	/**
	 * Transform Condition Patient when the encounter is requested
	 * 
	 * @return
	 * @throws HL7Exception
	 */
	private Condition transformCondition(Encounter encounter, Patient patient,
			DG1 dg1) throws HL7Exception {

		Condition condition = new Condition();

		condition.addIdentifier().setValue(
				dg1.getDg11_SetIDDiagnosis().getValue());

		condition.setEncounter(new ResourceReferenceDt(encounter));
		condition.setPatient(new ResourceReferenceDt(patient));
		condition.setCode(new CodeableConceptDt(SYSTEM_SNOMED_NAMESPACE, dg1
				.getDg13_DiagnosisCode().getCe1_Identifier().getValue()));
		condition.setCategory(new CodeableConceptDt(
				SYSTEM_FHIR_CONDITION_CATEGORY, CONDITION_ENCOUNTER));
		condition.setClinicalStatus(ConditionClinicalStatusEnum.PROVISIONAL);

		return condition;
	}

	/**
	 * Create and encounter with the send patient
	 * 
	 * @param pv1
	 * @param pv2
	 * @param patient
	 * @param evn
	 * @return
	 * @throws HL7Exception
	 */
	private Encounter transformEncounter(PV1 pv1, PV2 pv2, ID reasonCode,
			Patient patient, List<Practitioner> participants)
			throws HL7Exception {

		Encounter encounter = new Encounter();

		// PV1-19-visit number
		encounter.addIdentifier()
				.setValue(pv1.getPv119_VisitNumber().getCx1_ID().getValue())
				.setSystem(SYSTEM_V2_IDENTIFIER);

		// Status : No clear equivalent in V2.x; active/finished could be
		// inferred from PV1-44, PV1-45, PV2-24; inactive could be inferred from
		// PV2-16
		encounter.setStatus(EncounterStateEnum.PLANNED);

		// PV1-2-patient class
		if (pv1.getPv12_PatientClass().getValue() != null) {
			encounter.setClassElement(equivalentEncounterClass(pv1
					.getPv12_PatientClass().getValue()));

		}

		// PV1-4-admission type
		if (pv1.getPv14_AdmissionType().getValue() != null) {
			encounter
					.addType()
					.addCoding()
					.setCode(new CodeDt(pv1.getPv14_AdmissionType().getValue()))
					.setSystem(SYSTEM_V2_ADMISSION_TYPE);
		}

		// PID-3-patient ID list

		encounter.setPatient(new ResourceReferenceDt(patient));

		// participant type PRT-4-participation
		// participant individual PRT-5-participation person

		for (Practitioner participant : participants) {
			encounter.addParticipant().setIndividual(
					new ResourceReferenceDt(participant));
		}

		// (PV1-45 less PV1-44) iff ( (PV1-44 not empty) and (PV1-45 not empty)
		// ); units in minutes
		Date pv145 = pv1.getPv145_DischargeDateTime().getTimeOfAnEvent()
				.getValueAsDate();
		Date pv144 = pv1.getPv144_AdmitDateTime().getTimeOfAnEvent()
				.getValueAsDate();
		if (pv145 != null && pv144 != null) {
			DurationDt durationDt = new DurationDt();
			durationDt.setValue(pv145.getTime() - pv144.getTime());
			encounter.setLength(durationDt);
		}

		// EVN-4-event reason code / PV2-3-admit reason (note: PV2-3 is
		// nominally constrained to inpatient admissions; V2.x makes no
		// vocabulary suggestions for PV2-3; would not expect PV2 segment or
		// PV2-3 to be in use in all implementations)
		if (reasonCode.getValue() != null) {
			encounter.addReason().addCoding().setCode(reasonCode.getValue());
		}

		// PV2-25-visit priority code
		encounter.getPriority().addCoding()
				.setCode(pv2.getPv225_VisitPriorityCode().getValue());

		Hospitalization hospitalization = encounter.getHospitalization();

		// PV1-5-preadmit number
		if (pv1.getPv150_AlternateVisitID().getCx1_ID().getValue() != null) {
			hospitalization.setPreAdmissionIdentifier(new IdentifierDt(
					SYSTEM_V2_IDENTIFIER, pv1.getPv150_AlternateVisitID()
							.getCx1_ID().getValue()));
		}

		// PV1-14-admit source
		if (pv1.getPv114_AdmitSource().getValue() != null) {
			hospitalization.getAdmitSource().addCoding()
					.setCode(pv1.getPv114_AdmitSource().getValue());

		}

		if (pv1.getPv138_DietType().getValue() != null) {
			// PV1-38-diet type
			hospitalization.setDietPreference(new CodeableConceptDt(
					SYSTEM_V2_DIET, pv1.getPv138_DietType().getValue()));
		}

		// PV1-16-VIP indicator
		hospitalization.addSpecialCourtesy().addCoding()
				.setCode(pv1.getPv116_VIPIndicator().getValue());

		// PV1-15-ambulatory status / OBR-30-transportation mode /
		// OBR-43-planned patient transport comment
		if (pv1.getPv115_AmbulatoryStatus().length > 0) {
			hospitalization.addSpecialArrangement().addCoding()
					.setCode(pv1.getPv115_AmbulatoryStatus()[0].getValue());
		}

		// PV1-36-discharge disposition
		hospitalization.setDischargeDisposition(new CodeableConceptDt(pv1
				.getPv136_DischargeDisposition().getValue(), pv1
				.getPv136_DischargeDisposition().getValue()));

		if (pv1.getPv113_ReadmissionIndicator().getValue() != null) {
			// PV1-13-re-admission indicator
			hospitalization.setReAdmission(Boolean.valueOf(pv1
					.getPv113_ReadmissionIndicator().getValue()));
		}

		// Location
		// PV1-3-assigned patient location / PV1-6-prior patient
		// location / PV1-11-temporary location / PV1-42-pending location /
		// PV1-43-prior temporary location
		// Location locationEncounter = encounter.addLocation();

		// PV2-11-actual length of inpatient stay / PV1-44-admit date/time
		// PV1-45-discharge date/time
		PeriodDt periodDt = new PeriodDt();
		periodDt.setEnd(new DateTimeDt(pv1.getPv145_DischargeDateTime()
				.getTimeOfAnEvent().getValueAsDate()));
		periodDt.setStart(new DateTimeDt(pv1.getPv144_AdmitDateTime()
				.getTimeOfAnEvent().getValueAsDate()));

		if (pv1.getPv13_AssignedPatientLocation().getFacility()
				.getNamespaceID().getValue() != null) {
			ca.uhn.fhir.model.dstu2.resource.Location locationFacility = new ca.uhn.fhir.model.dstu2.resource.Location();
			CodeableConceptDt codeableConceptDtFacilityDt = new CodeableConceptDt(
					SYSTEM_FHIR_PHYSICAL_TYPE, pv1
							.getPv13_AssignedPatientLocation().getFacility()
							.getNamespaceID().getValue());
			locationFacility.setPhysicalType(codeableConceptDtFacilityDt);
			encounter.addLocation()
					.setLocation(new ResourceReferenceDt(locationFacility))
					.setPeriod(periodDt);
		}

		if (pv1.getPv13_AssignedPatientLocation().getPointOfCare().getValue() != null) {
			ca.uhn.fhir.model.dstu2.resource.Location locationPointCare = new ca.uhn.fhir.model.dstu2.resource.Location();
			CodeableConceptDt codeableConceptDtPointCareDt = new CodeableConceptDt(
					SYSTEM_FHIR_PHYSICAL_TYPE, pv1
							.getPv13_AssignedPatientLocation().getPointOfCare()
							.getValue());
			locationPointCare.setPhysicalType(codeableConceptDtPointCareDt);
			// locationPointCare.setPartOf(new
			// ResourceReferenceDt(locationFacility));
			encounter.addLocation()
					.setLocation(new ResourceReferenceDt(locationPointCare))
					.setPeriod(periodDt);
		}

		if (pv1.getPv13_AssignedPatientLocation().getBuilding().getValue() != null) {
			ca.uhn.fhir.model.dstu2.resource.Location locationBuilding = new ca.uhn.fhir.model.dstu2.resource.Location();
			CodeableConceptDt codeableConceptDtBuildingDt = new CodeableConceptDt(
					SYSTEM_FHIR_PHYSICAL_TYPE, pv1
							.getPv13_AssignedPatientLocation().getBuilding()
							.getValue());
			locationBuilding.setPhysicalType(codeableConceptDtBuildingDt);
			// locationBuilding.setPartOf(new
			// ResourceReferenceDt(locationPointCare));
			encounter.addLocation().setLocation(
					new ResourceReferenceDt(locationBuilding));
		}

		if (pv1.getPv13_AssignedPatientLocation().getFloor().getValue() != null) {
			ca.uhn.fhir.model.dstu2.resource.Location locationFloor = new ca.uhn.fhir.model.dstu2.resource.Location();
			CodeableConceptDt codeableConceptFloorDt = new CodeableConceptDt(
					SYSTEM_FHIR_PHYSICAL_TYPE, pv1
							.getPv13_AssignedPatientLocation().getFloor()
							.getValue());
			locationFloor.setPhysicalType(codeableConceptFloorDt);
			// locationFloor.setPartOf(new
			// ResourceReferenceDt(locationBuilding));
			encounter.addLocation()
					.setLocation(new ResourceReferenceDt(locationFloor))
					.setPeriod(periodDt);
		}

		if (pv1.getPv13_AssignedPatientLocation().getRoom().getValue() != null) {
			ca.uhn.fhir.model.dstu2.resource.Location locationRoom = new ca.uhn.fhir.model.dstu2.resource.Location();
			CodeableConceptDt codeableConceptRoomDt = new CodeableConceptDt(
					SYSTEM_FHIR_PHYSICAL_TYPE, pv1
							.getPv13_AssignedPatientLocation().getRoom()
							.getValue());
			locationRoom.setPhysicalType(codeableConceptRoomDt);
			// locationRoom.setPartOf(new ResourceReferenceDt(locationFloor));
			encounter.addLocation()
					.setLocation(new ResourceReferenceDt(locationRoom))
					.setPeriod(periodDt);
		}

		if (pv1.getPv13_AssignedPatientLocation().getBed().getValue() != null) {
			ca.uhn.fhir.model.dstu2.resource.Location locationBed = new ca.uhn.fhir.model.dstu2.resource.Location();
			CodeableConceptDt codeableConceptBedDt = new CodeableConceptDt(
					SYSTEM_FHIR_PHYSICAL_TYPE, pv1
							.getPv13_AssignedPatientLocation().getBed()
							.getValue());
			locationBed.setPhysicalType(codeableConceptBedDt);
			// locationBed.setPartOf(new ResourceReferenceDt(locationRoom));
			encounter.addLocation()
					.setLocation(new ResourceReferenceDt(locationBed))
					.setPeriod(periodDt);
		}

		// ServiceProvider
		// PV1-10-hospital service / PL.6 Person Location Type & PL.1 Point of
		// Care (note: V2.x definition is
		// "the treatment or type of surgery that the patient is scheduled to receive";
		// seems slightly out of alignment with the concept name 'hospital
		// service'. Would not trust that implementations apply this semantic by
		// default)

		Organization organization = new Organization();
		organization.setId(pv1.getPv110_HospitalService().getValue());
		encounter.setServiceProvider(new ResourceReferenceDt(organization));

		return encounter;
	}

	/**
	 * Transform Operator to Practitioner
	 * 
	 * @param evn
	 * @return
	 */
	private Practitioner transformOperator(EVN evn) {
		Practitioner operatorFhir = new Practitioner();
		CN operatorv2 = evn.getEvn5_OperatorID();

		if (operatorv2.getCn1_IDNumber().getValue() != null) {
			operatorFhir.addIdentifier().setSystem(SYSTEM_V2_IDENTIFIER)
					.setValue(operatorv2.getCn1_IDNumber().getValue());
			HumanNameDt nameFhirOperator = operatorFhir.getName();
			nameFhirOperator.addFamily(operatorv2.getCn2_FamilyName()
					.getValue());
			nameFhirOperator.addGiven(operatorv2.getCn3_GivenName().getValue());
			nameFhirOperator.addPrefix(operatorv2.getCn6_PrefixEgDR()
					.getValue());
			nameFhirOperator.addSuffix(operatorv2.getCn5_SuffixEgJRorIII()
					.getValue());

			operatorFhir.addPractitionerRole().setRole(
					PractitionerRoleEnum.ICT_PROFESSIONAL);

		}
		return operatorFhir;
	}

	/**
	 * Transform Doctor to Practitioner
	 * 
	 * @param evn
	 * @return
	 */
	private Practitioner transformPractitioner(XCN xcn) {
		Practitioner practitionerFhir = new Practitioner();

		if (xcn.getXcn1_IDNumber().getValue() != null) {
			practitionerFhir.addIdentifier().setSystem(SYSTEM_V2_IDENTIFIER)
					.setValue(xcn.getXcn1_IDNumber().getValue());
			HumanNameDt nameFhirOperator = practitionerFhir.getName();
			nameFhirOperator.addFamily(xcn.getXcn2_FamilyName().getValue());
			nameFhirOperator.addGiven(xcn.getXcn3_GivenName().getValue());
			nameFhirOperator.addPrefix(xcn.getXcn6_PrefixEgDR().getValue());
			nameFhirOperator
					.addSuffix(xcn.getXcn5_SuffixEgJRorIII().getValue());

			practitionerFhir.addPractitionerRole().setRole(
					PractitionerRoleEnum.DOCTOR);

		}
		return practitionerFhir;
	}

	/**
	 * Class Encounter Equivalent Table V2
	 * 
	 * @param pv2Class
	 * @return
	 */
	private EncounterClassEnum equivalentEncounterClass(String pv2Class) {
		if ("O".equalsIgnoreCase(pv2Class)) {
			return EncounterClassEnum.OUTPATIENT;
		}
		if ("I".equalsIgnoreCase(pv2Class)) {
			return EncounterClassEnum.INPATIENT;
		}
		if ("E".equalsIgnoreCase(pv2Class)) {
			return EncounterClassEnum.EMERGENCY;
		}
		if ("B".equalsIgnoreCase(pv2Class)) {
			return EncounterClassEnum.OTHER;
		}
		return null;
	}

	/**
	 * Equivalent Gender
	 * 
	 * @param pv2Gender
	 * @return
	 */
	private AdministrativeGenderEnum equivalentGender(String pv2Gender) {
		if ("M".equalsIgnoreCase(pv2Gender)) {
			return AdministrativeGenderEnum.MALE;
		}
		if ("F".equalsIgnoreCase(pv2Gender)) {
			return AdministrativeGenderEnum.FEMALE;
		}
		if ("O".equalsIgnoreCase(pv2Gender)) {
			return AdministrativeGenderEnum.OTHER;
		}
		if ("U".equalsIgnoreCase(pv2Gender)) {
			return AdministrativeGenderEnum.UNKNOWN;
		}
		return null;
	}

	/**
	 * Transform Header
	 * 
	 * @param msh
	 * @throws DataTypeException
	 */
	private MessageHeader transformHeader(MSH msh, ID reasonCode,
			Practitioner operator) throws DataTypeException {

		MessageHeader messageHeader = new MessageHeader();

		// MSH-10-message control ID
		messageHeader.setIdentifier(msh.getMsh10_MessageControlID().getValue());

		// MSH-7-message date/time
		messageHeader.setTimestamp(new InstantDt(msh
				.getMsh7_DateTimeOfMessage().getTimeOfAnEvent()
				.getValueAsDate()));

		// MSH-9.2-message type.trigger event
		messageHeader.setEvent(new CodingDt(SYSTEM_V2_EVENT, msh
				.getMsh9_MessageType().getCm_msg2_TriggerEvent().getValue()));


		MessageHeader.Source source = new MessageHeader.Source();
		// MSH-3-sending application
		source.setName(msh.getMsh3_SendingApplication().getHd1_NamespaceID()
				.getValue());
		// source software SFT-3-software product name ( +SFT-1-software
		// vendor organization)
		source.setSoftware(msh.getMsh4_SendingFacility().getHd1_NamespaceID()
				.getValue());
		// source version SFT-2-software certified version or release
		// number
		source.setVersion(msh.getMsh12_VersionID().getValue());

		source.setEndpoint("192.168.0.1");
		messageHeader.setSource(source);

		ArrayList<Destination> destinations = new ArrayList<MessageHeader.Destination>();
		MessageHeader.Destination destination = new MessageHeader.Destination();

		// MSH-5-receiving application
		destination.setName(msh.getMsh5_ReceivingApplication()
				.getHd1_NamespaceID().getValue());
		destination.setEndpoint("192.168.0.1");

		// MSH-6-receiving facility by implication)

		destinations.add(destination);
		messageHeader.setDestination(destinations);

		// enterer EVN-5-operator ID / ORC-10-entered by /
		// PRT-5-Participation Person:PRT-4-Participation='EP' / ROL where ROL.3
		// is EP or ORC.10

		messageHeader.setEnterer(new ResourceReferenceDt(operator));

		// reason EVN.4 / ORC.16 / OBR-31-reason for study / BPO-13-BP
		// indication for use / RXO-20-indication / RXE-27-give indication /
		// RXD-21-indication / RXG-22-indication / RXA-19-indication
		messageHeader
				.setReason(new CodeableConceptDt("", reasonCode.getValue()));

		return messageHeader;
	}

	/**
	 * Method to transform patient
	 * 
	 * @param pid
	 * @return
	 * @throws DataTypeException
	 */
	private Patient transformPatient(PID pid, List<NK1> listNk1)
			throws DataTypeException {

		CX[] patientIDInternalID = pid.getPatientIDInternalID();

		Patient patient = new Patient();
		IdentifierDt id = patient.addIdentifier();

		// PID-3
		id.setElementSpecificId(patientIDInternalID[0]
				.getCx5_IdentifierTypeCode().getValue());
		id.setSystem(SYSTEM_V2_IDENTIFIER);
		id.setValue(patientIDInternalID[0].getCx1_ID().getValue());
		id.setAssigner(new ResourceReferenceDt(patientIDInternalID[0]
				.getCx4_AssigningAuthority().getUniversalID().getValue()));

		// PID-5, PID-9
		XPN[] patientName = pid.getPatientName();
		HumanNameDt name = patient.addName();
		if (patientName.length > 0) {
			name.addFamily(patientName[0].getFamilyName().getValue());
			name.addGiven(patientName[0].getGivenName().getValue());
			name.addGiven(patientName[0].getMiddleInitialOrName().getValue());
		}

		// telecom PID-13, PID-14, PID-40(not v23)

		if (pid.getPid13_PhoneNumberHome().length > 0) {
			patient.addTelecom()
					.setValue(
							pid.getPid13_PhoneNumberHome()[0]
									.getXtn1_9999999X99999CAnyText().getValue())
					.setSystem(ContactPointSystemEnum.PHONE)
					.setUse(ContactPointUseEnum.HOME);
			;
		}

		if (pid.getPid14_PhoneNumberBusiness().length > 0) {
			patient.addTelecom()
					.setValue(
							pid.getPid14_PhoneNumberBusiness()[0]
									.getXtn1_9999999X99999CAnyText().getValue())
					.setSystem(ContactPointSystemEnum.PHONE)
					.setUse(ContactPointUseEnum.WORK);
			;
		}

		// PID-8
		if (pid.getPid8_Sex().getValue() != null) {
			patient.setGender(equivalentGender(pid.getPid8_Sex().getValue()));
		}

		// PID-7
		patient.setBirthDate(new DateDt(pid.getPid7_DateOfBirth()
				.getTimeOfAnEvent().getValueAsDate()));

		// deceased[x] PID-30 (bool) and PID-29 (datetime)
		patient.setDeceased(new BooleanDt(Boolean.valueOf(pid
				.getPid30_PatientDeathIndicator().getValue())));

		// PID-11
		AddressDt addressDt = patient.addAddress();
		XAD[] patientAddress = pid.getPid11_PatientAddress();
		if (patientAddress.length > 0) {
			addressDt.addLine(patientAddress[0].getXad1_StreetAddress()
					.getValue());
			addressDt.addLine(patientAddress[0].getXad5_ZipOrPostalCode()
					.getValue());
			addressDt.setCity(patientAddress[0].getXad3_City().getValue());
			addressDt
					.setCountry(patientAddress[0].getXad6_Country().getValue());
			addressDt.setState(patientAddress[0].getXad4_StateOrProvince()
					.getValue());
		}

		// PID-16
		if (pid.getPid16_MaritalStatus().length > 0
				&& pid.getPid16_MaritalStatus()[0].getValue() != null) {
			patient.getMaritalStatus().addCoding()
					.setCode(pid.getPid16_MaritalStatus()[0].getValue())
					.setSystem(SYSTEM_V2_MARITAL_STATUS);
		}

		if (pid.getPid24_MultipleBirthIndicator().getValue() != null) {
			// PID-24 (bool), PID-25 (integer)
			patient.setMultipleBirth(new BooleanDt(Boolean.valueOf(pid
					.getPid24_MultipleBirthIndicator().getValue())));
		}

		for (NK1 nk1 : listNk1) {
			transformContact(patient.addContact(), nk1);
		}

		return patient;
	}

	/**
	 * Transform Nk1 to Contact
	 * 
	 * @param patient
	 * @param nk1
	 */
	private void transformContact(Contact contact, NK1 nk1) {

		// NK1-7, NK1-3
		if (nk1.getNk13_Relationship().getCe1_Identifier().getValue() != null) {
			contact.addRelationship()
					.addCoding()
					.setCode(
							nk1.getNk13_Relationship().getCe1_Identifier()
									.getValue())
					.setSystem(SYSTEM_V2_RELATIONSHIP);
		}
		// NK1-2
		if (nk1.getNk12_NKName().length > 0) {
			HumanNameDt contactName = new HumanNameDt();
			contactName.addFamily().setValue(
					nk1.getNk12_NKName()[0].getFamilyName().getValue());
			contactName.addGiven().setValue(
					nk1.getNk12_NKName()[0].getGivenName().getValue());
			contact.setName(contactName);
		}

		// NK1-5, NK1-6, NK1-40 (not v23)
		if (nk1.getNk15_PhoneNumber().length > 0) {
			contact.addTelecom()
					.setValue(
							nk1.getNk15_PhoneNumber()[0]
									.getXtn1_9999999X99999CAnyText().getValue())
					.setSystem(ContactPointSystemEnum.PHONE)
					.setUse(ContactPointUseEnum.HOME);
		}
		if (nk1.getNk16_BusinessPhoneNumber().length > 0) {
			contact.addTelecom()
					.setValue(
							nk1.getNk16_BusinessPhoneNumber()[0]
									.getXtn1_9999999X99999CAnyText().getValue())
					.setSystem(ContactPointSystemEnum.PHONE)
					.setUse(ContactPointUseEnum.WORK);
		}

		// NK1-4
		AddressDt addressContactDt = contact.getAddress();
		XAD[] contactAddress = nk1.getNk14_Address();
		if (contactAddress.length > 0) {
			addressContactDt.addLine(contactAddress[0].getXad1_StreetAddress()
					.getValue());
			addressContactDt.addLine(contactAddress[0]
					.getXad5_ZipOrPostalCode().getValue());
			addressContactDt.setCity(contactAddress[0].getXad3_City()
					.getValue());
			addressContactDt.setCountry(contactAddress[0].getXad6_Country()
					.getValue());
			addressContactDt.setState(contactAddress[0]
					.getXad4_StateOrProvince().getValue());
		}

		// NK1-15
		if (nk1.getNk115_Sex().getValue() != null) {
			contact.setGender(equivalentGender(nk1.getNk115_Sex().getValue()));
		}

		// NK1-13, NK1-30, NK1-31, NK1-32, NK1-41
		ResourceReferenceDt organization = contact.getOrganization();
		if (nk1.getNk113_OrganizationName().length > 0) {
			organization.setDisplay(nk1.getNk113_OrganizationName()[0]
					.getOrganizationName().getValue());
		}
	}
}
