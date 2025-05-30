const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

exports.notifyOnNewMessage = functions.database
    .ref("/chats/{sender}_{receiver}/{messageId}")
    .onCreate(async (snap, context) => {
      const {sender, receiver} = context.params;
      const msgData = snap.val();
      const msgText = msgData.message || "Tienes un mensaje";

      const tokenSnap = await admin.database()
          .ref(`/tokens/${receiver}`)
          .once("value");
      const token = tokenSnap.val();
      if (!token) return null;

      const userSnap = await admin.database()
          .ref(`/users/${sender}/username`)
          .once("value");
      const username = userSnap.val() || "Alguien";

      const payload = {
        notification: {
          title: username,
          body: msgText.length > 40 ?
          msgText.substring(0, 37) + "…" :
          msgText,
          click_action: "OPEN_CHAT",
        },
        data: {
          senderUid: sender,
        },
      };

      return admin.messaging().sendToDevice(token, payload);
    });
