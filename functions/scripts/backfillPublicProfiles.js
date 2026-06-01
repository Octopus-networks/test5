"use strict";

const admin = require("firebase-admin");
const { buildPublicProfileFromPrivateProfile } = require("../publicProfileMirror");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

async function isEmailVerified(userId) {
  try {
    const userRecord = await admin.auth().getUser(userId);
    return userRecord.emailVerified === true;
  } catch (error) {
    console.warn("Could not read Auth user while backfilling public profile", {
      userId,
      error: error && error.message ? error.message : error,
    });
    return false;
  }
}

async function main() {
  const snapshot = await db.collection("profiles").get();
  let processed = 0;
  const batchSize = 400;
  let batch = db.batch();
  let writesInBatch = 0;

  for (const doc of snapshot.docs) {
    const userId = doc.id;
    const publicProfile = buildPublicProfileFromPrivateProfile(userId, doc.data(), {
      isEmailVerified: await isEmailVerified(userId),
      serverTimestamp: admin.firestore.FieldValue.serverTimestamp(),
    });

    batch.set(db.collection("publicProfiles").doc(userId), publicProfile, { merge: false });
    processed += 1;
    writesInBatch += 1;

    if (writesInBatch >= batchSize) {
      await batch.commit();
      batch = db.batch();
      writesInBatch = 0;
    }
  }

  if (writesInBatch > 0) {
    await batch.commit();
  }

  console.log(`Backfilled ${processed} publicProfiles documents from profiles.`);
}

main().catch((error) => {
  console.error("Backfill failed", error);
  process.exitCode = 1;
});
