# Укажите путь к директории с файлами
$folderPath = "C:\Users\decor\IdeaProjects\Plasmirror\data\for_export\Extinction\P"

# Список префиксов, которые нужно оставить
$prefixes = @(
    "computation_0.00000000",
    "computation_0.30000000",
    "computation_0.60000000",
    "computation_1.00000000"
)

# Получаем список всех txt-файлов в директории
$files = Get-ChildItem -Path $folderPath -Filter "*.txt"

# Проходим по каждому файлу
foreach ($file in $files) {
    # Проверяем, содержит ли имя файла один из нужных префиксов
    $keepFile = $false
    foreach ($prefix in $prefixes) {
        if ($file.Name -like "$prefix*") {
            $keepFile = $true
            break
        }
    }

    # Если файл не содержит нужный префикс, удаляем его
    if (-not $keepFile) {
        Remove-Item -Path $file.FullName -Force
    }
}

Write-Host "Files have been removed"
