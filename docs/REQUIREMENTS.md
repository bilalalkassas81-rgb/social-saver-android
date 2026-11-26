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
