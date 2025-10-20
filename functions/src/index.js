const admin = require("firebase-admin");

// Firebase Admin 초기화
admin.initializeApp();

/**
 * 모든 Cloud Functions를 자동으로 export
 *
 * 각 기능별 폴더에서 함수를 import하여 통합
 */

// ===== Room Management =====
const onParticipantRemoved = require("./room/onParticipantRemoved");

module.exports = {
  // Room 관련
  onParticipantRemoved,

  // TODO: 추후 추가될 함수들
  // User 관련
  // onUserStatusChanged,

  // Notification 관련
  // sendArrivalNotification,
};
