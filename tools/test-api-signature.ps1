param(
    [string]$JavaHome = "",
    [string]$ReportPath = "build/reports/api/api-signature-fixture.txt"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $JavaHome = $env:JAVA_HOME
}
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    throw "JAVA_HOME is required for the API signature fixture"
}
$JavaHome = $JavaHome.Trim().Trim('"').TrimEnd('\', '/')
$javac = Join-Path $JavaHome "bin\javac.exe"
$jarTool = Join-Path $JavaHome "bin\jar.exe"
if (!(Test-Path -LiteralPath $javac -PathType Leaf)) {
    $javac = Join-Path $JavaHome "bin\javac"
}
if (!(Test-Path -LiteralPath $jarTool -PathType Leaf)) {
    $jarTool = Join-Path $JavaHome "bin\jar"
}
if ((!(Test-Path -LiteralPath $javac -PathType Leaf) -or
     !(Test-Path -LiteralPath $jarTool -PathType Leaf)) -and
    (Split-Path -Leaf $JavaHome) -ieq "jre") {
    $JavaHome = Split-Path -Parent $JavaHome
    $javac = Join-Path $JavaHome "bin\javac.exe"
    $jarTool = Join-Path $JavaHome "bin\jar.exe"
    if (!(Test-Path -LiteralPath $javac -PathType Leaf)) {
        $javac = Join-Path $JavaHome "bin\javac"
    }
    if (!(Test-Path -LiteralPath $jarTool -PathType Leaf)) {
        $jarTool = Join-Path $JavaHome "bin\jar"
    }
}
if (!(Test-Path -LiteralPath $javac -PathType Leaf) -or
    !(Test-Path -LiteralPath $jarTool -PathType Leaf)) {
    throw "Could not find javac and jar under $JavaHome"
}

$fixtureRoot = Join-Path $repoRoot "build\api-signature-fixture"
$resolvedFixtureRoot = [IO.Path]::GetFullPath($fixtureRoot)
$resolvedBuildRoot = [IO.Path]::GetFullPath((Join-Path $repoRoot "build"))
if (!$resolvedFixtureRoot.StartsWith($resolvedBuildRoot + [IO.Path]::DirectorySeparatorChar,
        [StringComparison]::OrdinalIgnoreCase)) {
    throw "API signature fixture path escaped the repository build directory: $resolvedFixtureRoot"
}
if (Test-Path -LiteralPath $fixtureRoot) {
    Remove-Item -LiteralPath $fixtureRoot -Recurse -Force
}
$null = New-Item -ItemType Directory -Path $fixtureRoot -Force

function Write-Source([string]$Root, [string]$RelativePath, [string]$Content) {
    $path = Join-Path $Root $RelativePath
    $directory = Split-Path -Parent $path
    $null = New-Item -ItemType Directory -Path $directory -Force
    [IO.File]::WriteAllText($path, $Content, [Text.UTF8Encoding]::new($false))
}

function Build-FixtureJar([string]$Name, [hashtable]$Sources) {
    $root = Join-Path $fixtureRoot $Name
    $sourceRoot = Join-Path $root "src"
    $classes = Join-Path $root "classes"
    $jarPath = Join-Path $root "$Name.jar"
    $null = New-Item -ItemType Directory -Path $classes -Force
    foreach ($entry in $Sources.GetEnumerator()) {
        Write-Source $sourceRoot $entry.Key $entry.Value
    }
    $sourceFiles = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter "*.java" -File |
        ForEach-Object { $_.FullName })
    & $javac -source 8 -target 8 -d $classes $sourceFiles
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed for API fixture $Name"
    }
    & $jarTool cf $jarPath -C $classes .
    if ($LASTEXITCODE -ne 0) {
        throw "jar failed for API fixture $Name"
    }
    return $jarPath
}

function Invoke-Signature([string]$Name, [string]$JarPath, [string]$Baseline = "") {
    $output = Join-Path $fixtureRoot "$Name.sig"
    $arguments = @(
        "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $repoRoot "tools\api-signature.ps1"),
        "-JarPath", $JarPath,
        "-OutputPath", $output,
        "-JavaHome", $JavaHome
    )
    if (![string]::IsNullOrWhiteSpace($Baseline)) {
        $arguments += @("-CompatibilityBaseline", $Baseline)
    }
    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & powershell.exe @arguments *> (Join-Path $fixtureRoot "$Name.log")
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorAction
    return @{
        ExitCode = $exitCode
        Output = $output
        Log = Join-Path $fixtureRoot "$Name.log"
    }
}

function Expect-Pass([string]$Name, [hashtable]$Result) {
    if ($Result.ExitCode -ne 0) {
        throw "Expected $Name to pass. See $($Result.Log)"
    }
}

function Expect-Fail([string]$Name, [hashtable]$Result) {
    if ($Result.ExitCode -eq 0) {
        throw "Expected $Name to fail compatibility validation"
    }
}

$baselineSources = @{
    "fixture/ExistingInterface.java" = @'
package fixture;
public interface ExistingInterface {
    void required();
    default void keptDefault() {}
    static void keptStatic() {}
}
'@
    "fixture/ExistingType.java" = @'
package fixture;
public class ExistingType {
    public static final String VERSION = "1";
    protected int field;
    public ExistingType() {}
    public void method(String value) {}
}
'@
}

$baselineJar = Build-FixtureJar "baseline" $baselineSources
$baselineResult = Invoke-Signature "baseline" $baselineJar
Expect-Pass "baseline generation" $baselineResult
$baselineSignature = $baselineResult.Output

$compatibleSources = @{
    "fixture/ExistingInterface.java" = @'
package fixture;
public interface ExistingInterface {
    void required();
    default void keptDefault() {}
    default void addedDefault() {}
    static void keptStatic() {}
    static void addedStatic() {}
}
'@
    "fixture/ExistingType.java" = @'
package fixture;
public class ExistingType {
    public static final String VERSION = "2";
    protected int field;
    public ExistingType() {}
    public void method(String value) {}
    public void method(int value) {}
}
'@
    "fixture/AddedType.java" = @'
package fixture;
public final class AddedType {
    public void added() {}
}
'@
}
Expect-Pass "compatible additions" (Invoke-Signature "compatible" `
    (Build-FixtureJar "compatible" $compatibleSources) $baselineSignature)

$removedSources = $baselineSources.Clone()
$removedSources["fixture/ExistingType.java"] = @'
package fixture;
public class ExistingType {
    public static final String VERSION = "2";
    protected int field;
    public ExistingType() {}
}
'@
Expect-Fail "removed member" (Invoke-Signature "removed" `
    (Build-FixtureJar "removed" $removedSources) $baselineSignature)

$descriptorSources = $baselineSources.Clone()
$descriptorSources["fixture/ExistingType.java"] = @'
package fixture;
public class ExistingType {
    public static final String VERSION = "2";
    protected long field;
    public ExistingType() {}
    public void method(String value) {}
}
'@
Expect-Fail "changed descriptor" (Invoke-Signature "descriptor" `
    (Build-FixtureJar "descriptor" $descriptorSources) $baselineSignature)

$inheritanceSources = $baselineSources.Clone()
$inheritanceSources["fixture/ExistingType.java"] = @'
package fixture;
public class ExistingType extends java.util.ArrayList<String> {
    public static final String VERSION = "2";
    protected int field;
    public ExistingType() {}
    public void method(String value) {}
}
'@
Expect-Fail "changed inheritance" (Invoke-Signature "inheritance" `
    (Build-FixtureJar "inheritance" $inheritanceSources) $baselineSignature)

$abstractSources = $baselineSources.Clone()
$abstractSources["fixture/ExistingInterface.java"] = @'
package fixture;
public interface ExistingInterface {
    void required();
    void addedAbstract();
    default void keptDefault() {}
    static void keptStatic() {}
}
'@
Expect-Fail "added abstract interface member" (Invoke-Signature "abstract" `
    (Build-FixtureJar "abstract" $abstractSources) $baselineSignature)

$report = [IO.Path]::GetFullPath((Join-Path $repoRoot $ReportPath))
$reportDirectory = Split-Path -Parent $report
$null = New-Item -ItemType Directory -Path $reportDirectory -Force
[IO.File]::WriteAllLines($report, @(
    "Ponder API signature fixture: PASS",
    "Java home: $JavaHome",
    "Compatible: added type, overload, default/static interface methods, changed constant value",
    "Rejected: removed member, changed descriptor, changed inheritance, added abstract interface method"
), [Text.UTF8Encoding]::new($false))
Write-Host "API signature fixture passed. Report: $report"
