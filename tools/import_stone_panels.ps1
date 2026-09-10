param([Parameter(Mandatory = $true)][string]$MasterDirectory, [string]$Only = '')

# Copy exact centre-crop pixels; no scaling, tinting, or image metadata.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$targetDirectory = Join-Path $PSScriptRoot '../src/client/resources/assets/metrakron/textures/gui'
$sources = [ordered]@{
    'aganite.png' = 'aganite_panel.png'
    'aterzon.png' = 'aterzon_panel.png'
    'borealis_master_1024.png' = 'borealis_panel.png'
    'brectite_master_1024.png' = 'brectite_panel.png'
    'calacattum_master_1024.png' = 'calacattum_panel.png'
    'chalstrom_master_1024.png' = 'chalstrom_panel.png'
    'chrysonyx_master_1024.png' = 'chrysonyx_panel.png'
    'etruscus_master_1024.png' = 'etruscus_panel.png'
    'gelastrum_master_1024.png' = 'gelastrum_panel.png'
    'glacium_master_1024.png' = 'glacium_panel.png'
    'hesperion master.jpg' = 'hesperion_panel.png'
    'imperium_master_1024.png' = 'imperium_panel.png'
    'kelastrion master.png' = 'kelastrion_quiet_panel.png'
    'kylorion.png' = 'kylorion_panel.png'
    'latmion master.png' = 'latmion_panel.png'
    'laurentium_master_1024.png' = 'laurentium_panel.png'
    'mielonyx_master_1024.png' = 'mielonyx_panel.png'
    'noxoplis_master_1024.png' = 'noxoplis_panel.png'
    'porpyros.png' = 'porphyros_panel.png'
    'portorium_master_1024.png' = 'portorium_panel.png'
    'psamatheon master.png' = 'psamatheon_panel.png'
    'rosinium_master_1024.png' = 'rosinium_panel.png'
    'Sanguenite.png' = 'sanguenite_panel.png'
    'selenephos.png' = 'selenephos_quiet_panel.png'
    'solistra.png' = 'solistra_panel.png'
    'striatus.png' = 'striatus_panel.png'
}
foreach ($entry in $sources.GetEnumerator()) {
    if ($Only -and $entry.Key -ne $Only) { continue }
    $source = [System.Drawing.Bitmap]::new((Join-Path $MasterDirectory $entry.Key))
    try {
        $cropWidth = [int][Math]::Min(960, [Math]::Min($source.Width, $source.Height * 2.5))
        $cropHeight = [int][Math]::Floor($cropWidth / 2.5)
        if ($cropWidth -lt 384) {
            throw "Master is too small: $($entry.Key)"
        }
        $left = [int][Math]::Floor(($source.Width - $cropWidth) / 2)
        $top = [int][Math]::Floor(($source.Height - $cropHeight) / 2)
        $panel = [System.Drawing.Bitmap]::new($cropWidth, $cropHeight)
        try {
            # SetPixel preserves the source crop without interpolation or metadata.
            for ($y = 0; $y -lt $cropHeight; $y++) {
                for ($x = 0; $x -lt $cropWidth; $x++) {
                    $panel.SetPixel($x, $y, $source.GetPixel($left + $x, $top + $y))
                }
            }
            $panel.Save((Join-Path $targetDirectory $entry.Value), [System.Drawing.Imaging.ImageFormat]::Png)
            Write-Output "$($entry.Value): ${cropWidth}x${cropHeight} at ($left,$top) from $($source.Width)x$($source.Height)"
        } finally { $panel.Dispose() }
    } finally { $source.Dispose() }
}
