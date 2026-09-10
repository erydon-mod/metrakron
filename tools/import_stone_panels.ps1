param([Parameter(Mandatory = $true)][string]$MasterDirectory)

# Copy exact centre-crop pixels; no scaling, tinting, or image metadata.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$targetDirectory = Join-Path $PSScriptRoot '../src/client/resources/assets/metrakron/textures/gui'
$sources = [ordered]@{
    'aganite.png' = 'aganite_panel.png'
    'glacium_master_1024.png' = 'glacium_panel.png'
    'kelastrion master.png' = 'kelastrion_quiet_panel.png'
    'selenephos.png' = 'selenephos_quiet_panel.png'
}
foreach ($entry in $sources.GetEnumerator()) {
    $source = [System.Drawing.Bitmap]::new((Join-Path $MasterDirectory $entry.Key))
    try {
        if ($source.Width -lt 960 -or $source.Height -lt 384) {
            throw "Master is too small: $($entry.Key)"
        }
        $left = [int][Math]::Floor(($source.Width - 960) / 2)
        $top = [int][Math]::Floor(($source.Height - 384) / 2)
        $panel = [System.Drawing.Bitmap]::new(960, 384)
        try {
            # SetPixel preserves the source crop without interpolation or metadata.
            for ($y = 0; $y -lt 384; $y++) {
                for ($x = 0; $x -lt 960; $x++) {
                    $panel.SetPixel($x, $y, $source.GetPixel($left + $x, $top + $y))
                }
            }
            $panel.Save((Join-Path $targetDirectory $entry.Value), [System.Drawing.Imaging.ImageFormat]::Png)
            Write-Output "$($entry.Value): 960x384 at ($left,$top) from $($source.Width)x$($source.Height)"
        } finally { $panel.Dispose() }
    } finally { $source.Dispose() }
}
