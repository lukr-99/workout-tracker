# Languages

Ember speaks English and Czech. Settings, Appearance, Language has System, English and Čeština.
System follows the phone (or the browser) and uses English for any other language. The choice is
saved on this device only and is not part of a backup.

## How text works

The screens are shared code that Android and the web edition both compile, so text lives in Kotlin
catalogs, not in `strings.xml`.

- A catalog is an `object` in `ui/i18n/` that extends `TextCatalog`, one per screen or area.
- `msg("English", "Česky")` makes a message. The Czech text is a required argument.
  `{name}` marks a value: `Text.foo.text("name" to x)`.
- `plural(enOne, enOther, csOne, csFew, csMany)` makes a count with its noun. Czech needs three forms:
  1 (`1 série`), 2 to 4 (`2 série`) and 0 or 5 and more (`5 sérií`).
- In a composable call `.text()`. In a non-composable lambda read `LocalLanguage.current` in the
  composable and call `.get(lang)`. In a ViewModel or other code use `.now()`.
- New catalogs go in `AllCatalogs`. Use `sameInCzech = true` only for words that really are the same.
- Dates use `DateStyle.patternFor(language)`; Android sets the JVM locale, so numbers follow it.
  The web edition writes Czech day and month names itself and keeps a decimal point.
- Android-only text outside Compose (the app name and notifications) is in `res/values/strings.xml`
  with `res/values-cs/strings.xml`. These follow the phone's language.

## Checks

- `MessageCatalogTest` fails when a message has no Czech text, a Czech text equal to the English one,
  an empty plural form, or different `{values}` between the languages.
- `AndroidStringsTest` fails when `values-cs/strings.xml` lacks a key.
- `HardcodedTextTest` fails when a screen file under `ui/` gets more English text written straight into
  `Text(...)`, `title =` and similar than `app/src/test/resources/hardcoded-text-baseline.txt` allows.

## Not translated yet

Text that comes from `domain/` or `data/` (import problems, validation messages, What's New notes,
muscle names, `statsSummary`) stays English, because those layers must not depend on `ui/`.
Messages a ViewModel builds with `.now()` keep the language they were made in until the screen is left.
