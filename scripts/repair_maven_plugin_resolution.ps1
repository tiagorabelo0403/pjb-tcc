$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $root
$targets = @(
    "org\apache\maven\plugins\maven-clean-plugin",
    "org\apache\maven\plugins\maven-compiler-plugin",
    "org\apache\maven\plugins\maven-deploy-plugin",
    "org\apache\maven\plugins\maven-enforcer-plugin",
    "org\apache\maven\plugins\maven-failsafe-plugin",
    "org\apache\maven\plugins\maven-install-plugin",
    "org\apache\maven\plugins\maven-jar-plugin",
    "org\apache\maven\plugins\maven-resources-plugin",
    "org\apache\maven\plugins\maven-site-plugin",
    "org\apache\maven\plugins\maven-surefire-plugin",
    "org\apache\maven\plugins\maven-toolchains-plugin",
    "org\jacoco\jacoco-maven-plugin",
    "org\springframework\boot\spring-boot-maven-plugin"
)
$m2 = Join-Path $env:USERPROFILE ".m2\repository"
foreach ($target in $targets) {
    $path = Join-Path $m2 $target
    if (Test-Path $path) {
        Remove-Item -Recurse -Force $path
    }
}
Get-ChildItem -Path $m2 -Recurse -Filter "*.lastUpdated" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
& .\mvnw.cmd -U -pl pjb-api -am -DskipTests validate -e -DtrimStackTrace=false
