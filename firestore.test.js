const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1001";
const BOB_UID = "bob_uid_1002";
const EVE_UID = "eve_uid_9999";

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("short_id_lookup").doc("1001").get());
  await assertFails(unauthDb.collection("chats").get());
  await assertFails(unauthDb.collection("calls").get());
});

test("User profile & contacts: owner succeeds, cross-user PII denied, ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      shortId: "1001",
      displayName: "Rahim",
      email: "rahim@example.com",
      notificationsEnabled: true,
      callRingtoneEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Rahim Ahmed",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read & write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field & future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Contacts subcollection: owner list & create succeed, cross-user fails
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").doc(BOB_UID).set({
      contactUid: BOB_UID,
      shortId: "1002",
      displayName: "Karim",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("contacts").get()
  );
});

test("Short ID lookup: direct get allowed for signed-in, list scraping blocked, owner-only writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("short_id_lookup").doc("1001").set({
      userId: ALICE_UID,
      shortId: "1001",
      displayName: "Rahim",
      online: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Direct get succeeds for Bob
  await assertSucceeds(bobDb.collection("short_id_lookup").doc("1001").get());
  // Listing all IDs is denied
  await assertFails(bobDb.collection("short_id_lookup").get());
  // Bob cannot hijack Alice's short ID
  await assertFails(
    bobDb.collection("short_id_lookup").doc("1001").update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Chats & messages: member queries and writes succeed, non-member & unfiltered queries fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_1001_1002").set({
      groupChat: true,
      title: "পরিবার গ্রুপ",
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: { [ALICE_UID]: "Rahim", [BOB_UID]: "Karim" },
      memberShortIds: { [ALICE_UID]: "1001", [BOB_UID]: "1002" },
      lastMessageText: "গ্রুপ তৈরি হয়েছে",
      lastMessageSenderId: ALICE_UID,
      lastMessageSenderName: "Rahim",
      lastMessageType: "system",
      lastMessageAt: now,
      typingUids: [],
      activeCallVideo: false,
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-existent direct chat existence check succeeds for participant, fails for non-participant
  await assertSucceeds(
    aliceDb.collection("chats").doc(`direct_${ALICE_UID}_${BOB_UID}`).get()
  );
  await assertFails(
    eveDb.collection("chats").doc(`direct_${ALICE_UID}_${BOB_UID}`).get()
  );

  // Filtered member query succeeds
  await assertSucceeds(
    bobDb.collection("chats").where("memberUids", "array-contains", BOB_UID).get()
  );
  // Unfiltered list query denied
  await assertFails(aliceDb.collection("chats").get());
  // Non-member denied
  await assertFails(eveDb.collection("chats").doc("chat_1001_1002").get());

  // Message create & seen update
  const msgRef = aliceDb
    .collection("chats")
    .doc("chat_1001_1002")
    .collection("messages")
    .doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      chatId: "chat_1001_1002",
      senderId: ALICE_UID,
      senderName: "Rahim",
      memberUids: [ALICE_UID, BOB_UID],
      text: "হ্যালো!",
      messageType: "text",
      deliveredTo: [ALICE_UID],
      seenBy: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb
      .collection("chats")
      .doc("chat_1001_1002")
      .collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertSucceeds(
    bobDb
      .collection("chats")
      .doc("chat_1001_1002")
      .collection("messages")
      .doc("msg_1")
      .update({
        deliveredTo: [ALICE_UID, BOB_UID],
        seenBy: [ALICE_UID, BOB_UID],
        updatedAt: new Date(Date.now() - 500),
      })
  );
  await assertFails(
    eveDb
      .collection("chats")
      .doc("chat_1001_1002")
      .collection("messages")
      .doc("msg_1")
      .get()
  );
});

test("Calls & ICE candidates: ringing -> active -> ended lifecycle and terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      callerId: ALICE_UID,
      callerName: "Rahim",
      callerShortId: "1001",
      participantUids: [ALICE_UID, BOB_UID],
      joinedUids: [ALICE_UID],
      participantNames: { [ALICE_UID]: "Rahim", [BOB_UID]: "Karim" },
      participantShortIds: { [ALICE_UID]: "1001", [BOB_UID]: "1002" },
      videoCall: true,
      groupCall: false,
      status: "ringing",
      durationSeconds: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob answers call
  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").update({
      status: "active",
      joinedUids: [ALICE_UID, BOB_UID],
      answerSdp: "v=0...",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // ICE candidate exchange
  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").collection("candidates").doc("cand_1").set({
      senderId: BOB_UID,
      participantUids: [ALICE_UID, BOB_UID],
      sdpMid: "0",
      sdpMLineIndex: 0,
      candidate: "candidate:1 1 UDP 2122252543 192.168.1.2 54321 typ host",
      createdAt: new Date(Date.now() - 800),
    })
  );

  // End call
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").update({
      status: "ended",
      durationSeconds: 42,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Terminal state locking: cannot mutate after ended
  await assertFails(
    bobDb.collection("calls").doc("call_1").update({
      status: "active",
      joinedUids: [BOB_UID],
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Non-participant denied
  await assertFails(eveDb.collection("calls").doc("call_1").get());
  await assertSucceeds(
    bobDb.collection("calls").where("participantUids", "array-contains", BOB_UID).get()
  );
});
