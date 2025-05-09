/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onRequest} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp()
// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
const functions = require("firebase-functions");
const admin = require("firebase-admin");

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.notificarCalificacionBaja = functions.firestore
  .document('Tutorados/{tutoradoId}')
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();

    const materiasAntes = beforeData.materias || [];
    const materiasDespues = afterData.materias || [];

    // Revisamos cada materia nueva o actualizada
    for (let i = 0; i < materiasDespues.length; i++) {
      const calificacionAntes = materiasAntes[i] ? materiasAntes[i].Calificacion : null;
      const calificacionDespues = materiasDespues[i].Calificacion;

      if (calificacionAntes !== calificacionDespues && calificacionDespues < 70) {
        console.log(`Calificación baja detectada en ${materiasDespues[i].Materia}: ${calificacionDespues}`);

        // Suponiendo que guardas el token del alumno en un campo `token`
        const alumnoToken = afterData.token; 

        if (!alumnoToken) {
          console.warn('No se encontró token del alumno, no se puede enviar notificación.');
          continue;
        }

        const payload = {
          notification: {
            title: '¡Atención! Calificación baja',
            body: `Obtuviste ${calificacionDespues} en ${materiasDespues[i].Materia}. Contacta a tu tutor.`,
          }
        };

        try {
          const response = await admin.messaging().sendToDevice(alumnoToken, payload);
          console.log('Notificación enviada:', response);
        } catch (error) {
          console.error('Error al enviar notificación:', error);
        }
      }
    }
  });
