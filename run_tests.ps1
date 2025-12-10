
Write-Host "Starting Backend Tests in Docker..."
docker compose -f docker-compose.test.yml up test-backend --exit-code-from test-backend
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Some tests failed or execution error."
}

$reportFile = "report/verification.tex"
$testsDir = "target/surefire-reports"

# Check if report directory exists
if (-not (Test-Path "report")) {
    New-Item -ItemType Directory -Force -Path "report"
}

# Create/Overwrite report file
"\\subsection{自动化单元与集成测试}" | Out-File -FilePath $reportFile -Encoding utf8
"为了确保系统的稳定性与核心功能的正确性，我们基于 JUnit 5 与 Spring Boot Test 构建了自动化测试套件，覆盖了医学知识库管理、AI 分诊逻辑以及完整的患者就诊流程（挂号-分诊-接诊）。" | Out-File -FilePath $reportFile -Append -Encoding utf8
"本次测试运行于隔离的 Docker 容器环境 (Maven 3.9, OpenJDK 21)，确保了测试环境的一致性与可复现性。" | Out-File -FilePath $reportFile -Append -Encoding utf8

"\\begin{table}[H]" | Out-File -FilePath $reportFile -Append -Encoding utf8
"\\centering" | Out-File -FilePath $reportFile -Append -Encoding utf8
"\\caption{单元与集成测试执行结果}" | Out-File -FilePath $reportFile -Append -Encoding utf8
"\\begin{tabular}{|l|p{6cm}|c|c|}" | Out-File -FilePath $reportFile -Append -Encoding utf8
"\\hline" | Out-File -FilePath $reportFile -Append -Encoding utf8
"\\textbf{测试类} & \\textbf{测试用例} & \\textbf{状态} & \\textbf{耗时(s)} \\\\" | Out-File -FilePath $reportFile -Append -Encoding utf8
"\\hline" | Out-File -FilePath $reportFile -Append -Encoding utf8

$hasResults = $false
if (Test-Path $testsDir) {
    Get-ChildItem -Path $testsDir -Filter "TEST-*.xml" | ForEach-Object {
        try {
            [xml]$xml = Get-Content $_.FullName
            $xml.testsuite.testcase | ForEach-Object {
                $hasResults = $true
                $className = $_.classname -replace ".*\.", ""
                $testName = $_.name -replace "_", "\_" # Escape underscores for LaTeX
                
                # Truncate long class names for display if needed
                if ($className.Length -gt 25) { $className = $className.Substring(0, 22) + "..." }

                $time = $_.time
                $status = "通过"
                if ($_.failure) { 
                    $status = "\\textcolor{red}{失败}" 
                } elseif ($_.error) {
                    $status = "\\textcolor{red}{错误}"
                }
                
                "$className & $testName & $status & $time \\\\" | Out-File -FilePath $reportFile -Append -Encoding utf8
                "\\hline" | Out-File -FilePath $reportFile -Append -Encoding utf8
            }
        } catch {
            Write-Warning "Error parsing $($_.Name)"
        }
    }
}

if (-not $hasResults) {
    "N/A & 未找到测试报告 (请检查Maven运行状态) & - & - \\\\" | Out-File -FilePath $reportFile -Append -Encoding utf8
    "\\hline" | Out-File -FilePath $reportFile -Append -Encoding utf8
}

"\\end{tabular}" | Out-File -FilePath $reportFile -Append -Encoding utf8
"\\label{tab:test_results}" | Out-File -FilePath $reportFile -Append -Encoding utf8
"\\end{table}" | Out-File -FilePath $reportFile -Append -Encoding utf8

Write-Host "Verification report generated at $reportFile"
