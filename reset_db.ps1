<#
	reset_db.ps1
	PowerShell script to reset the DB of TinyHIS without tearing down containers.
	Usage:
		- Run plain: ./reset_db.ps1            # drop and recreate database 'tinyhis'
		- Run with reinit: ./reset_db.ps1 -Reinit  # drop & recreate and re-run docker/init.sql
		- Provide custom root password: ./reset_db.ps1 -MysqlRootPassword "yourpw"
#>

param(
	[string]$MysqlRootPassword = $env:MYSQL_ROOT_PASSWORD,
	[switch]$Reinit
)

if (-not $MysqlRootPassword -or $MysqlRootPassword -eq '') {
	# try to parse .env file at repo root for MYSQL_ROOT_PASSWORD
	$repoEnvFile = Join-Path (Get-Location) '.env'
	if (Test-Path $repoEnvFile) {
		try {
			$envLines = Get-Content $repoEnvFile | Where-Object { $_ -match '=' }
			foreach ($l in $envLines) {
				if ($l -match '^\s*MYSQL_ROOT_PASSWORD\s*=\s*(.*)\s*$') {
					$MysqlRootPassword = $matches[1].Trim('"')
					break
				}
			}
		} catch { }
	}
	if (-not $MysqlRootPassword -or $MysqlRootPassword -eq '') {
		$MysqlRootPassword = 'tinyhis123'
	}
}

Write-Host "MySQL root password: (hidden)" -ForegroundColor Yellow

# Container name from docker-compose.yml
$containerName = 'tinyhis-mysql'

# Helper: ensure the mysql container is running
$running = (& docker ps --filter "name=$containerName" --format "{{.Names}}") -eq $containerName
if (-not $running) {
	Write-Host "MySQL container ($containerName) is not running. Starting the container..." -ForegroundColor Yellow
	& docker compose up -d mysql
	# Wait for the container to be healthy
	Write-Host "Waiting for MySQL to be ready..." -ForegroundColor Yellow
	$maxWait = 60
	for ($i = 0; $i -lt $maxWait; $i++) {
		Start-Sleep -Seconds 2
		try {
			& docker exec -i $containerName mysql -uroot -p$MysqlRootPassword -e "SELECT 1;" > $null 2>&1
			if ($LASTEXITCODE -eq 0) { break }
		} catch { }
	}
}

# Drop and recreate the database
Write-Host "Dropping and recreating database 'tinyhis'..." -ForegroundColor Cyan
Function ExecMysql($pwd, $sql) {
	# Try running with MYSQL_PWD env for less noisy CLI warning
	& docker exec -i --env MYSQL_PWD=$pwd $containerName mysql -uroot -e $sql
}

# Try a list of password candidates (returns the one that works or $null)
Function TryPasswords($sql, [string[]]$candidates) {
	foreach ($p in $candidates) {
		if ($null -eq $p) { $p = '' }
		$label = if ($p -and $p -ne '') { '(non-empty)' } else { '(empty)' }
		Write-Host "Trying mysql with password: $label" -ForegroundColor Gray
		ExecMysql $p $sql
		if ($LASTEXITCODE -eq 0) {
			return $p
		}
	}
	return $null
}

$dropCreateSql = "DROP DATABASE IF EXISTS tinyhis; CREATE DATABASE tinyhis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
# Build candidate password list: provided root, default, empty
$candidatePasswords = @($MysqlRootPassword, 'tinyhis123', '')
$ExecResult = TryPasswords $dropCreateSql $candidatePasswords
if ($ExecResult -ne $null) {
	$UsedRootPassword = $ExecResult
} else {
	$UsedRootPassword = $null
}

	# If still not found, prompt interactively as last resort
	if (-not $UsedRootPassword) {
		if ($Host.UI.SupportsVirtualTerminal) {
			try {
				$secure = Read-Host "Enter root password to try (input hidden, press Enter to skip)" -AsSecureString
				$unsecure = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
				if ($unsecure -and $unsecure -ne '') {
					Write-Host "Trying with interactive password..." -ForegroundColor Gray
					$ExecResult = TryPasswords $dropCreateSql @($unsecure)
					if ($ExecResult -ne $null) { $UsedRootPassword = $ExecResult }
				}
			} catch { }
		}
	}
	Write-Host "Drop/Create with provided password failed, attempting to use the container's MYSQL_ROOT_PASSWORD if different..." -ForegroundColor Yellow
	# Attempt to read MYSQL_ROOT_PASSWORD from container environment
	try {
		$envList = & docker inspect --format='{{range .Config.Env}}{{println .}}{{end}}' $containerName
		$containerPwdLine = $envList | Select-String -Pattern '^MYSQL_ROOT_PASSWORD=' -SimpleMatch
		if ($containerPwdLine) {
			$containerPwd = $containerPwdLine -replace '^MYSQL_ROOT_PASSWORD=', ''
			if ($containerPwd -and $containerPwd -ne $MysqlRootPassword) {
				Write-Host "Found container MYSQL_ROOT_PASSWORD via inspect; trying with that password..." -ForegroundColor Yellow
				$ExecResult = TryPasswords $dropCreateSql @($containerPwd)
				if ($ExecResult -ne $null) {
					$UsedRootPassword = $ExecResult
				}
			}
		}
	} catch {
		Write-Host "Failed to read container env: $_" -ForegroundColor Yellow
	}

	if (-not $UsedRootPassword) {
		Write-Host "Failed to execute drop/create SQL. Ensure the container is running and the root password is correct." -ForegroundColor Red
		Write-Host "Helpful debug steps:" -ForegroundColor Yellow
		Write-Host "  1) Check container env: docker inspect --format='{{range .Config.Env}}{{println .}}{{end}}' $containerName" -ForegroundColor Yellow
		Write-Host "  2) Try logging in interactively: docker exec -it $containerName mysql -uroot -p" -ForegroundColor Yellow
		Write-Host "  3) Check container logs: docker logs --tail 50 $containerName | Select-String -Pattern 'temporary password'" -ForegroundColor Yellow
		Write-Host "  4) If you cannot recover password, remove mysql volume (data loss): docker compose down -v" -ForegroundColor Yellow
		exit 1
	}

# Optionally, re-run init.sql to recreate schema and seed data
if ($Reinit) {
	$initSqlPath = "docker/init.sql"
	if (-not (Test-Path $initSqlPath)) {
		Write-Host "Initialization SQL file not found: $initSqlPath" -ForegroundColor Red
		exit 1
	}
	Write-Host "Re-initializing schema from $initSqlPath..." -ForegroundColor Cyan
	if (-not $UsedRootPassword) {
		Write-Host "No valid root password was discovered; cannot run initialization SQL without a valid root password" -ForegroundColor Red
		exit 1
	}
	# Copy init.sql to container to avoid PowerShell encoding issues
	& docker cp $initSqlPath "$($containerName):/tmp/init.sql"
	
	# Run the SQL script inside the container
	& docker exec -i --env MYSQL_PWD=$UsedRootPassword $containerName mysql -uroot --default-character-set=utf8mb4 tinyhis -e "source /tmp/init.sql"
	$sqlResult = $LASTEXITCODE
	
	# Cleanup
	& docker exec -i $containerName rm /tmp/init.sql
	
	if ($sqlResult -ne 0) {
		Write-Host "Failed to run initialization SQL." -ForegroundColor Red
		exit 1
	}
}

Write-Host "Database reset completed successfully." -ForegroundColor Green

# Useful next steps
Write-Host "Hint: If your backend uses cached data or the app needs restarting, use 'docker compose restart backend' or restart the specific service." -ForegroundColor Yellow
