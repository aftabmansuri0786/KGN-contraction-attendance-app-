const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read or write worker data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").set({ name: "Ramesh" }));
});

test("Authenticated owner: can read and write own workers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").set({
    id: "w1",
    name: "Ramesh Kumar",
    mobile: "+91 9876543210",
    category: "Mason",
    dailyWage: 800
  }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").get());
});

test("Cross-user access: Bob cannot access Alice's records", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("workers").doc("w1").set({
      id: "w1",
      name: "Alice Worker"
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workers").doc("w1").delete());
});
