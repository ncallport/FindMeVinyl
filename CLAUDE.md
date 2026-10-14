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
- When syncing albums from MusicBrainz, use `insertIfNotExists` (IGNORE) and never `insert` (REPLACE), so a user's owned and wanted flags are not overwritten.
- Anything that catches `Exception` around a suspend call must rethrow `CancellationException` first.
- Never hardcode the contact email in the User-Agent. `AppUserAgent.VALUE` is built from `BuildConfig.MUSICBRAINZ_CONTACT_EMAIL`, which comes from `musicbrainz.contact.email` in `local.properties`.
- Nominatim and Overpass are shared public servers. Always send the User-Agent, keep requests few, and keep the Overpass retry (up to 3 attempts on 429, 502, 503 and 504) and the `[timeout:25]` setting in the query. The query uses `nwr` so shops mapped as buildings are found.
- Nominatim returns latitude and longitude as strings.
- The `Surface` in `MainActivity` uses `Modifier.fillMaxSize().safeDrawingPadding()`. Keep it when editing that file.
- `OpeningHours` reads a subset of the OpenStreetMap hours syntax. Text it cannot read returns null and the shop row shows the raw text. `PH` rules are ignored for the weekday check, and the status line says public holidays may differ.
- An owned album is never wanted. `toggleOwned` clears the wanted flag when it marks an album owned, `AlbumRow` hides `+ Want` on owned albums, and the `getWantedAlbums` query excludes owned albums so older rows with both flags do not show up.
- Compose tests: `assertExists()` and similar are member functions, not imports. `weight()` needs no import inside a Row or Column. `onNodeWithText` fails when two nodes match, for example a text field and a result with the same text. A `LazyColumn` needs a bounded height, so give it `weight(1f)` inside a Column. A `clickable` row merges the nodes inside it, so finders for things inside it (the cover, for example) need `useUnmergedTree = true`.
- Rotation: small values (current screen, typed text) use `rememberSaveable`. Result lists never go in the saved-state Bundle, because a big city's shop list can exceed its size limit. Put them in `SearchResultCache` and save only a flag. `ScreenSaver` saves `Screen` as a list of strings.
- Album covers come from the Cover Art Archive at `coverartarchive.org/release-group/{id}/front-250`, using the MusicBrainz release group id the discography already has. A 404 means no art. `CoverArtLoader.shared` keeps images and 404s in memory, allows four downloads at once, sends `AppUserAgent.VALUE`, and returns null on any failure. `CoverImage` shows `ic_cover_placeholder` until a cover arrives and when there is none. The placeholder copies the art in `ic_launcher_foreground.xml`, so change both together.
- `CoverArtLoaderTest` uses `@GraphicsMode(GraphicsMode.Mode.NATIVE)`. In the default mode Robolectric returns a fake bitmap for bytes that are not an image, where a device returns null. Give `MockWebServer.takeRequest` a timeout, because it waits forever when no request is made.
- Restore tests use `runComposeUiTest` with `StateRestorationTester(this)` and `emulateSaveAndRestore()`, under `@OptIn(ExperimentalTestApi::class)`. It takes a `ComposeUiTest`, not a rule.

## Running the app

- The emulator on this machine needs Graphics set to Software - GLES 2.0, or the screen stays black.
- Fake the GPS position with `adb emu geo fix <longitude> <latitude>` (longitude first). It resets when the emulator restarts.

## Known gaps

- Search results are kept in memory only (`SearchResultCache.shared`). Rotation keeps them, but if Android kills the process the user gets the right screen and typed text back and has to search again. A search that is loading during a rotation is not resumed.
- Covers are cached in memory only. archive.org sends no cache headers, and every album costs a redirect request per run of the app. The cover is the release group's usual front cover, not the cover of a vinyl pressing.
- Opening hours use the phone's clock and do not know about public holidays.
- The app has only been run on the emulator.