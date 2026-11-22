param(
    [string]$Gateway = "http://localhost:8080",
    [string]$Knowledge = "http://localhost:8087",
    [string]$Email = "admin.local@test.com",
    [string]$Password = "Password123!"
)
$ErrorActionPreference = "Stop"

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$cases = Get-Content (Join-Path $here "questions.json") -Raw -Encoding UTF8 | ConvertFrom-Json

$login = @{ email = $Email; password = $Password } | ConvertTo-Json
$token = (Invoke-RestMethod "$Gateway/api/auth/login" -Method Post -ContentType "application/json" -Body $login).accessToken
$headers = @{ Authorization = "Bearer $token" }

$results = foreach ($case in $cases) {
    $json = @{ question = $case.question } | ConvertTo-Json
    $reply = Invoke-RestMethod "$Knowledge/api/docs/ask" -Method Post -Headers $headers `
        -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($json))
    $sources = @($reply.sources)
    $answerable = $case.answerable -ne $false

    if ($answerable) {
        $sourceHit = @($sources | Where-Object {
            $_.fileName -eq $case.file -and ("$($_.pageNumber)" -eq $case.location -or $_.heading -eq $case.location)
        }).Count -gt 0
        $correct = $reply.grounded -and ($reply.answer -match $case.pattern)
    } else {
        $sourceHit = $null
        $correct = -not $reply.grounded
    }

    [pscustomobject]@{
        Question   = $case.question
        Answerable = $answerable
        SourceHit  = $sourceHit
        Correct    = $correct
        TopScore   = if ($sources.Count -gt 0) { ($sources | Measure-Object score -Maximum).Maximum } else { $null }
        Answer     = $reply.answer
    }
}

$results | Format-Table Question, Answerable, SourceHit, Correct, TopScore -AutoSize

$answerable = @($results | Where-Object Answerable)
$unanswerable = @($results | Where-Object { -not $_.Answerable })
function Pct($part, $whole) { if ($whole -eq 0) { "n/a" } else { "{0}/{1} ({2:P0})" -f $part, $whole, ($part / $whole) } }

""
"Retrieval hit rate : " + (Pct @($answerable | Where-Object SourceHit).Count $answerable.Count)
"Answer accuracy    : " + (Pct @($answerable | Where-Object Correct).Count $answerable.Count)
"Refusal accuracy   : " + (Pct @($unanswerable | Where-Object Correct).Count $unanswerable.Count)

$wrong = @($results | Where-Object { -not $_.Correct })
if ($wrong.Count -gt 0) {
    ""
    "Needs a look:"
    $wrong | ForEach-Object { " - $($_.Question)`n   -> $($_.Answer)" }
}