[CmdletBinding()]
param(
    [string]$Region = 'ap-south-1',
    [string]$StackName = 'acme-payroll-prod',
    [string]$ApplicationName = 'acme-payroll',
    [string]$EnvironmentName = 'acme-payroll-prod',
    [string]$InstanceType = 't3.micro',
    [string]$DbInstanceClass = 'db.t4g.micro',
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$SkipSmokeTest
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$template = Join-Path $root 'infra\aws-elastic-beanstalk.yaml'
$jar = Join-Path $root 'backend\target\payroll-service-0.0.1-SNAPSHOT.jar'
$statePath = Join-Path $root '.aws-deploy.local.json'

function Assert-AwsSuccess([string]$message) {
    if ($LASTEXITCODE -ne 0) {
        throw $message
    }
}

function New-DbPassword {
    $chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789-_'
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    $value = -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
    if ($value -notmatch '[A-Z]' -or $value -notmatch '[a-z]' -or $value -notmatch '[0-9]') {
        $value = "Aa1$value"
    }
    return $value
}

Write-Output "AWS deploy: $ApplicationName / $EnvironmentName in $Region"

$identityJson = aws sts get-caller-identity --region $Region --output json 2>$null
Assert-AwsSuccess 'AWS CLI is not authenticated. Run aws sso login or aws configure first.'
$identity = $identityJson | ConvertFrom-Json
$account = $identity.Account
Write-Output "Account: $account"

if (-not $SkipBuild) {
    Write-Output 'Building the single production jar...'
    if ($SkipInstall) {
        & (Join-Path $PSScriptRoot 'build-all.ps1') -SkipInstall
    }
    else {
        & (Join-Path $PSScriptRoot 'build-all.ps1')
    }
    if ($LASTEXITCODE -ne 0) {
        throw 'Production build failed. If ng serve is running, rerun with -SkipInstall.'
    }
}

if (-not (Test-Path -LiteralPath $jar)) {
    throw "Jar not found: $jar. Run without -SkipBuild."
}

$state = $null
if (Test-Path -LiteralPath $statePath) {
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
}

$stackStatusJson = aws cloudformation describe-stacks --stack-name $StackName --region $Region --query 'Stacks[0].StackStatus' --output text 2>$null
$stackExists = $LASTEXITCODE -eq 0
$stackStatus = if ($stackExists) { $stackStatusJson.Trim() } else { '' }
if ($stackExists -and -not $state) {
    throw "Stack $StackName already exists but $statePath is missing. Recover the database password or use a new stack name."
}
if ($stackExists -and $stackStatus -match 'IN_PROGRESS') {
    throw "Stack $StackName is still $stackStatus. Wait for it to finish before deploying."
}

if ($state -and ($state.stackName -ne $StackName -or $state.region -ne $Region)) {
    throw "Local state belongs to $($state.stackName) in $($state.region). Use a matching stack/region or move $statePath aside."
}

$dbPassword = if ($state) { $state.dbPassword } else { New-DbPassword }

$vpcId = aws ec2 describe-vpcs --region $Region --filters 'Name=is-default,Values=true' --query 'Vpcs[0].VpcId' --output text
Assert-AwsSuccess "No default VPC found in $Region."
if ([string]::IsNullOrWhiteSpace($vpcId) -or $vpcId -eq 'None') {
    throw "No default VPC found in $Region. Create one or adapt the template to an existing VPC."
}

$subnetJson = aws ec2 describe-subnets --region $Region --filters "Name=vpc-id,Values=$vpcId" --output json
Assert-AwsSuccess 'Unable to list subnets in the default VPC.'
$subnets = @(($subnetJson | ConvertFrom-Json).Subnets)
if ($subnets.Count -lt 2) {
    throw 'The default VPC must contain at least two subnets in different Availability Zones.'
}

$publicSubnets = @($subnets | Where-Object { $_.MapPublicIpOnLaunch -eq $true })
$appSubnet = if ($publicSubnets.Count -gt 0) { $publicSubnets[0].SubnetId } else { $subnets[0].SubnetId }

$dbSubnetIds = @()
$seenAz = @{}
foreach ($subnet in $subnets) {
    if (-not $seenAz.ContainsKey($subnet.AvailabilityZone)) {
        $dbSubnetIds += $subnet.SubnetId
        $seenAz[$subnet.AvailabilityZone] = $true
    }
    if ($dbSubnetIds.Count -eq 2) {
        break
    }
}
if ($dbSubnetIds.Count -lt 2) {
    throw 'RDS needs two subnets in different Availability Zones.'
}

$platformArn = aws elasticbeanstalk list-platform-versions --region $Region `
    --query "PlatformSummaryList[?PlatformName=='Java' && contains(PlatformBranchName, 'Corretto 21') && Status=='Ready'] | sort_by(@, &DateCreated) | [-1].PlatformArn" `
    --output text
Assert-AwsSuccess 'Unable to list Elastic Beanstalk platform versions.'
if ([string]::IsNullOrWhiteSpace($platformArn) -or $platformArn -eq 'None') {
    $platformArn = aws elasticbeanstalk list-platform-versions --region $Region `
        --query "PlatformSummaryList[?contains(PlatformBranchName, 'Corretto 21') && Status=='Ready'] | sort_by(@, &DateCreated) | [-1].PlatformArn" `
        --output text
}
if ([string]::IsNullOrWhiteSpace($platformArn) -or $platformArn -eq 'None') {
    throw 'No ready Elastic Beanstalk Corretto 21 platform found in this region.'
}
Write-Output "Platform: $platformArn"

$dbEngineVersion = aws rds describe-db-engine-versions --region $Region --engine postgres `
    --query "DBEngineVersions[?starts_with(DBEngineVersion, '17.') && Status=='available'] | sort_by(@, &DBEngineVersion) | [-1].DBEngineVersion" `
    --output text
Assert-AwsSuccess 'Unable to list RDS PostgreSQL versions.'
if ([string]::IsNullOrWhiteSpace($dbEngineVersion) -or $dbEngineVersion -eq 'None') {
    $dbEngineVersion = '17'
}
Write-Output "PostgreSQL: $dbEngineVersion"

$bucket = "acme-payroll-src-$account-$Region".ToLowerInvariant()
aws s3api head-bucket --bucket $bucket --region $Region 2>$null
if ($LASTEXITCODE -ne 0) {
    if ($Region -eq 'us-east-1') {
        aws s3api create-bucket --bucket $bucket --region $Region
    }
    else {
        aws s3api create-bucket --bucket $bucket --region $Region --create-bucket-configuration "LocationConstraint=$Region"
    }
    Assert-AwsSuccess "Unable to create source bucket $bucket."
}

aws s3api put-public-access-block --bucket $bucket --region $Region `
    --public-access-block-configuration 'BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true'
Assert-AwsSuccess "Unable to block public access on $bucket."

aws s3api put-bucket-encryption --bucket $bucket --region $Region `
    --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
Assert-AwsSuccess "Unable to enable encryption on $bucket."

$versionLabel = 'v' + (Get-Date -Format 'yyyyMMddHHmmss')
$sourceKey = "payroll-service-$versionLabel.jar"
aws s3 cp $jar "s3://$bucket/$sourceKey" --region $Region --only-show-errors
Assert-AwsSuccess "Unable to upload the application jar to $bucket."

$state = [ordered]@{
    stackName = $StackName
    region = $Region
    applicationName = $ApplicationName
    environmentName = $EnvironmentName
    dbPassword = $dbPassword
    sourceBucket = $bucket
    sourceKey = $sourceKey
    versionLabel = $versionLabel
    deployedAt = (Get-Date).ToString('o')
}
$state | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding UTF8
Write-Output "Uploaded s3://$bucket/$sourceKey"

$parameters = @(
    "ApplicationName=$ApplicationName",
    "EnvironmentName=$EnvironmentName",
    "SourceBucket=$bucket",
    "SourceKey=$sourceKey",
    "PlatformArn=$platformArn",
    "InstanceType=$InstanceType",
    "VpcId=$vpcId",
    "SubnetId=$appSubnet",
    "DbSubnetIds=$($dbSubnetIds -join ',')",
    "DbEngineVersion=$dbEngineVersion",
    "DbInstanceClass=$DbInstanceClass",
    "DbPassword=$dbPassword"
)

Write-Output 'Deploying CloudFormation stack. RDS and Beanstalk can take 15-25 minutes...'
aws cloudformation deploy `
    --stack-name $StackName `
    --template-file $template `
    --region $Region `
    --capabilities CAPABILITY_IAM `
    --no-fail-on-empty-changeset `
    --parameter-overrides @parameters

if ($LASTEXITCODE -ne 0) {
    Write-Output 'Stack events (newest first):'
    aws cloudformation describe-stack-events --stack-name $StackName --region $Region --max-items 30 --output table
    throw "CloudFormation deployment failed. Inspect events with: aws cloudformation describe-stack-events --stack-name $StackName --region $Region"
}

$outputsJson = aws cloudformation describe-stacks --stack-name $StackName --region $Region --query 'Stacks[0].Outputs' --output json
Assert-AwsSuccess 'Unable to read stack outputs.'
$outputs = $outputsJson | ConvertFrom-Json
$url = ($outputs | Where-Object { $_.OutputKey -eq 'ApplicationUrl' }).OutputValue
$dbEndpoint = ($outputs | Where-Object { $_.OutputKey -eq 'DatabaseEndpoint' }).OutputValue

Write-Output ''
Write-Output "Application URL: $url"
Write-Output "Database endpoint: $dbEndpoint"
Write-Output "State file: $statePath (git-ignored)"

if (-not $SkipSmokeTest) {
    Write-Output 'Waiting for the first boot, Flyway migration, and 10,000-employee seed...'
    $ready = $false
    for ($attempt = 1; $attempt -le 40; $attempt++) {
        try {
            $summary = Invoke-RestMethod -Uri "$url/api/analytics/summary" -TimeoutSec 15
            Write-Output "Ready: totalEmployees=$($summary.totalEmployees), currencies=$($summary.currencies.Count)"
            $ready = $true
            break
        }
        catch {
            Start-Sleep -Seconds 15
        }
    }
    if (-not $ready) {
        throw "The environment deployed but did not become ready at $url. Check Beanstalk logs and the RDS security group."
    }

    $deepLink = Invoke-WebRequest -Uri "$url/employees" -UseBasicParsing -TimeoutSec 20
    Write-Output "SPA deep link: HTTP $($deepLink.StatusCode)"
}
