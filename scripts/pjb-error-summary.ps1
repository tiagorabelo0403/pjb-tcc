param(
    [Parameter(Mandatory = $true)]
    [string]$LogPath,
    [int]$Context = 6
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (!(Test-Path $LogPath)) {
    throw "Log não encontrado: $LogPath"
}

$patterns = @(
    '^\[ERROR\]',
    'COMPILATION ERROR',
    'BUILD FAILURE',
    '<<< FAILURE!',
    '<<< ERROR!',
    '^Failures:',
    '^Errors:',
    'Caused by:',
    'AssertionFailedError',
    'NullPointerException',
    'NoSuchBeanDefinitionException',
    'UnsatisfiedDependencyException',
    'UnnecessaryStubbingException',
    'InvalidUseOfMatchersException',
    'Failed to load ApplicationContext',
    'Unknown data type',
    'Mockito is currently self-attaching',
    'NativeCommandError',
    'Table ".*" not found',
    'Cannot invoke',
    'expected: <',
    'but was:'
)

Select-String -Path $LogPath -Pattern $patterns -Context 0, $Context
