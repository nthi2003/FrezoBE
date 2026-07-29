# Sync EU guide markdown FE → BE classpath (GuideDataInitializer).
# Usage (from repo root FrezoBE or anywhere):
#   powershell -File module-qtht-bom/scripts/sync-guides-from-fe.ps1
# Then restart BE — seed upserts published rows owned by guide-seed/system.

$ErrorActionPreference = "Stop"
# scripts → module-qtht-bom → FrezoBE
$beRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$feDocs = Join-Path $beRoot "..\FrezoFE\packages\erp\src\docs"
$beGuides = Join-Path $PSScriptRoot "..\src\main\resources\guides"

if (-not (Test-Path $feDocs)) {
    throw "FE docs not found: $feDocs (expect FrezoFE sibling of FrezoBE)"
}

New-Item -ItemType Directory -Force -Path $beGuides | Out-Null

$files = @(
    "getting-started.md", "menu-guide.md",
    "guide-payroll.md", "guide-leave.md", "guide-approval-inbox.md",
    "guide-attendance-settings.md", "guide-hire.md",
    "guide-customers.md", "guide-deals.md",
    "guide-workflows.md", "guide-approval-flows.md", "guide-approval-attach.md",
    "guide-warehouse-sales.md",
    "guide-qlts.md", "guide-asset-assign.md", "guide-depreciation.md",
    "guide-articles.md",
    "guide-mobile.md", "guide-mobile-attendance.md", "guide-mobile-leave.md", "guide-mobile-payslip.md",
    "guide-accounting.md", "guide-accounting-setup.md", "guide-accounting-journal.md",
    "guide-accounting-bank.md", "guide-accounting-reports.md",
    "guide-warehouse-reorder-rules.md",
    "guide-warehouse-grn-gin.md",
    "guide-warehouse-stock-takes.md"
)

$copied = 0
foreach ($f in $files) {
    $src = Join-Path $feDocs $f
    if (-not (Test-Path $src)) {
        Write-Warning "Missing FE file: $f"
        continue
    }
    Copy-Item -Force $src (Join-Path $beGuides $f)
    $copied++
}

Write-Host "Synced $copied/$($files.Count) guides → $beGuides"
Write-Host "Next: restart BE (look for [guide-seed] done in logs)."
