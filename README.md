# Social Saver (Android)

Paste an Instagram, Facebook or X link → pick a quality → download. Files go to `Downloads/SocialSaver`.

## Get the app on your phone
1. Push this project to a GitHub repository (branch `main`).
2. GitHub Actions builds it automatically (about 5–8 minutes). Check the **Actions** tab.
3. Open the repo's **Releases** page on your phone and download **SocialSaver-arm64.apk**.
4. Open it and allow "Install unknown apps" when Android asks.

Every push creates a new release. New builds install over the old one, so your history is kept.

## If downloads stop working
Sites change often. Open **Settings → Update engine**.

## Tech
Kotlin · Jetpack Compose · [youtubedl-android](https://github.com/JunkFood02/youtubedl-android) (yt-dlp + FFmpeg on the phone) · photo fallback via Open Graph tags and the fxtwitter API.

See `docs/REQUIREMENTS.md` and `docs/QA_REPORT.md`.

Only download content you have the right to save, and respect creators and each platform's terms.
