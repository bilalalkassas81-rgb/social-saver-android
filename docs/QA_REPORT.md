# QA Report: v1

## Automated tests (run on every push by GitHub Actions)
52 unit tests, all passing (v1.2), plus a simulated run of the in-browser script:

| Area | Tests | Covers |
|---|---|---|
| LinkValidator | 7 | Instagram/Facebook/X detection, links inside shared text, punctuation, look-alike domains, invalid input |
| QualityPlanner | 8 | Reel labels (1080p not 1920p), sorting, size estimates, duplicates, caps, fallbacks |
| InfoParser | 6 | Single video, carousel posts, direct URLs, noisy output, empty/garbage output |
| PhotoParsing | 8 | Tweet IDs, X photos and videos, Open Graph images, **regression: Reel links never treated as photos** |
| ErrorText | 6 | Login/rate-limit, unsupported, no video, network, unknown errors |
| InstagramParsing | 10 | Shortcodes, media id (checked against yt-dlp), session token, logged-out GraphQL reel, quality list, carousels |
| CookieFile | 4 | Login session detection, cookie file for yt-dlp |
| HistoryCodec | 3 | Save/load round trip (incl. Arabic text), corrupt file, invalid entries |

## Manual checklist (on your phone)
- [ ] App opens; first Fetch may take ~10–20 s while the engine unpacks (first launch only)
- [ ] Instagram Reel while **logged out** → shows quality choices → downloads the video
- [ ] If it fails: tap **Details → Copy details** and send to the developer
- [ ] Log in → app retries automatically → video downloads and plays
- [ ] Settings → Accounts shows "Logged in"; Log out works
- [ ] Instagram carousel post → all items saved
- [ ] Instagram photo post → photo saved
- [ ] Facebook public video → downloads
- [ ] X video tweet → downloads; X photo tweet → photos saved
- [ ] Audio only → MP3 plays
- [ ] Cancel during download → "Download cancelled", no broken file left
- [ ] Airplane mode → friendly network error
- [ ] Random text in link box → "doesn't look like a link"
- [ ] History: open, share, remove; Clear history keeps files
- [ ] Settings → Update engine works
