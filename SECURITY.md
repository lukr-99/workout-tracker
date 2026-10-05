# Security

## Supported versions

Ember is a personal app. Only the latest GitHub Release gets fixes. Debug builds (Ember dev) are
not supported.

## Reporting

Report a problem privately to the repository owner, `lukr-99` on GitHub. Do not open a public issue
for anything that could expose someone's data.

## Trust boundaries

The app holds one person's training history, body weight and GPS traces. It has no account, no
server and no analytics. These inputs cross into the app and are treated as untrusted:

| Input | Where | Checks today |
|---|---|---|
| Update APK | Public GitHub Releases API, then the system installer | HTTPS only, before and after redirects. Exact asset names (`Ember-<version>.apk` and its `.sha256`). Every announced byte must arrive, the SHA-256 must match, and the package name and signing certificate must match the installed app before the installer opens. Development builds never update. |
| JSON backup or export | File picker or the backup folder | Refused over 64 MB. Parsed with an allowlist of format versions, planned and previewed before any write, committed in one transaction. Each photo must decode as an image and be at most 8 MB, and is written under a name the app chooses. |
| Lyfta CSV | File picker | Parsed into drafts and validated by `WorkoutFactory` before saving. |
| GPX run | File picker | XML parser with DTDs and external entities turned off (`GpxCodec`). |
| wger catalog | `wger.de` over HTTPS | Paging limited to the configured origin. Only fills blank fields on rows that came from wger. |
| Route snapping | `router.project-osrm.org` over HTTPS | Only coordinates the owner tapped are sent. |
| Map tiles and exercise images | OpenFreeMap and GitHub raw content over HTTPS | Rendered as images only. |
| Health Connect | Android Health Connect | Only the permissions the owner grants: exercise, weight, distance, calories, routes. |
| Guide links on exercises | Typed by the owner | Only `http` and `https` are accepted, then opened in the browser. |

Permissions and why the app needs them:

- **Location and foreground location service:** to record a run.
- **Wake lock:** to keep GPS fixes coming with the screen off.
- **Notifications:** for the running-run notification.
- **Request install packages:** for the in-app updater.
- **Internet:** for the peers above.
- **Vibrate:** for haptics.

Secrets never go into git:

- `keystore.jks` and `keystore.properties` (release signing);
- `spotify.properties`;
- `local.properties`.

## Recovery

- **Backup and restore:** turn on automatic backup in Settings, or Save JSON from Settings, Data.
  A backup holds everything, settings and photos included. Restore by importing it: Merge adds to
  what is there, Replace everything puts the backup back exactly. A failed restore changes nothing.
- **Deleting data:** Settings, Data, danger zone, Delete all data, then type "delete". Uninstalling
  the app also removes all of its data. Backup files and Health Connect data are not touched.
- The full contract is in [docs/data-contract.md](docs/data-contract.md).
- **Rollback:** a database migration cannot be undone. To go back to an older version, export,
  reinstall and import. Older builds reject newer export versions.
- **Lost signing key:** without the keystore, no update can install in place. Every update would
  then need an export, an uninstall and an import. Keep two backups of the keystore
  (`docs/RELEASING.md`).
