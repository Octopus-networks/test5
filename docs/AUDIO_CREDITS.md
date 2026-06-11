# Audio Credits — Adhan Recordings

The adhan recordings bundled in `app/src/main/res/raw/` are freely licensed works
from Wikimedia Commons. Attribution and license details below. The in-app sound
picker shows a credits line referencing this file.

| Resource file | Source file (Wikimedia Commons) | Author | License | Duration |
|---|---|---|---|---|
| `adhan_short.ogg` (picker: "Full Adhan") | [Azan.ogg](https://commons.wikimedia.org/wiki/File:Azan.ogg) | Andrewler | CC BY-SA 4.0 | 3:03 |
| `adhan_makkah.ogg` (picker: "Serene Adhan") | [Beautiful adhan.ogg](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg) | Adam-synagda | CC0 1.0 (Public Domain Dedication) | 2:34 |
| `adhan_madinah.ogg` (picker: "Madinah Adhan — short clip") | [Oración Al-Azzan.ogg](https://commons.wikimedia.org/wiki/File:Oraci%C3%B3n_Al-Azzan.ogg) | B9 | Public Domain | 0:30 |
| `adhan_egypt.mp3` (picker: "Sunnah-style Adhan") | [The Adhan - Muslim Call to Prayer - Aaqib Azeez.mp3](https://commons.wikimedia.org/wiki/File:The_Adhan_-_Muslim_Call_to_Prayer.mp3) | Atcovi / Aaqib Azeez | CC BY-SA 4.0 | 1:27 |
| `takbeer.ogg` (picker: "Short Adhan") | [Adhan wiki.oga](https://commons.wikimedia.org/wiki/File:Adhan_wiki.oga) | Jarih | CC BY-SA 3.0 (also GFDL 1.2+) | 0:27 |
| `bird_chirp.wav` | Synthesized in-house (numpy) | — | Project license | 0:01 |

Notes:

- Picker pattern keys (`TAKBEER`, `ADHAN_FULL`, `ADHAN_MAKKAH`, `ADHAN_MADINAH`,
  `ADHAN_EGYPT`) are persisted in user SharedPreferences and resolved by resource
  name at runtime, so the resource file names intentionally stay stable even
  though the display labels were renamed to describe the recordings honestly.
- The `ADHAN_AQSA` option was removed from the picker (no freely-licensed
  recording sourced yet); its strings remain unused in `strings.xml`.
- CC BY-SA recordings are redistributed unmodified as part of a collection; this
  file plus the in-app credits line provide the required attribution and license
  notice. If a recording is ever replaced or edited, update this table.
- A recording of Sabah Fakhry's 1985 adhan on Commons was deliberately NOT used:
  its public-domain claim covers the adhan text, not the 1985 sound recording.
