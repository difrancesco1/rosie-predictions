# PowerShell script for running in mock mode
Write-Host "Starting Twitch Predictions Manager in MOCK mode..." -ForegroundColor Green

# Set environment variables
$env:SPRING_PROFILES_ACTIVE = "mock"
$env:APP_MOCK_ENABLED = "true"

# Start the Next.js frontend in a new window
Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd frontend && npm run dev"

# Navigate to backend directory and start Spring Boot with mock profile
Set-Location -Path ".\backend"
mvn spring-boot:run "-Dspring.profiles.active=mock"