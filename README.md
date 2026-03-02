# Astonia Sewer Item Locator

A Java Swing desktop tool for plotting and narrowing sewer item search zones on a rotated map.

## Features

- Rotated base map background (45° clockwise)
- Coordinate plotting using `X` and `Y` values in range `0..255`
- Directional narrowing with `NW`, `NE`, `SW`, `SE`
- Incremental rectangular search-area restriction from each entry
- Zoom in/out with scroll support and drag panning when zoomed
- Corner-mapped grid coordinates:
  - North corner = `0,0`
  - East corner = `255,0`
  - South corner = `255,255`
  - West corner = `0,255`

## Requirements

- Windows (or any OS with Java 17+)
- JDK 17 or newer on PATH (`java`, `javac`)
- Optional for packaging executable: `jpackage` (included with most modern JDKs)

## Project Layout

- Main app source: `src/main/java/com/rustharbor/Main.java`
- Workbook parser: `src/main/java/com/rustharbor/SewerWorkbookCube.java`
- Map image: `src/main/java/com/rustharbor/sewer_map.png`
- Workbook data: `src/main/java/com/rustharbor/SEWERS.xlsx`

## Run (quick)

From project root (`seweritemlocator`):

```powershell
javac src\main\java\com\rustharbor\SewerWorkbookCube.java src\main\java\com\rustharbor\Main.java
java -cp src\main\java com.rustharbor.Main
```

## Build executable (Windows app-image)

From project root (`seweritemlocator`):

```powershell
if (Test-Path build) { Remove-Item -Recurse -Force build }
if (Test-Path dist) { Remove-Item -Recurse -Force dist }
New-Item -ItemType Directory -Path build\classes\com\rustharbor -Force | Out-Null

javac -d build\classes src\main\java\com\rustharbor\*.java
Copy-Item src\main\java\com\rustharbor\sewer_map.png build\classes\sewer_map.png -Force
Copy-Item src\main\java\com\rustharbor\SEWERS.xlsx build\classes\com\rustharbor\SEWERS.xlsx -Force

jar --create --file build\seweritemlocator.jar --main-class com.rustharbor.Main -C build\classes .

jpackage --type app-image --name SewerItemLocator --input build --main-jar seweritemlocator.jar --dest dist
```

Output executable:

- `dist\SewerItemLocator\SewerItemLocator.exe`

## Notes

- If map does not appear, confirm `sewer_map.png` exists in `src/main/java/com/rustharbor/`.
- If you update `SEWERS.xlsx`, restart the app to reload workbook data.

## License

This project is licensed under the GNU General Public License v3.0.
See [LICENSE](LICENSE).
