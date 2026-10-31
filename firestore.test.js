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
const CHARLIE_UID = "charlie_789";

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

test("Unauthenticated user: cannot read users or events", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("events").get());
});

test("User Profile: Alice can create her profile, Bob can read, Bob cannot update Alice's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Smith",
      username: "alice_smith",
      createdAt: now,
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).get());

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      fullName: "Hacked by Bob",
      updatedAt: now,
    })
  );
});

test("Private Info: Alice can write and read her private info, Bob is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("info").set({
      email: "alice@example.com",
      phoneNumber: "+1234567890",
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get()
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get()
  );
});

test("Events & Invitations: Alice creates event, invites Bob, Bob accepts RSVP, Charlie cannot read Bob's invite", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const eventId = "event_party_1";

  // Alice creates event
  await assertSucceeds(
    aliceDb.collection("events").doc(eventId).set({
      eventId: eventId,
      organizerId: ALICE_UID,
      organizerName: "Alice Smith",
      title: "Alice's Birthday Bash",
      description: "Join us for music, food and fun!",
      category: "Birthday",
      eventDate: "2026-11-20",
      startTime: "19:00",
      location: "Skyline Lounge, New York",
      invitationMessage: "Can't wait to celebrate together!",
      visibility: "public",
      eventStatus: "active",
      invitedCount: 1,
      acceptedCount: 0,
      maybeCount: 0,
      declinedCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice invites Bob
  await assertSucceeds(
    aliceDb.collection("events").doc(eventId).collection("invitations").doc(BOB_UID).set({
      recipientId: BOB_UID,
      recipientName: "Bob Jones",
      recipientUsername: "bobjones",
      senderId: ALICE_UID,
      senderName: "Alice Smith",
      eventId: eventId,
      eventTitle: "Alice's Birthday Bash",
      eventDate: "2026-11-20",
      eventLocation: "Skyline Lounge, New York",
      invitationStatus: "sent",
      rsvpStatus: "PENDING",
      sentAt: now,
      updatedAt: now,
    })
  );

  // Bob reads invitation
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("events").doc(eventId).collection("invitations").doc(BOB_UID).get()
  );

  // Charlie tries to read Bob's invitation - MUST FAIL
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("events").doc(eventId).collection("invitations").doc(BOB_UID).get()
  );

  // Bob queries his received invitations via collectionGroup
  await assertSucceeds(
    bobDb.collectionGroup("invitations").where("recipientId", "==", BOB_UID).get()
  );

  // Bob updates RSVP to ACCEPTED with note
  await assertSucceeds(
    bobDb.collection("events").doc(eventId).collection("invitations").doc(BOB_UID).update({
      rsvpStatus: "ACCEPTED",
      responseMessage: "Count me in! Looking forward to it.",
      respondedAt: now,
      updatedAt: now,
    })
  );
});
