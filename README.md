# S.K TRADERS – Business Manager (Android, Java)

Native Android app (Java) that hosts the original UI from `app/src/main/assets/index.html`.

## Build
- Android Studio: open this folder, Run.
- GitHub Actions: push the folder to a repo; the APK appears under Actions > Artifacts (`app-debug.apk`).

## Native features (MainActivity.java)
- Offline, persistent data (WebView local storage inside the app)
- JSON backup / CSV export saved to the phone's Downloads folder
- Print / Save PDF via Android's print service
- WhatsApp share opens WhatsApp
- Restore backup uses the system file picker
- Back button closes dialogs, then returns to Dashboard

Back up regularly from Settings: uninstalling the app or clearing its data erases records.
