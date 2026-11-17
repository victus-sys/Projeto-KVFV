param(
    [string]$FonteArquivo = (Join-Path $PSScriptRoot 'diagrama-de-casos-de-uso.drawio'),
    [string]$DestinoArquivo = (Join-Path $PSScriptRoot 'diagrama-de-casos-de-uso.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
[xml]$arquivo = Get-Content -LiteralPath $FonteArquivo -Raw -Encoding UTF8
$modelo = $arquivo.mxfile.diagram.mxGraphModel
$largura = [int]$modelo.pageWidth
$altura = [int]$modelo.pageHeight
$bitmap = [System.Drawing.Bitmap]::new($largura, $altura)
$grafico = [System.Drawing.Graphics]::FromImage($bitmap)
$grafico.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$grafico.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$grafico.Clear([System.Drawing.Color]::White)

$caneta = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(89, 101, 121), 2)
$borda = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(82, 96, 109), 2)
$fundo = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(234, 243, 251))
$texto = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(31, 41, 55))
$subtexto = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(82, 96, 109))
$labelFont = [System.Drawing.Font]::new('Segoe UI', 15)
$titleFont = [System.Drawing.Font]::new('Segoe UI', 24, [System.Drawing.FontStyle]::Bold)
$actorFont = [System.Drawing.Font]::new('Segoe UI', 14)
$celulas = @{}

function Draw-CenteredText {
    param([System.Drawing.Graphics]$Graphics, [string]$Label, [System.Drawing.Font]$Font,
        [System.Drawing.Brush]$Brush, [System.Drawing.RectangleF]$Box)
    $medida = $Graphics.MeasureString($Label, $Font)
    $x = [single]($Box.X + (($Box.Width - $medida.Width) / 2))
    $y = [single]($Box.Y + (($Box.Height - $medida.Height) / 2))
    $ponto = [System.Drawing.PointF]::new($x, $y)
    $Graphics.DrawString($Label, $Font, $Brush, $ponto)
}

foreach ($celula in $modelo.root.mxCell) {
    if ($celula.vertex -ne '1' -or $null -eq $celula.mxGeometry) { continue }
    $geometria = $celula.mxGeometry
    $celulas[$celula.id] = [pscustomobject]@{
        X = [float]$geometria.x; Y = [float]$geometria.y
        W = [float]$geometria.width; H = [float]$geometria.height
        Label = [string]$celula.value; Style = [string]$celula.style
    }
}

foreach ($celula in $modelo.root.mxCell) {
    if ($celula.edge -ne '1') { continue }
    $origem = $celulas[[string]$celula.source]
    $destinoCelula = $celulas[[string]$celula.target]
    if ($null -eq $origem -or $null -eq $destinoCelula) { continue }
    $origemPelaDireita = $origem.X -gt ($largura / 2)
    if ($origem.Style.Contains('umlActor')) {
        $origemX = if ($origemPelaDireita) { $origem.X } else { $origem.X + $origem.W }
        $origemY = $origem.Y + 80
    } else {
        $origemX = if ($origemPelaDireita) { $origem.X } else { $origem.X + $origem.W }
        $origemY = $origem.Y + ($origem.H / 2)
    }
    if ($destinoCelula.Style.Contains('umlActor')) {
        $destinoX = if ($destinoCelula.X -gt ($largura / 2)) { $destinoCelula.X } else { $destinoCelula.X + $destinoCelula.W }
        $destinoY = $destinoCelula.Y + 80
    } else {
        $destinoX = if ($origemPelaDireita) { $destinoCelula.X + $destinoCelula.W } else { $destinoCelula.X }
        $destinoY = $destinoCelula.Y + ($destinoCelula.H / 2)
    }
    $grafico.DrawLine($caneta, $origemX, $origemY, $destinoX, $destinoY)
}

foreach ($celula in $modelo.root.mxCell) {
    if ($celula.vertex -ne '1' -or $celula.id -in @('0', '1')) { continue }
    $forma = $celulas[[string]$celula.id]
    $retangulo = [System.Drawing.RectangleF]::new($forma.X, $forma.Y, $forma.W, $forma.H)
    if ($forma.Style.Contains('umlActor')) {
        $centroX = $forma.X + ($forma.W / 2)
        $cabeca = [System.Drawing.RectangleF]::new($centroX - 17, $forma.Y + 18, 34, 34)
        $grafico.DrawEllipse($borda, $cabeca)
        $grafico.DrawLine($borda, $centroX, $forma.Y + 52, $centroX, $forma.Y + 102)
        $grafico.DrawLine($borda, $centroX - 27, $forma.Y + 72, $centroX + 27, $forma.Y + 72)
        $grafico.DrawLine($borda, $centroX, $forma.Y + 102, $centroX - 25, $forma.Y + 132)
        $grafico.DrawLine($borda, $centroX, $forma.Y + 102, $centroX + 25, $forma.Y + 132)
        Draw-CenteredText $grafico $forma.Label $actorFont $texto ([System.Drawing.RectangleF]::new($retangulo.X, $forma.Y + 135, $forma.W, 30))
    } elseif ($forma.Style.Contains('ellipse')) {
        $grafico.FillEllipse($fundo, $retangulo)
        $grafico.DrawEllipse($caneta, $retangulo)
        Draw-CenteredText $grafico $forma.Label $labelFont $texto $retangulo
    } elseif ($forma.Style.Contains('swimlane')) {
        $grafico.DrawRectangle($borda, $retangulo.X, $retangulo.Y, $retangulo.Width, $retangulo.Height)
        $grafico.DrawString($forma.Label, $titleFont, $texto, [System.Drawing.PointF]::new(($retangulo.X + 12), ($retangulo.Y + 8)))
    } elseif ($forma.Style.Contains('fontSize=30')) {
        Draw-CenteredText $grafico $forma.Label $titleFont $texto $retangulo
    } else {
        Draw-CenteredText $grafico $forma.Label $actorFont $subtexto $retangulo
    }
}

$bitmap.Save($DestinoArquivo, [System.Drawing.Imaging.ImageFormat]::Png)
$labelFont.Dispose(); $titleFont.Dispose(); $actorFont.Dispose()
$caneta.Dispose(); $borda.Dispose(); $fundo.Dispose(); $texto.Dispose(); $subtexto.Dispose()
$grafico.Dispose(); $bitmap.Dispose()
Write-Output "Diagrama atualizado: $DestinoArquivo"
