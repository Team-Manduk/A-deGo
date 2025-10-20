const functions = require("firebase-functions");
const admin = require("firebase-admin");

/**
 * 참가자가 제거될 때 자동으로 트리거되는 함수
 * 마지막 참가자가 나가면 방 전체를 삭제합니다.
 *
 * @trigger onDelete: /participants/{roomId}/{userId}
 */
module.exports = functions.database
    .ref("/participants/{roomId}/{userId}")
    .onDelete(async (snapshot, context) => {
      const roomId = context.params.roomId;
      const userId = context.params.userId;

      console.log(`Participant removed: ${userId} from room ${roomId}`);

      try {
        // 해당 방의 남은 참가자 확인
        const participantsSnapshot = await admin
            .database()
            .ref(`/participants/${roomId}`)
            .once("value");

        // 참가자가 아무도 없으면 방 삭제
        if (!participantsSnapshot.exists() ||
            !participantsSnapshot.hasChildren()) {
          console.log(`Last participant left. Deleting room: ${roomId}`);

          await admin
              .database()
              .ref(`/rooms/${roomId}`)
              .remove();

          console.log(`Room deleted successfully: ${roomId}`);
        } else {
          const remainingCount = participantsSnapshot.numChildren();
          console.log(
              `${remainingCount} participant(s) remaining in room ${roomId}`,
          );
        }
      } catch (error) {
        console.error(`Error handling participant removal: ${error}`);
        throw error;
      }
    });
