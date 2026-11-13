const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const HOST_UID = "host_alice";
const GUEST_UID = "guest_bob";
const STRANGER_UID = "stranger_charlie";

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

function validRoomPayload(overrides = {}) {
  return {
    roomId: "ROOM42",
    hostUid: HOST_UID,
    hostName: "Alice",
    hasPasscode: false,
    status: "waiting",
    turn: "@",
    phase: "drop",
    board: ["", "", "", "", "", "", "", "", ""],
    atPiecesPlaced: 0,
    hashPiecesPlaced: 0,
    winner: "",
    winPattern: [],
    repeatMoveCount: 0,
    lastMoveFrom: -1,
    lastMoveTo: -1,
    lastMovePiece: "",
    rematchRequestedBy: [],
    memberUids: [HOST_UID],
    createdAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

test("Unauthenticated user: cannot read rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("rooms").doc("ROOM42").get());
});

test("Authenticated host: can create valid room", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  await assertSucceeds(
    hostDb.collection("rooms").doc("ROOM42").set(validRoomPayload())
  );
});

test("Stranger: cannot create room with spoofed hostUid", async () => {
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(
    strangerDb.collection("rooms").doc("ROOM42").set(validRoomPayload({ hostUid: HOST_UID }))
  );
});

test("Guest: can join waiting room", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("ROOM42").set(validRoomPayload());
  });

  const guestDb = testEnv.authenticatedContext(GUEST_UID).firestore();
  await assertSucceeds(
    guestDb.collection("rooms").doc("ROOM42").update({
      guestUid: GUEST_UID,
      guestName: "Bob",
      status: "playing",
      memberUids: [HOST_UID, GUEST_UID],
      updatedAt: new Date(),
    })
  );
});

test("Non-turn player: cannot make move out of turn", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("ROOM42").set(validRoomPayload({
      status: "playing",
      guestUid: GUEST_UID,
      guestName: "Bob",
      memberUids: [HOST_UID, GUEST_UID],
      turn: "@", // Host's turn
    }));
  });

  const guestDb = testEnv.authenticatedContext(GUEST_UID).firestore();
  // Bob (#) tries to move on @'s turn
  await assertFails(
    guestDb.collection("rooms").doc("ROOM42").update({
      board: ["#", "", "", "", "", "", "", "", ""],
      turn: "@",
      hashPiecesPlaced: 1,
      updatedAt: new Date(),
    })
  );
});

test("Current turn player: can place piece on their turn", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("ROOM42").set(validRoomPayload({
      status: "playing",
      guestUid: GUEST_UID,
      guestName: "Bob",
      memberUids: [HOST_UID, GUEST_UID],
      turn: "@",
    }));
  });

  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  await assertSucceeds(
    hostDb.collection("rooms").doc("ROOM42").update({
      board: ["@", "", "", "", "", "", "", "", ""],
      turn: "#",
      phase: "drop",
      atPiecesPlaced: 1,
      hashPiecesPlaced: 0,
      winner: "",
      winPattern: [],
      repeatMoveCount: 0,
      lastMoveFrom: -1,
      lastMoveTo: 0,
      lastMovePiece: "@",
      status: "playing",
      updatedAt: new Date(),
    })
  );
});
