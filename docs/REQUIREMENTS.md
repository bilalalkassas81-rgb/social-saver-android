# Social Saver for Android, v1: Requirements (approved)

**Platform:** Android 7.0+ · personal use (sideloaded APK, not Google Play) · English UI

**Sites:** Instagram (reels, videos, photos, carousels) · Facebook (videos, reels) · X/Twitter (videos, photos)

**Screens**
1. **Download**: paste link → Fetch → preview (thumbnail, title) → choose quality (e.g. 1080p / 720p / Audio only) → Download with progress and Cancel.
2. **History**: past downloads with thumbnail, platform, quality, date. Open, share, remove from list.
3. **Settings**: update the download engine (yt-dlp), clear history.

**Storage:** `Downloads/SocialSaver`, visible in the gallery.

**Out of scope for v1:** share-menu integration, batch downloads, Arabic UI, Play Store, login for private posts.

**Known risks:** Instagram/Facebook often require login (v1 = public posts only). Photo-only posts use a fallback that may give lower resolution on Instagram/Facebook.

## v1.1 changes (after first device test)
- **Bug fix:** a Reel's cover image was saved instead of the video. Video posts never fall back to the cover image now.
- **Login:** optional Instagram / Facebook login inside the app (their own login page; password never reaches the app). Downloads use the session; Instagram posts also come through Instagram's own API in original quality.
- **X:** videos also work through the fallback, not just photos.
- **New design:** own colour palette and headline font, bigger link panel, quality pills, gallery-style Saved screen, accounts in Settings.

## v1.2 changes
- **Instagram without login.** Root cause: yt-dlp only downloads logged-out Instagram posts when it can impersonate a real browser (curl_cffi), which isn't available on Android. The app now makes the same requests instagram.com makes in a browser: first directly, then inside an invisible WebView (the phone's own Chrome) as a backup. Login stays as an optional last resort.
- **Quality choice for Instagram** from the sizes Instagram serves.
- **Engine updates itself** once a day.
- **"Details" on errors** so failures can be copied and sent for diagnosis.
- **App icon** now uses the app's own indigo + coral.

## v1.3 changes
- **In-app updates:** on every launch the app checks GitHub Releases. If a newer build exists, a popup offers **Update now** (downloads inside the app and opens Android's installer) or **Later**. Settings → About has **Check now**.
- **About:** "Made by Bilkas Group".
- Requires the repository to be public so the app can read the releases.

## v1.3.1 fix
- Play Protect blocked build-5, most likely because it asked for permission to install apps (REQUEST_INSTALL_PACKAGES). That permission is removed: **Update now** opens the APK link in the browser, which downloads and installs it.
