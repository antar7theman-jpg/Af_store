const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_antar_uid";
const MANAGER_UID = "manager_1_uid";
const WORKER_ALICE_UID = "alice_worker_123";
const WORKER_BOB_UID = "bob_worker_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function buildValidStaffPayload(overrides = {}) {
  const now = new Date();
  return {
    userId: WORKER_ALICE_UID,
    storeId: "af_store",
    email: "alice@example.com",
    fullName: "أليس موظفة الكاشير",
    phone: "0550123456",
    requestedRole: "worker",
    role: "worker",
    approvalStatus: "PENDING",
    approvedByUid: "",
    approvedByName: "",
    canMakeSales: true,
    canManageInventory: true,
    canEditPrices: false,
    canViewReports: false,
    canManageDebtsAndCustomers: true,
    canRefundOrVoid: false,
    canApplyDiscounts: true,
    canViewCostPrices: false,
    canExportData: false,
    canManagePurchaseInvoices: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("1. Unauthenticated user cannot read or create staff_accounts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("staff_accounts").doc(WORKER_ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("staff_accounts")
      .doc(WORKER_ALICE_UID)
      .set(buildValidStaffPayload())
  );
});

test("2. Worker can self-register their own account as PENDING worker only", async () => {
  const aliceDb = testEnv
    .authenticatedContext(WORKER_ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(
    aliceDb
      .collection("staff_accounts")
      .doc(WORKER_ALICE_UID)
      .set(buildValidStaffPayload({ userId: WORKER_ALICE_UID }))
  );

  await assertSucceeds(
    aliceDb.collection("staff_accounts").doc(WORKER_ALICE_UID).get()
  );
});

test("3. Worker cannot self-register with requestedRole=manager or role=manager", async () => {
  const aliceDb = testEnv
    .authenticatedContext(WORKER_ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();

  await assertFails(
    aliceDb
      .collection("staff_accounts")
      .doc(WORKER_ALICE_UID)
      .set(
        buildValidStaffPayload({
          userId: WORKER_ALICE_UID,
          requestedRole: "manager",
        })
      )
  );

  await assertFails(
    aliceDb
      .collection("staff_accounts")
      .doc(WORKER_ALICE_UID)
      .set(
        buildValidStaffPayload({
          userId: WORKER_ALICE_UID,
          role: "manager",
        })
      )
  );
});

test("4. Worker cannot read another worker's account (PII Isolation)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("staff_accounts")
      .doc(WORKER_BOB_UID)
      .set(
        buildValidStaffPayload({
          userId: WORKER_BOB_UID,
          email: "bob@example.com",
          fullName: "بوب الكاشير",
        })
      );
  });

  const aliceDb = testEnv
    .authenticatedContext(WORKER_ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();

  await assertFails(
    aliceDb.collection("staff_accounts").doc(WORKER_BOB_UID).get()
  );
});

test("5. Owner Deffas antar (Antar7theman@gmail.com) can create and approve Manager accounts", async () => {
  const ownerDb = testEnv
    .authenticatedContext(OWNER_UID, {
      email: "antar7theman@gmail.com",
      email_verified: true,
    })
    .firestore();

  // Owner creates a pre-approved Manager account manually from Owner Dashboard
  await assertSucceeds(
    ownerDb
      .collection("staff_accounts")
      .doc(MANAGER_UID)
      .set(
        buildValidStaffPayload({
          userId: MANAGER_UID,
          email: "manager1@afstore.com",
          fullName: "مدير الفرع الأول",
          requestedRole: "manager",
          role: "manager",
          approvalStatus: "APPROVED",
          approvedByUid: OWNER_UID,
          approvedByName: "Deffas antar",
          canEditPrices: true,
          canViewReports: true,
          canRefundOrVoid: true,
          canViewCostPrices: true,
          canExportData: true,
          canManagePurchaseInvoices: true,
        })
      )
  );

  // Owner also registers the Manager in /admins
  await assertSucceeds(
    ownerDb.collection("admins").doc(MANAGER_UID).set({
      userId: MANAGER_UID,
      storeId: "af_store",
      email: "manager1@afstore.com",
      fullName: "مدير الفرع الأول",
      createdAt: new Date(),
    })
  );
});

test("6. Regular Manager can approve Worker accounts but CANNOT create or approve Manager accounts", async () => {
  const baseTime = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(MANAGER_UID).set({
      userId: MANAGER_UID,
      storeId: "af_store",
      email: "manager1@afstore.com",
      fullName: "مدير الفرع",
      createdAt: baseTime,
    });
    await context
      .firestore()
      .collection("staff_accounts")
      .doc(WORKER_ALICE_UID)
      .set(
        buildValidStaffPayload({
          userId: WORKER_ALICE_UID,
          createdAt: baseTime,
          updatedAt: baseTime,
        })
      );
  });

  const managerDb = testEnv
    .authenticatedContext(MANAGER_UID, {
      email: "manager1@afstore.com",
      email_verified: true,
    })
    .firestore();

  // Regular manager CAN approve Alice as a worker
  await assertSucceeds(
    managerDb.collection("staff_accounts").doc(WORKER_ALICE_UID).update({
      approvalStatus: "APPROVED",
      role: "worker",
      approvedByUid: MANAGER_UID,
      approvedByName: "مدير الفرع",
      updatedAt: new Date(),
    })
  );

  // Regular manager CAN create a new Worker account affiliated with their own manager UID
  await assertSucceeds(
    managerDb
      .collection("staff_accounts")
      .doc("wrk_affiliated_1")
      .set(
        buildValidStaffPayload({
          userId: "wrk_affiliated_1",
          email: "worker_m1@afstore.com",
          fullName: "عامل تابع للمدير الأول",
          requestedRole: "worker",
          role: "worker",
          approvalStatus: "APPROVED",
          approvedByUid: MANAGER_UID,
          approvedByName: "مدير الفرع",
          parentManagerUid: MANAGER_UID,
          parentManagerName: "مدير الفرع",
          parentManagerEmail: "manager1@afstore.com",
          branchName: "فرع المدير الأول",
          secretPin: "4455",
        })
      )
  );

  // Regular manager CANNOT create a worker assigned to a DIFFERENT manager
  await assertFails(
    managerDb
      .collection("staff_accounts")
      .doc("wrk_spoof_other_mgr")
      .set(
        buildValidStaffPayload({
          userId: "wrk_spoof_other_mgr",
          email: "spoof@afstore.com",
          requestedRole: "worker",
          role: "worker",
          approvalStatus: "APPROVED",
          parentManagerUid: "other_manager_uid_999",
        })
      )
  );

  // Regular manager CANNOT promote Alice to manager
  await assertFails(
    managerDb.collection("staff_accounts").doc(WORKER_ALICE_UID).update({
      role: "manager",
      updatedAt: new Date(),
    })
  );

  // Regular manager CANNOT create a new Manager account
  await assertFails(
    managerDb
      .collection("staff_accounts")
      .doc("new_mgr_999")
      .set(
        buildValidStaffPayload({
          userId: "new_mgr_999",
          email: "mgr999@afstore.com",
          requestedRole: "manager",
          role: "manager",
          approvalStatus: "APPROVED",
        })
      )
  );
});

test("7. Email spoofing without email_verified == true is rejected for Owner", async () => {
  const unverifiedOwnerDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "antar7theman@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(
    unverifiedOwnerDb
      .collection("staff_accounts")
      .where("storeId", "==", "af_store")
      .get()
  );
});

test("8. Shadow field injection is rejected", async () => {
  const aliceDb = testEnv
    .authenticatedContext(WORKER_ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();

  await assertFails(
    aliceDb
      .collection("staff_accounts")
      .doc(WORKER_ALICE_UID)
      .set({
        ...buildValidStaffPayload({ userId: WORKER_ALICE_UID }),
        isSuperAdminHack: true,
      })
  );
});
