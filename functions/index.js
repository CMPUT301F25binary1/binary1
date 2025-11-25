// functions/index.js

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

initializeApp();

exports.sendChosenNotifications = onCall(async (request) => {
  const data = request.data || {};
  const eventId = data.eventId;

  if (!eventId || typeof eventId !== "string") {
    throw new HttpsError("invalid-argument", "eventId is required");
  }

  const db = getFirestore();
  const eventRef = db.collection("events").doc(eventId);

  // 🔹 Fetch event info (name + date)
  const eventSnap = await eventRef.get();
  if (!eventSnap.exists) {
    throw new HttpsError("not-found", "Event does not exist");
  }

  const eventData = eventSnap.data();
  const eventName = eventData.name || "Event";
  const eventDate = eventData.eventDate?.toDate?.() || null;

  // Format date (simple readable format)
  const formattedDate = eventDate
    ? eventDate.toLocaleDateString("en-US", {
        year: "numeric",
        month: "short",
        day: "numeric",
      })
    : "Date not available";

  // Confirmation instructions for selected entrants
  const confirmationInstructions =
    "Please open the app and confirm your participation.";

  // 🔹 Fetch selected entrants
  const selectedSnap = await eventRef.collection("selected").get();
  if (selectedSnap.empty) {
    return { sentCount: 0, failureCount: 0 };
  }

  let sentCount = 0;
  let failureCount = 0;

  const batch = db.batch();

  selectedSnap.forEach((doc) => {
    const d = doc.data() || {};
    const status = d.status || "pending";

    // Only notify once
    if (status !== "pending") return;

    // 🔹 This is where actual FCM send would happen.
    // For now we simulate success:
    sentCount++;

    batch.update(doc.ref, {
      status: "notified",
      notifiedAt: FieldValue.serverTimestamp(),
      eventName,
      eventDate: formattedDate,
      confirmationInstructions,
    });
  });

  await batch.commit();

  // Return values so Android can show them
  return {
    sentCount,
    failureCount,
    eventName,
    formattedDate,
    confirmationInstructions,
  };
});
