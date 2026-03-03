param(
    [Parameter(Mandatory = $true)]
    [string]$InstallerPath,

    [string]$TimestampUrl = "http://timestamp.digicert.com"
)

$ErrorActionPreference = 'Stop'

Write-Host "Signing: $InstallerPath"

# Uses first suitable cert in CurrentUser/LocalMachine cert store.
# Replace /a with /sha1 <thumbprint> if you want a specific cert.
signtool sign /fd SHA256 /tr $TimestampUrl /td SHA256 /a "$InstallerPath"

Write-Host "Verifying signature..."
signtool verify /pa /v "$InstallerPath"
