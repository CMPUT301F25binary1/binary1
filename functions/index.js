// functions/index.js

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

/**
 * sendChosenNotifications
 *
 * - If only eventId is provided: notify ALL selected/pending entrants for that event
 *   (used by the Chosen Entrants screen / “Notify Selected Entrants”).
 * - If eventId + entrantId is provided: notify ONLY that entrant
 *   (used by the Sampling & Replacement “Notify Entrant” button).
 *
 * For CMPUT 301 testing:
 * - If user has no FCM token, we still count it as "sent" and mark status=notified.
 *   This lets you test everything without real devices.
 */
exports.sendChosenNotifications = onCall(async (request) => {
  const data = request.data || {};
  const eventId = data.eventId;
  const singleEntrantId = data.entrantId || null;

  if (!eventId || typeof eventId !== "string") {
    throw new HttpsError("invalid-argument", "eventId is required");
  }

  const db = getFirestore();
  const eventRef = db.collection("events").doc(eventId);

  const eventSnap = await eventRef.get();
  if (!eventSnap.exists) {
    throw new HttpsError("not-found", "Event not found");
  }
  const event = eventSnap.data() || {};

  const eventName = event.name || "Your event";
  let eventDateText = "";
  if (event.eventDate && typeof event.eventDate.toDate === "function") {
    const d = event.eventDate.toDate();
    eventDateText = d.toLocaleDateString("en-CA");
  }

  const title = `Update: ${eventName}`;
  const body = eventDateText
    ? `You are selected for ${eventName} on ${eventDateText}. Please open the app and confirm your participation.`
    : `You are selected for ${eventName}. Please open the app and confirm your participation.`;

  // ---------- load selected docs ----------
  let selectedDocs = [];

  if (singleEntrantId) {
    const docSnap = await eventRef.collection("selected").doc(singleEntrantId).get();
    if (docSnap.exists) {
      selectedDocs = [docSnap];
    }
  } else {
    const selectedSnap = await eventRef.collection("selected").get();
    selectedDocs = selectedSnap.docs;
  }

  if (!selectedDocs.length) {
    return { sentCount: 0, failureCount: 0 };
  }

  const messaging = getMessaging();
  const batch = db.batch();

  let sentCount = 0;
  let failureCount = 0;

  const tokens = [];
  const docsToMarkNotified = [];

  for (const doc of selectedDocs) {
    const selData = doc.data() || {};
    const status = selData.status || "pending";
    const userId = selData.userId;

    // Only notify if still pending/selected
    if (status !== "pending" && status !== "selected") continue;

    // We will mark this doc as notified if we attempt to notify
    docsToMarkNotified.push(doc.ref);

    // If no userId at all, just treat as "sent" for testing
    if (!userId) {
      sentCount++;
      continue;
    }

    const userSnap = await db.collection("users").doc(userId).get();
    const user = userSnap.exists ? (userSnap.data() || {}) : {};

    const prefs = user.notificationPreferences || {};
    // If both toggles are false, respect opt-out: mark notified but don't count failure
    if (prefs.lotteryResults === false && prefs.organizerUpdates === false) {
      continue;
    }

    const token = user.fcmToken;

    if (!token) {
      // **Important for testing**: count as success even with no FCM token
      sentCount++;
      continue;
    }

    tokens.push(token);
  }

  // If we actually have tokens, send real FCM notifications
  if (tokens.length > 0) {
    const message = {
      notification: { title, body },
      tokens,
      data: {
        eventId,
        eventName,
        eventDate: eventDateText,
        confirmationInstructions:
          "Please open the app and confirm your participation.",
      },
    };

    const resp = await messaging.sendEachForMulticast(message);
    sentCount += resp.successCount;
    failureCount += resp.failureCount;
  }

  // Mark all attempted docs as notified
  docsToMarkNotified.forEach((ref) => {
    batch.update(ref, {
      status: "notified",
      notifiedAt: FieldValue.serverTimestamp(),
      eventName,
      eventDate: eventDateText,
      confirmationInstructions:
        "Please open the app and confirm your participation.",
    });
  });

  await batch.commit();

  return { sentCount, failureCount };
});
