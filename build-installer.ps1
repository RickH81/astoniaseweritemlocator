$ErrorActionPreference = 'Stop'

Set-Location $PSScriptRoot

function Invoke-External {
	param(
		[Parameter(Mandatory = $true)]
		[string]$Label,

		[Parameter(Mandatory = $true)]
		[scriptblock]$Command
	)

	& $Command
	if ($LASTEXITCODE -ne 0) {
		throw "$Label failed with exit code $LASTEXITCODE."
	}
}

Write-Host "[1/6] Cleaning build folders..."
if (Test-Path build) { Remove-Item -Recurse -Force build }
if (Test-Path installer-dist) { Remove-Item -Recurse -Force installer-dist }

Write-Host "[2/6] Compiling Java sources..."
New-Item -ItemType Directory -Path build\classes\com\rustharbor -Force | Out-Null
Invoke-External -Label "javac" -Command { javac -d build\classes src\main\java\com\rustharbor\*.java }

Write-Host "[3/6] Copying app resources..."
Copy-Item src\main\java\com\rustharbor\sewer_map.png build\classes\sewer_map.png -Force

Write-Host "[4/6] Building runnable JAR..."
Invoke-External -Label "jar" -Command { jar --create --file build\seweritemlocator.jar --main-class com.rustharbor.Main -C build\classes . }

Write-Host "[5/6] Building installer EXE via jpackage..."

if (-not (Get-Command wix -ErrorAction SilentlyContinue) -and -not (Get-Command candle -ErrorAction SilentlyContinue)) {
	Write-Warning "WiX Toolset is required for jpackage --type exe."
	Write-Warning "Install with: winget install --id WiXToolset.WiXToolset -e --source winget"
}

Invoke-External -Label "jpackage" -Command {
	jpackage --type exe --name SewerItemLocator --input build --main-jar seweritemlocator.jar --dest installer-dist --app-version 1.1.0 --vendor "Vozziks" --win-menu --win-menu-group "Sewer Item Locator" --win-shortcut --win-shortcut-prompt
}

Write-Host "[6/6] Done. Installer output:" -ForegroundColor Green
if (-not (Test-Path installer-dist)) {
	throw "Installer output folder 'installer-dist' was not created."
}

$installer = Get-ChildItem installer-dist -Filter *.exe -ErrorAction SilentlyContinue
if (-not $installer) {
	throw "No installer EXE found in installer-dist."
}

$installer | Select-Object FullName, Length, LastWriteTime
