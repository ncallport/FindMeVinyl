# FindMeVinyl

A personal Android app (Kotlin, Jetpack Compose) for finding record shops, browsing artist discographies and keeping a want list. It is a hobby project in a public repository, so nothing personal or work-related belongs in this file or in commits.

## How to work on it

- Work test first. For each new behaviour: write the failing test, run it and check it fails for the right reason, write the least code that passes, then run the whole suite.
- Keep steps small. Say briefly what changed and why.
- Run `.\gradlew testDebugUnitTest` after every change (PowerShell on Windows). Roughly 230 tests pass today, and all of them must still pass before moving on.
- Run one class with `.\gradlew testDebugUnitTest --tests "com.nickallport.findmevinyl.shops.OpeningHoursTest"`.
- Test reports are in `app/build/reports/tests/testDebugUnitTest/index.html`.
- Ask before adding a dependency or changing a version. Several versions below were chosen to fix real problems.
- Never stage `local.properties`.

## Layout

The package root is `com.nickallport.findmevinyl`.

- `musicbrainz`, `nominatim`, `overpass`: Retrofit interfaces, DTOs and mappers for the three public APIs.
- `artist`, `discography`, `favourites`, `wantlist`, `shops`, `export`: logic and view models for each feature.
- `storage`: the Room database, entities and DAOs.
- `location`: the device location lookup.
- `coverart`: the Cover Art Archive URL and `CoverArtLoader`, which downloads album covers.
- `image`: `ImageLoader`, which downloads any image by URL. `CoverArtLoader` is a thin wrapper around it. `wikidata`: the Wikidata API, used to find artist pictures.
- `ui.*`: composables only (`ui.artist`, `ui.discography`, `ui.favourites`, `ui.shops`, `ui.wantlist`, `ui.theme`).

Conventions:

- View models are plain classes, not `androidx.lifecycle.ViewModel`. They sit beside the logic they serve, not under `ui`, and expose suspend functions that return a UI state.
- Screens are stateless composables. A "Route" composable owns the state and takes suspend functions as parameters, so tests can pass fakes and never touch the network.
- `MainActivity` wires everything together. Navigation is a sealed class `Screen` plus a `BackHandler`.
- Composable tests run on the JVM with Robolectric. Network tests use MockWebServer. Pure logic tests use plain JUnit.

## Stack and versions

Kotlin 2.2.10, Android Gradle Plugin 9 with built-in Kotlin, KSP 2.2.10-2.0.2, Room 2.7.2, Retrofit 2.11.0 with Gson, OkHttp 4.12.0 (declared explicitly), Robolectric 4.13, MockWebServer 4.12.0.

- Gradle has to run on JDK 21. On JDK 25 Robolectric fails with "Unsupported class file major version 69". Check with `.\gradlew --version`.
- `gradle.properties` contains `android.disallowKotlinSourceSets=false`. KSP needs it with AGP 9. Keep it.
- Do not set `ksp.useKSP2=false`. It breaks the build with AGP 9.
- Do not downgrade Room below 2.7.2. Room 2.6.1 generates broken code for suspend DAO methods under KSP2.
- `app/src/test/resources/robolectric.properties` sets `sdk=34` because the app targets SDK 37 and Robolectric stops at 34. Keep it.

## Rules learned the hard way

- DAO methods are `suspend`. Room refuses main-thread queries in the real app, and tests use `allowMainThreadQueries()`. DAO methods return `Long` or `Int` instead of `Unit`.
- When syncing albums from MusicBrainz, use `insertIfNotExists` or `insertAllIfNotExist` (IGNORE) and never `insert` (REPLACE), so a user's owned and wanted flags are not overwritten. Save a whole discography with `insertAllIfNotExist`, which is one transaction. 419 single inserts took 7 seconds on the emulator.
- MusicBrainz returns 25 results unless asked for more, and at most 100 a page. `DiscographyViewModel.discography(artistId)` is a `Flow` that asks for 100 at a time and follows the pages to the `release-group-count`. Each request starts at least 1.1 seconds after the previous one started, and it stops after 10 pages. Each page is saved as it arrives and a state is emitted after it, so the first page shows while the rest load. The list is always built from the saved albums (`loadSaved`), never from a response. An artist opened before starts with its saved albums, before any request. Once as many albums are saved as MusicBrainz lists, the remaining pages are skipped, so reopening an artist is one request. A first page that fails with nothing saved is an `Error`. Any other failure keeps the list and sets `loadError`, so the list is never silently short. Give `MockWebServer.takeRequest` a timeout in these tests.
- Taps on Owned, Want and Save must never reload the discography from MusicBrainz. `DiscographyRoute` calls `loadSaved` and copies the flags onto the albums on screen, album by album, so albums that arrived meanwhile are kept. Before this, every tap re-downloaded every page.
- MusicBrainz sends `"first-release-date": ""` for undated albums. The mapper turns it into null, and `loadSaved` also treats `""` as undated, because rows saved earlier have it. An empty string sorts before every real date.
- The emulator and this machine share one IP, and MusicBrainz limits requests per IP. Repeatedly reopening artists, format lookups and manual probes got "HTTP 503 ... busy" for several minutes. Keep emulator runs against MusicBrainz few, and wait when it answers 503.
- `DiscographyScreen` gives every row and header a key, so the list holds its place as pages add albums above what is on screen. Keep the keys.
- The format filter (`FormatFilter`, `FormatFilterBar`) works on what is stored. An album with `formats == null` has not been looked up, and it stays in the list while a format is chosen, marked "Checking formats…", until its lookup shows whether it matches. An album looked up with no known formats (an empty list) is checked and does not match. Chips and counts come from the albums known so far, so they grow as lookups arrive. The choice is `rememberSaveable` in `DiscographyRoute`, one format at a time.
- `DiscographyRoute` records every format lookup into its own state as it returns (`lookUpFormats`). Without that, the filter, the chips and the counts would be stale, because a row's own result stays inside the row. `refreshSaved` also merges formats from the saved albums, never replacing known formats with none.
- MusicBrainz answers carry `X-RateLimit-Limit` (400 was seen) and `X-RateLimit-Remaining`. Choosing a format while many albums are unchecked starts lookups for the rows on screen, one a second, for as long as it is on screen. Keep emulator sessions with a filter on short.
- Anything that catches `Exception` around a suspend call must rethrow `CancellationException` first.
- Never hardcode the contact email in the User-Agent. `AppUserAgent.VALUE` is built from `BuildConfig.MUSICBRAINZ_CONTACT_EMAIL`, which comes from `musicbrainz.contact.email` in `local.properties`.
- Nominatim and Overpass are shared public servers. Always send the User-Agent, keep requests few, and keep the Overpass retry (up to 3 attempts on 429, 502, 503 and 504) and the `[timeout:25]` setting in the query. The query uses `nwr` so shops mapped as buildings are found.
- Nominatim returns latitude and longitude as strings.
- The `Surface` in `MainActivity` uses `Modifier.fillMaxSize().safeDrawingPadding()`. Keep it when editing that file. It sits inside a `Box` with the theme background, so the status and navigation bar areas follow the theme. Without the Box they were flat white and the clock was unreadable in dark mode.
- `OpeningHours` reads a subset of the OpenStreetMap hours syntax. Text it cannot read returns null and the shop row shows the raw text. `PH` rules are ignored for the weekday check, and the status line says public holidays may differ.
- An owned album is never wanted. `toggleOwned` clears the wanted flag when it marks an album owned, `AlbumRow` hides `+ Want` on owned albums, and the `getWantedAlbums` query excludes owned albums so older rows with both flags do not show up.
- Look and feel: the app has its own palette in both light and dark (`ui.theme`), the teal and orange of the icon. Dynamic colour (the wallpaper) is off (`USE_DYNAMIC_COLOR`). `ThemeContrastTest` checks every text and background pair is at least 4.5:1, so change colours there first. The icon teal is too light for text or buttons on white (3.4:1), so light mode uses `TealDeep` for primary.
- Navigation has no menu. `Screen.Home` is the front screen (`HomeScreen`: the big logo and ARTISTS, WANTS and SHOPS buttons). Every other page gets a `Masthead` bar from `MainActivity`, and tapping it goes home. Back goes where `Screen.backTarget()` says: home from any page, from the results back to the Artists page, and from a discography back to where you came from. The Artists page (`Screen.Search`, `ArtistsScreen`) has a search field and the favourite artists. Searching opens `Screen.ArtistResults(query)` (`ArtistResultsRoute`), which searches when it opens unless `SearchResultCache` already holds results for that same query (`artistQuery`), so rotation and coming back from a discography never search again. Only a successful search is cached.
- The logo (`ui.brand.Logo`) is drawn in code with Android's built in `cursive` font, curved along an arc with `drawTextOnPath`. The three button icons (`Glyphs`) are path data, because the Material icons library is not a dependency. Ask before adding one.
- The Shops page (`ShopSearchRoute`) is a search field (the keyboard searches, like the Artists page) and a Near me button. It starts from the last result in `SearchResultCache.shop`, so coming back or rotating costs no request. The first time it opens with nothing kept it searches near you once by itself (`nearbyTried` stops it repeating, and is only set once the search has finished, so a rotation in the middle tries again). That is one Overpass request, which took about 20 seconds on the emulator. After that, only Near me or a typed town searches. A failed nearby search is kept and shown, and Near me tries again.
- There is no search button: `SearchField` searches on the keyboard's search key (trimmed, and not when blank). The logo bar is `focusProperties { canFocus = false }`, because focus moved onto it after the search key and left a grey band.
- Compose tests: `assertExists()` and similar are member functions, not imports. `weight()` needs no import inside a Row or Column. `onNodeWithText` fails when two nodes match, for example a text field and a result with the same text. A `LazyColumn` needs a bounded height, so give it `weight(1f)` inside a Column. A `clickable` row merges the nodes inside it, so finders for things inside it (the cover, for example) need `useUnmergedTree = true`.
- Rotation: small values (current screen, typed text) use `rememberSaveable`. Result lists never go in the saved-state Bundle, because a big city's shop list can exceed its size limit. Put them in `SearchResultCache` and save only a flag. `ScreenSaver` saves `Screen` as a list of strings.
- Album covers come from the Cover Art Archive at `coverartarchive.org/release-group/{id}/front-250`, using the MusicBrainz release group id the discography already has. A 404 means no art. `CoverArtLoader.shared` keeps images and 404s in memory, allows four downloads at once, sends `AppUserAgent.VALUE`, and returns null on any failure. `CoverImage` shows `ic_cover_placeholder` until a cover arrives and when there is none. The placeholder copies the art in `ic_launcher_foreground.xml`, so change both together.
- Release formats are fetched per album from `release?release-group={id}&inc=media&limit=100` and stored in `albums.formats` as MusicBrainz's raw names joined with `|` (a comma will not do, some names contain one), with `formatsCheckedAt`. Null means never fetched, an empty string means fetched and none known. `ReleaseFormats` turns the names into the labels (LP, 12", 10", 7", CD, MC, Digital), so the rules can change without fetching again. Formats are looked up only for rows on screen, and refreshed after 30 days. Every lookup goes through `MusicBrainzThrottle.shared`: one at a time, 1.1 seconds apart, a pause after a 429 or 503. A started lookup saves its result inside the throttle block, so cancelling the row does not lose it.
- Every change to a Room entity needs a version bump and a `Migration` in `storage/Migrations.kt`, never a destructive fallback, because it would delete a user's owned and wanted flags. Room writes each version's schema to `app/schemas`, and those files are committed. `MigrationTest` builds the old database from SQL, fills it with data, and opens it with the current database.
- Artist pictures on the search results come from Wikimedia Commons, found through an exact chain and never by name: MusicBrainz `artist/{id}?inc=url-rels` gives the Wikidata item, Wikidata `wbgetclaims` property P18 gives the file, and the address is `commons.wikimedia.org/wiki/Special:FilePath/{file}?width=120`. If Wikidata has no picture, MusicBrainz's own "image" link to a Commons file is used. `ArtistImageViewModel.loadImageUrl` keeps the result in the `artist_images` table (a null address means "checked, no picture", and is kept too), looks again after 90 days, and keeps nothing on a failure. Finding and saving run inside one `MusicBrainzThrottle` turn, so a cancelled row cannot lose a result. `ImageLoader.shared` downloads the picture. The search results show "Artist photos: Wikimedia Commons" while a picture is on screen, because Commons asks for credit. Do not match artists to pictures by name (Deezer put the wrong "Genesis" first).
- `SquareImage` is the one composable for the 56dp square with the record placeholder, used for album covers (`CoverImage`) and artist pictures (`ArtistImage`).
- `CoverArtLoaderTest` uses `@GraphicsMode(GraphicsMode.Mode.NATIVE)`. In the default mode Robolectric returns a fake bitmap for bytes that are not an image, where a device returns null. Give `MockWebServer.takeRequest` a timeout, because it waits forever when no request is made.
- Restore tests use `runComposeUiTest` with `StateRestorationTester(this)` and `emulateSaveAndRestore()`, under `@OptIn(ExperimentalTestApi::class)`. It takes a `ComposeUiTest`, not a rule.

## Running the app

- The emulator on this machine needs Graphics set to Software - GLES 2.0, or the screen stays black.
- Fake the GPS position with `adb emu geo fix <longitude> <latitude>` (longitude first). It resets when the emulator restarts.

## Known gaps

- Search results are kept in memory only (`SearchResultCache.shared`). Rotation keeps them, but if Android kills the process the user gets the right screen and typed text back and has to search again. A search that is loading during a rotation is not resumed.
- The formats of an album are those of every release in its release group, so a later CD reissue of a record that was only ever on vinyl shows CD. Only the first 100 releases of a group are read. Format names that are not shown (DVD, SACD layers and so on) are ignored. New rows fill in at about one album a second, and the discography paging does not use the shared throttle.
- Artist pictures show on the search results and the favourites list (both use `ArtistImage` and `PhotoCredit`). A first search costs about two requests per visible artist (MusicBrainz and Wikidata), one a second, and nothing after that for 90 days. Coverage was only measured on well-known artists, so obscure artists may often have none. The picture is a photo cropped to a square, so some faces are cut off.
- Covers are cached in memory only. archive.org sends no cache headers, and every album costs a redirect request per run of the app. The cover is the release group's usual front cover, not the cover of a vinyl pressing.
- Try again after a failed page loads the discography again from page 1. A first page that fails with nothing saved shows only the error, with no Try again button. A list is built from every saved album, so an album MusicBrainz later removes from a discography still shows.
- The format filter is only as complete as the lookups. For an artist whose albums have mostly not been looked up (Genesis showed 366 of 419 unchecked), the counts are low and the list keeps changing while a format is chosen. Lookups run only for rows on screen. There is no "check all" button, no choosing more than one format, and the chosen format is not kept when you leave the artist.
- Opening hours use the phone's clock and do not know about public holidays.
- The app has only been run on the emulator.