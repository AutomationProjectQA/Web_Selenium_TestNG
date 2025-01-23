# Set initial variables
$isWSLExecution=false
$FolderName="./Generated/VNCs/"

# Create directory if it doesn't exist
if (-Not (Test-Path $FolderName)) {
    New-Item -ItemType Directory -Path $FolderName
    Write-Host "Folder created successfully"
} else {
    Write-Host "Folder exists"
}

# Get IP address of the machine
$ipAdd = (ipconfig getifaddr en0)
Write-Host "IP Address: $ipAdd"


# Get Docker container information
$output=/usr/local/bin/docker ps --format "{{.Ports}}"
Write-Host $output

# Getting port numbers of the containers to start the VNC
$portNo =[regex]::Matches($output, '\b\d{5}\b') | ForEach-Object { $_.Value }
Write-Host "Raw output: $portNo"

# Create VNC configuration files and start VNC viewers
foreach ($value in $portNo) {
    # Prepare VNC file content
    $fileContent = (Get-Content -Path "/Users/meet/eclipse-workspace/WebPOM/Execution/vnc/configvnc.vnc").Replace('%s', $value).Replace('%IPAdd%', $ipAdd)
    
    # Save the file
    $fileName = "/Users/meet/eclipse-workspace/WebPOM/Execution/vnc/Generated/VNCs/"+"vnc$value.vnc"
    
    Write-Host $fileName
    New-Item -Path $fileName -ItemType File
    Set-Content -Path $fileName -Value $fileContent
    
    # Start the VNC viewer
   open -a "VNC Viewer" "$fileName"
    Start-Sleep -Seconds 5
    
    osascript -e 'tell application "System Events" to tell (first application process whose frontmost is true) to set the size of the front window to {600, 600}'
}
