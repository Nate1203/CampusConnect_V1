"use strict";

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.onQueryStatusUpdate = functions.firestore
    .document("queries/{queryId}")
    .onUpdate(async (change, context) => {
        const newData = change.after.data();
        const previousData = change.before.data();

        if (newData.status === previousData.status) {
            return;
        }

        const studentId = newData.studentId;
        console.log(`Processing status update for student: ${studentId}`);

        try {
            const studentDoc = await admin.firestore()
                .collection("students")
                .doc(studentId)
                .get();

            if (!studentDoc.exists) {
                console.log("Student document not found");
                return;
            }

            const fcmToken = studentDoc.data().fcmToken;
            if (!fcmToken) {
                console.log("No FCM token found for student");
                return;
            }

            let notificationTitle;
            let notificationBody;

            if (newData.status === "Processing") {
                notificationTitle = "Query Update";
                notificationBody = `Your ${newData.queryType} is now being processed.`;
            } else if (newData.status === "Solved") {
                notificationTitle = "Query Resolved";
                notificationBody = `Your ${newData.queryType} has been resolved.`;
            }

            if (notificationTitle && notificationBody) {
                const message = {
                    notification: {
                        title: notificationTitle,
                        body: notificationBody,
                    },
                    data: {
                        queryId: context.params.queryId,
                        queryType: newData.queryType,
                        status: newData.status,
                    },
                    token: fcmToken,
                };

                await admin.messaging().send(message);
                console.log("Notification sent successfully");
            }
        } catch (error) {
            console.error("Error:", error);
        }
    });