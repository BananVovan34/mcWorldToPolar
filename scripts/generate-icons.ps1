$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$pngPath = Join-Path $root "src/main/resources/icons/app.png"
$icoPath = Join-Path $root "packaging/windows/mc-world-to-polar.ico"

New-Item -ItemType Directory -Force (Split-Path -Parent $pngPath) | Out-Null
New-Item -ItemType Directory -Force (Split-Path -Parent $icoPath) | Out-Null

function New-RoundedRectanglePath {
    param(
        [System.Drawing.RectangleF]$Rectangle,
        [float]$Radius
    )

    $diameter = $Radius * 2
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddArc($Rectangle.Left, $Rectangle.Top, $diameter, $diameter, 180, 90)
    $path.AddArc($Rectangle.Right - $diameter, $Rectangle.Top, $diameter, $diameter, 270, 90)
    $path.AddArc($Rectangle.Right - $diameter, $Rectangle.Bottom - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($Rectangle.Left, $Rectangle.Bottom - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-AppIconBitmap {
    param([int]$Size)

    $scale = $Size / 256.0
    $bitmap = [System.Drawing.Bitmap]::new(
        $Size,
        $Size,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.Clear([System.Drawing.Color]::Transparent)

    $backgroundRectangle = [System.Drawing.RectangleF]::new(
        8 * $scale,
        8 * $scale,
        240 * $scale,
        240 * $scale
    )
    $backgroundPath = New-RoundedRectanglePath -Rectangle $backgroundRectangle -Radius (42 * $scale)
    $backgroundBrush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        [System.Drawing.PointF]::new(20 * $scale, 20 * $scale),
        [System.Drawing.PointF]::new(236 * $scale, 236 * $scale),
        [System.Drawing.Color]::FromArgb(255, 24, 30, 48),
        [System.Drawing.Color]::FromArgb(255, 52, 27, 72)
    )
    $graphics.FillPath($backgroundBrush, $backgroundPath)

    $cyanPen = [System.Drawing.Pen]::new(
        [System.Drawing.Color]::FromArgb(220, 62, 220, 229),
        [Math]::Max(1.0, 6 * $scale)
    )
    $cyanPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $cyanPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $graphics.DrawArc($cyanPen, 30 * $scale, 48 * $scale, 196 * $scale, 150 * $scale, 205, 285)
    $graphics.DrawArc($cyanPen, 52 * $scale, 70 * $scale, 152 * $scale, 112 * $scale, 210, 270)

    $axisPen = [System.Drawing.Pen]::new(
        [System.Drawing.Color]::FromArgb(150, 170, 126, 255),
        [Math]::Max(1.0, 3 * $scale)
    )
    $graphics.DrawLine($axisPen, 128 * $scale, 32 * $scale, 128 * $scale, 224 * $scale)
    $graphics.DrawLine($axisPen, 35 * $scale, 151 * $scale, 221 * $scale, 151 * $scale)

    $outlinePen = [System.Drawing.Pen]::new(
        [System.Drawing.Color]::FromArgb(255, 226, 236, 255),
        [Math]::Max(1.0, 4 * $scale)
    )
    $topBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 108, 226, 214))
    $leftBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 73, 119, 181))
    $rightBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 113, 73, 171))

    $top = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new(128 * $scale, 60 * $scale),
        [System.Drawing.PointF]::new(191 * $scale, 95 * $scale),
        [System.Drawing.PointF]::new(128 * $scale, 130 * $scale),
        [System.Drawing.PointF]::new(65 * $scale, 95 * $scale)
    )
    $left = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new(65 * $scale, 95 * $scale),
        [System.Drawing.PointF]::new(128 * $scale, 130 * $scale),
        [System.Drawing.PointF]::new(128 * $scale, 204 * $scale),
        [System.Drawing.PointF]::new(65 * $scale, 169 * $scale)
    )
    $right = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new(128 * $scale, 130 * $scale),
        [System.Drawing.PointF]::new(191 * $scale, 95 * $scale),
        [System.Drawing.PointF]::new(191 * $scale, 169 * $scale),
        [System.Drawing.PointF]::new(128 * $scale, 204 * $scale)
    )

    $graphics.FillPolygon($topBrush, $top)
    $graphics.FillPolygon($leftBrush, $left)
    $graphics.FillPolygon($rightBrush, $right)
    $graphics.DrawPolygon($outlinePen, $top)
    $graphics.DrawPolygon($outlinePen, $left)
    $graphics.DrawPolygon($outlinePen, $right)

    $highlightPen = [System.Drawing.Pen]::new(
        [System.Drawing.Color]::FromArgb(210, 255, 255, 255),
        [Math]::Max(1.0, 2 * $scale)
    )
    $graphics.DrawLine($highlightPen, 128 * $scale, 72 * $scale, 174 * $scale, 97 * $scale)
    $graphics.DrawLine($highlightPen, 82 * $scale, 98 * $scale, 128 * $scale, 123 * $scale)

    $highlightPen.Dispose()
    $rightBrush.Dispose()
    $leftBrush.Dispose()
    $topBrush.Dispose()
    $outlinePen.Dispose()
    $axisPen.Dispose()
    $cyanPen.Dispose()
    $backgroundBrush.Dispose()
    $backgroundPath.Dispose()
    $graphics.Dispose()

    return $bitmap
}

$sizes = @(16, 24, 32, 48, 64, 128, 256)
$images = @()

foreach ($size in $sizes) {
    $bitmap = New-AppIconBitmap $size
    $stream = [System.IO.MemoryStream]::new()
    $bitmap.Save($stream, [System.Drawing.Imaging.ImageFormat]::Png)
    $bytes = $stream.ToArray()
    $images += [PSCustomObject]@{ Size = $size; Bytes = $bytes }

    if ($size -eq 256) {
        [System.IO.File]::WriteAllBytes($pngPath, $bytes)
    }

    $stream.Dispose()
    $bitmap.Dispose()
}

$fileStream = [System.IO.File]::Create($icoPath)
$writer = [System.IO.BinaryWriter]::new($fileStream)
$writer.Write([UInt16]0)
$writer.Write([UInt16]1)
$writer.Write([UInt16]$images.Count)

$offset = 6 + (16 * $images.Count)
foreach ($image in $images) {
    $dimension = if ($image.Size -eq 256) { 0 } else { $image.Size }
    $writer.Write([Byte]$dimension)
    $writer.Write([Byte]$dimension)
    $writer.Write([Byte]0)
    $writer.Write([Byte]0)
    $writer.Write([UInt16]1)
    $writer.Write([UInt16]32)
    $writer.Write([UInt32]$image.Bytes.Length)
    $writer.Write([UInt32]$offset)
    $offset += $image.Bytes.Length
}

foreach ($image in $images) {
    $writer.Write($image.Bytes)
}

$writer.Dispose()
$fileStream.Dispose()

Write-Host "Generated $pngPath"
Write-Host "Generated $icoPath"
