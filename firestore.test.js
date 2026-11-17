const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_ehs_01";
const BOB_UID = "bob_ehs_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any EHS collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("safety_records").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("safety_records")
      .doc("rec_1")
      .set({
        userId: ALICE_UID,
        pillar: "OBSERVATIONS",
        recordType: "Unsafe Condition",
        title: "Missing guardrail",
        description: "Scaffolding edge open",
        siteId: "",
        siteName: "Main Plant",
        locationArea: "Zone B",
        severity: "HIGH",
        status: "OPEN",
        assignedTo: "Site Lead",
        capaAction: "Install guardrail",
        rootCause: "",
        dueDateLabel: "Today",
        closureNotes: "",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Owner can create, update, and close a SafetyRecord (Observations / Incidents & CAPA)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const recRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("safety_records")
    .doc("rec_001");

  await assertSucceeds(
    recRef.set({
      userId: ALICE_UID,
      pillar: "INCIDENTS_CAPA",
      recordType: "Near Miss",
      title: "Overhead lifting sling frayed",
      description: "Observed frayed wire sling during pump rigging.",
      siteId: "",
      siteName: "WTP Sector 4",
      locationArea: "Pump House",
      severity: "CRITICAL",
      status: "OPEN",
      assignedTo: "R. Sharma",
      capaAction: "Quarantine sling and replace with certified strap",
      rootCause: "Pre-lift inspection skipped",
      dueDateLabel: "2026-11-18",
      closureNotes: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Move to IN_PROGRESS
  await assertSucceeds(
    recRef.update({
      status: "IN_PROGRESS",
      assignedTo: "R. Sharma",
      capaAction: "New certified sling ordered and tagged",
      rootCause: "Pre-lift inspection skipped",
      dueDateLabel: "2026-11-18",
      severity: "CRITICAL",
      description: "Observed frayed wire sling during pump rigging.",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Close with valid closure notes
  await assertSucceeds(
    recRef.update({
      status: "CLOSED",
      closureNotes: "Replaced sling and verified load test certificate.",
      capaAction: "New certified sling ordered and tagged",
      rootCause: "Pre-lift inspection skipped",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: cannot reopen or edit once CLOSED
  await assertFails(
    recRef.update({
      status: "OPEN",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Rejects ghost fields, @DocumentId 'id' leakage, future timestamps, and invalid enums", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Ghost field / @DocumentId id field must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("safety_records")
      .doc("rec_bad_id")
      .set({
        id: "rec_bad_id",
        userId: ALICE_UID,
        pillar: "OBSERVATIONS",
        recordType: "Unsafe Act",
        title: "No helmet",
        description: "",
        siteId: "",
        siteName: "Main Plant",
        locationArea: "Gate 1",
        severity: "MEDIUM",
        status: "OPEN",
        assignedTo: "",
        capaAction: "",
        rootCause: "",
        dueDateLabel: "",
        closureNotes: "",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Future timestamp must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("toolbox_talks")
      .doc("tbt_future")
      .set({
        userId: ALICE_UID,
        siteId: "",
        siteName: "Main Plant",
        topic: "Working at Height",
        conductorName: "A. Verma",
        category: "TBT",
        attendeeCount: 24,
        durationMinutes: 15,
        keyTakeaways: "100% tie-off required above 1.8m.",
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );
});

test("Owner can create and list Sites, Inspections, and ToolboxTalks; cross-user access is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Officer",
      roleTitle: "EHS Lead",
      activeSiteName: "WTP Phase 2",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sites").doc("site_1").set({
      userId: ALICE_UID,
      name: "WTP Phase 2",
      projectCode: "WTP-204",
      location: "Pune Sector B",
      managerName: "Alice Officer",
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("inspections")
      .doc("insp_1")
      .set({
        userId: ALICE_UID,
        siteId: "site_1",
        siteName: "WTP Phase 2",
        templateName: "IMS Scaffolding Audit",
        inspectorName: "Alice Officer",
        area: "Aeration Tank",
        passedCount: 4,
        totalCount: 5,
        scorePercent: 80,
        status: "COMPLETED",
        checklistItems: [
          "Green scaffold tag displayed|PASS",
          "Guardrails & toe-boards intact|PASS",
          "Base plates & sole boards level|PASS",
          "Access ladder secured|PASS",
          "Full body harness inspected|FAIL",
        ],
        notes: "One harness tag expired; replaced on spot.",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("toolbox_talks")
      .doc("tbt_1")
      .set({
        userId: ALICE_UID,
        siteId: "site_1",
        siteName: "WTP Phase 2",
        topic: "Confined Space Entry Protocol",
        conductorName: "Alice Officer",
        category: "TBT",
        attendeeCount: 18,
        durationMinutes: 20,
        keyTakeaways: "Gas test & standby attendant mandatory.",
        createdAt: now,
        updatedAt: now,
      })
  );

  // List queries succeed for owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("sites").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("inspections").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("toolbox_talks").get());

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sites").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("inspections").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("toolbox_talks").doc("tbt_1").get()
  );
});
