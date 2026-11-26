# QA Report: v1

## Automated tests (run on every push by GitHub Actions)
35 unit tests, all passing:

| Area | Tests | Covers |
|---|---|---|
| LinkValidator | 7 | Instagram/Facebook/X detection, links inside shared text, punctuation, look-alike domains, invalid input |
| QualityPlanner | 8 | Reel labels (1080p not 1920p), sorting, size estimates, duplicates, caps, fallbacks |
| InfoParser | 6 | Single video, carousel posts, direct URLs, noisy output, empty/garbage output |
| PhotoParsing | 5 | Tweet IDs, original-size X photos, fxtwitter API, Open Graph images |
| ErrorText | 6 | Login/rate-limit, unsupported, no video, network, unknown errors |
| HistoryCodec | 3 | Save/load round trip (incl. Arabic text), corrupt file, invalid entries |

## Manual checklist (on your phone)
- [ ] App opens; first Fetch may take ~10–20 s while the engine unpacks (first launch only)
- [ ] Instagram Reel (public) → shows qualities → 1080p downloads → plays in gallery
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
