param(
    [Security.SecureString]$Key
)

if ($null -eq $Key) {
    $Key = Read-Host "Clave privada AGROSENTINEL_DEMO_DATA_PASSWORD_KEY" -AsSecureString
}

$keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Key)

try {
    $keyText = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer)
    $keyBytes = [Text.Encoding]::UTF8.GetBytes($keyText)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer)
}

if ($keyBytes.Length -lt 32) {
    throw "La clave debe tener al menos 32 bytes UTF-8."
}

$emails = [System.Collections.Generic.List[string]]::new()
$emails.Add("admin1@gmail.com")
foreach ($prefix in @("tecnico", "supervisor", "operario", "almacenista")) {
    $lastNumber = switch ($prefix) {
        "tecnico" { 5 }
        "supervisor" { 5 }
        "operario" { 20 }
        "almacenista" { 3 }
    }

    for ($number = 1; $number -le $lastNumber; $number++) {
        $emails.Add("$prefix$number@gmail.com")
    }
}

$hmac = [Security.Cryptography.HMACSHA256]::new($keyBytes)
try {
    $credentials = foreach ($email in $emails) {
        $message = [Text.Encoding]::UTF8.GetBytes("agrosentinel-demo-account-v1:$($email.ToLowerInvariant())")
        $passwordBytes = $hmac.ComputeHash($message)
        $password = [Convert]::ToBase64String($passwordBytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
        [PSCustomObject]@{
            Email = $email
            Password = $password
        }
    }
}
finally {
    $hmac.Dispose()
    [Array]::Clear($keyBytes, 0, $keyBytes.Length)
    $keyText = $null
}
$credentials | ConvertTo-Csv -NoTypeInformation
