# --- CONFIGURAZIONE ---
$ContainerName = "postgres-db"   
$DbUser = "postgres"
$BackupDir = "C:\Users\Nunzio\OneDrive\Desktop\progetto_ss\prenotazion-aule-keycloak\prenotazion-aule-keycloak\Backups_Postgres"
$Date = Get-Date -Format "yyyyMMdd_HHmmss"

# Nomi dei file
$SqlFile = "$BackupDir\temp_backup_$Date.sql"  # File temporaneo in chiaro
$ZipFile = "$BackupDir\backup_SECURE_$Date.7z" # File finale cifrato
$Password = "nunzio"         # <--- CAMBIA QUESTA!

$7z = "C:\Program Files\7-Zip\7z.exe"

# --- 1. CREAZIONE CARTELLA ---
if (-not (Test-Path -Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir | Out-Null
}

# --- 2. ESECUZIONE DUMP (In Chiaro) ---
Write-Host "1. Scarico il database..." -ForegroundColor Yellow
if ($(docker ps -q -f name=$ContainerName)) {
    docker exec -i $ContainerName pg_dumpall -c -U $DbUser > $SqlFile 2>&1
    
    # --- 3. CIFRATURA (Il tocco di classe) ---
    if (Test-Path $SqlFile) {
        Write-Host "2. Cifratura in corso con AES-256..." -ForegroundColor Cyan
        
        # Comando magico di 7-Zip:
        # a = aggiungi all'archivio
        # -p = imposta password
        # -mhe = cifra anche i nomi dei file (header encryption)
        & $7z a -t7z "$ZipFile" "$SqlFile" -p"$Password" -mhe | Out-Null
        
        if (Test-Path $ZipFile) {
            # --- 4. PULIZIA ---
            Remove-Item $SqlFile # Cancella il file in chiaro
            Write-Host "SUCCESSO! Backup cifrato creato: $ZipFile" -ForegroundColor Green
        } else {
            Write-Host "ERRORE: Creazione ZIP fallita." -ForegroundColor Red
        }
    } else {
        Write-Host "ERRORE: Il file SQL non è stato creato." -ForegroundColor Red
    }
} else {
    Write-Host "ERRORE: Container non trovato." -ForegroundColor Red
}
