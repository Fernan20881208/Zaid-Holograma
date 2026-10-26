#!/system/bin/sh

# KRISSGAMERVIP - SCRIPT UNIFICADO
# Orden: Holo.sh y despues Holoo.sh
# 1 = Guardar copias de seguridad
# 2 = Activar Holo + Cor Forte en ambas etapas
# 3 = Restaurar copias de seguridad
# 4 = Activar solo Cor Forte en ambas etapas

SCRIPT="$0"

# ============================================================
# 1. COMPROBAR ROOT
# ============================================================

if [ "$(id -u 2>/dev/null)" -ne 0 ]; then
    if ! command -v su >/dev/null 2>&1; then
        echo "[ERRO] ROOT/SU nao encontrado."
        exit 1
    fi

    exec su -c "sh \"$SCRIPT\" \"${1:-2}\""
fi

if [ "$(id -u 2>/dev/null)" -ne 0 ]; then
    echo "[ERRO] O script precisa executar como root."
    exit 1
fi

echo "[OK] Executando como root (UID 0)."

# ============================================================
# 2. LOCALIZAR O DIRETORIO DO FREE FIRE
# ============================================================

# Detectar el paquete y perfil Android reales
APP_DIR=""
FALLBACK_DIR=""

PKG_LIST="$(pm list packages 2>/dev/null | sed 's/^package://')"

for pkg in $PKG_LIST; do
    case "$pkg" in
        *freefire*|*garena*)
            for user_root in /data/user/* /data/user_de/* /data/data; do
                candidate="$user_root/$pkg/files"
                [ -d "$candidate" ] || continue

                if [ -f "$candidate/split_asset_pack_install_time.apk.pdcache" ] ||
                   [ -f "$candidate/split_asset_pack_install_time.apk" ]; then
                    APP_DIR="$candidate"
                    break 2
                fi

                [ -n "$FALLBACK_DIR" ] || FALLBACK_DIR="$candidate"
            done
            ;;
    esac
done

# Comprobar nombres de paquete habituales
if [ -z "$APP_DIR" ]; then
    for pkg in com.dts.freefireth com.dts.freefiremax com.dts.freefire; do
        for user_root in /data/user/* /data/user_de/* /data/data; do
            candidate="$user_root/$pkg/files"
            [ -d "$candidate" ] || continue

            if [ -f "$candidate/split_asset_pack_install_time.apk.pdcache" ] ||
               [ -f "$candidate/split_asset_pack_install_time.apk" ]; then
                APP_DIR="$candidate"
                break 2
            fi

            [ -n "$FALLBACK_DIR" ] || FALLBACK_DIR="$candidate"
        done
    done
fi

[ -n "$APP_DIR" ] || APP_DIR="$FALLBACK_DIR"

if [ -z "$APP_DIR" ] || [ ! -d "$APP_DIR" ]; then
    echo "[ERROR] No se encontro la carpeta files del juego."
    echo "[INFO] Paquetes relacionados detectados:"
    pm list packages 2>/dev/null |
        grep -Ei 'freefire|garena' ||
        echo "No se detectaron paquetes relacionados."
    exit 1
fi

echo "[OK] Directorio detectado: $APP_DIR"
fi

if [ ! -d "$APP_DIR" ]; then
    echo "[ERRO] Diretorio do Free Fire nao encontrado."
    echo "[INFO] Caminho verificado:"
    echo "/data/user/0/com.dts.freefireth/files"
    echo "[INFO] Perfis adicionais verificados:"
    echo "/data/user/<usuario>/com.dts.freefireth/files"
    exit 1
fi

echo "[OK] Diretorio encontrado: $APP_DIR"

# ============================================================
# 3. COPIAR O ARQUIVO COMO ROOT
# ============================================================

SOURCE_APK="/data/local/tmp/split_asset_pack_install_time.apk"

TARGET1="$APP_DIR/split_asset_pack_install_time.apk"
TARGET2="$APP_DIR/split_asset_pack_install_time.apk.pdcache"

if [ ! -f "$SOURCE_APK" ]; then
    echo "[ERRO] Arquivo de origem nao encontrado:"
    echo "$SOURCE_APK"
    exit 1
fi

echo "[OK] Copiando como root (UID 0)..."

if ! cp -f "$SOURCE_APK" "$TARGET1"; then
    echo "[ERRO] Falha ao copiar o arquivo."
    echo "[INFO] Destino: $TARGET1"
    exit 1
fi

echo "[OK] Copia concluida: $TARGET1"

# ============================================================
# 4. ARQUIVOS TEMPORARIOS E BACKUPS INDEPENDENTES
# ============================================================

BACKUP1="/data/local/tmp/krissgamervipHolo.original.bak"
BACKUP2="/data/local/tmp/krissgamervipHoloo.original.bak"

WORK1="/data/local/tmp/krissgamervipHolo.work"
WORK2="/data/local/tmp/krissgamervipHoloo.work"

ROLLBACK1="/data/local/tmp/krissgamervipHolo.rollback"
ROLLBACK2="/data/local/tmp/krissgamervipHoloo.rollback"

# ============================================================
# 5. CODIGOS DOS MATERIAIS
# ============================================================

AVATAR_B64="MjE2YTcxMTNiN2YxYmM3NGViMjI4OWQ0N2YyYTQwNmQ="
ESP_ROSA_B64="ZjAwY2VmMGYwYjFiYTI2NDdiZTZmMmU1MjE3NjhhNzI="
TEMP_HOLO_B64="UkFUT19TV0FQX1RFTVBfMDAwMDAwMDAwMDAwMDAwMDE="

SHADER_ROSA_B64="MTJlYTM0MDcyYjM3Yjc5NDE4NGM5MTI1NmM4MDkwOTc="
XRAY_B64="MjdjY2NlMmFiNmEyZDczNGNiZDRhOTc1MDc0ZGZiN2I="
TEMP_COR_B64="UkFUT19TV0FQX1RFTVBfMDAwMDAwMDAwMDAwMDAwMDI="

# ============================================================
# 6. FUNCOES AUXILIARES
# ============================================================

decode64() {
    printf '%s' "$1" | base64 -d 2>/dev/null
}

count_code() {
    grep -aobF "$1" "$2" 2>/dev/null | wc -l | tr -d '[:space:]'
}

check_target() {
    if [ ! -f "$TARGET" ]; then
        echo "[ERRO] Arquivo alvo nao encontrado:"
        echo "$TARGET"
        return 1
    fi

    return 0
}

stop_game() {
    am force-stop com.dts.freefireth >/dev/null 2>&1
}

# ============================================================
# 7. GUARDAR BACKUP
# ============================================================

save_original() {
    check_target || return 1

    if [ -f "$BACKUP" ]; then
        echo "[OK] Backup existente preservado: $BACKUP"
        return 0
    fi

    if ! cp -p "$TARGET" "$BACKUP"; then
        echo "[ERRO] Nao foi possivel guardar o backup."
        return 1
    fi

    chmod 600 "$BACKUP" 2>/dev/null
    sync

    echo "[OK] Backup salvo: $BACKUP"
}

# ============================================================
# 8. PREPARAR ALTERACAO
# ============================================================

prepare_change() {
    check_target || return 1
    stop_game
    return 0
}

# ============================================================
# 9. TROCAR OS CODIGOS
# ============================================================

swap_pair() {
    FIRST="$(decode64 "$1")"
    SECOND="$(decode64 "$2")"
    TEMP="$(decode64 "$3")"

    if [ "${#FIRST}" -ne 32 ] ||
       [ "${#SECOND}" -ne 32 ] ||
       [ "${#TEMP}" -ne 32 ]; then
        echo "[ERRO] Falha ao decodificar os codigos."
        return 1
    fi

    FIRST_COUNT="$(count_code "$FIRST" "$TARGET")"
    SECOND_COUNT="$(count_code "$SECOND" "$TARGET")"

    if [ "$FIRST_COUNT" -eq 0 ] ||
       [ "$SECOND_COUNT" -eq 0 ]; then
        echo "[ERRO] O par de codigos nao existe:"
        echo "$TARGET"
        return 1
    fi

    if ! cp -p "$TARGET" "$WORK"; then
        echo "[ERRO] Falha ao criar arquivo temporario:"
        echo "$WORK"
        return 1
    fi

    ORIGINAL_SIZE="$(wc -c < "$TARGET" | tr -d '[:space:]')"

    if ! sed -i \
        -e "s/$FIRST/$TEMP/g" \
        -e "s/$SECOND/$FIRST/g" \
        -e "s/$TEMP/$SECOND/g" \
        "$WORK"; then
        rm -f "$WORK"
        echo "[ERRO] Falha na troca dos codigos."
        return 1
    fi

    NEW_SIZE="$(wc -c < "$WORK" | tr -d '[:space:]')"

    NEW_FIRST="$(count_code "$FIRST" "$WORK")"
    NEW_SECOND="$(count_code "$SECOND" "$WORK")"
    NEW_TEMP="$(count_code "$TEMP" "$WORK")"

    if [ "$NEW_SIZE" -ne "$ORIGINAL_SIZE" ] ||
       [ "$NEW_FIRST" -ne "$SECOND_COUNT" ] ||
       [ "$NEW_SECOND" -ne "$FIRST_COUNT" ] ||
       [ "$NEW_TEMP" -ne 0 ]; then
        rm -f "$WORK"
        echo "[ERRO] A verificacao da troca falhou."
        return 1
    fi

    if ! cat "$WORK" > "$TARGET"; then
        rm -f "$WORK"
        echo "[ERRO] Falha ao gravar o arquivo alvo."
        return 1
    fi

    rm -f "$WORK"
    chmod 660 "$TARGET"
    command -v restorecon >/dev/null 2>&1 &&
        restorecon "$TARGET" 2>/dev/null

    sync
    return 0
}

# ============================================================
# 10. ATIVAR HOLO + COR FORTE
# ============================================================

activate_holo_and_color() {
    prepare_change || return 1

    if ! cp -p "$TARGET" "$ROLLBACK"; then
        echo "[ERRO] Falha ao preparar restauracao temporaria."
        return 1
    fi

    if ! swap_pair "$AVATAR_B64" "$ESP_ROSA_B64" "$TEMP_HOLO_B64" ||
       ! swap_pair "$SHADER_ROSA_B64" "$XRAY_B64" "$TEMP_COR_B64"; then

        cat "$ROLLBACK" > "$TARGET"
        chmod 660 "$TARGET"

        command -v restorecon >/dev/null 2>&1 &&
            restorecon "$TARGET" 2>/dev/null

        rm -f "$ROLLBACK"
        sync

        echo "[ERRO] Alteracao falhou; arquivo restaurado."
        return 1
    fi

    rm -f "$ROLLBACK"
    sync

    echo "[OK] Holo e Cor Forte ativados."
    return 0
}

# ============================================================
# 11. ATIVAR SOMENTE COR FORTE
# ============================================================

activate_strong_color() {
    prepare_change || return 1

    swap_pair "$SHADER_ROSA_B64" "$XRAY_B64" "$TEMP_COR_B64" ||
        return 1

    echo "[OK] Cor Forte ativada."
    return 0
}

# ============================================================
# 12. RESTAURAR BACKUP
# ============================================================

restore_original() {
    check_target || return 1

    if [ ! -f "$BACKUP" ]; then
        echo "[ERRO] Backup nao encontrado:"
        echo "$BACKUP"
        return 1
    fi

    stop_game

    if ! cat "$BACKUP" > "$TARGET"; then
        echo "[ERRO] Falha ao restaurar o backup."
        return 1
    fi

    chmod 660 "$TARGET"

    command -v restorecon >/dev/null 2>&1 &&
        restorecon "$TARGET" 2>/dev/null

    sync

    echo "[OK] Arquivo restaurado: $TARGET"
    return 0
}

# ============================================================
# 13. EXECUTAR UMA ETAPA
# ============================================================

run_for_target() {
    STEP="$1"

    if [ "$STEP" = "1" ]; then
        TARGET="$TARGET1"
        BACKUP="$BACKUP1"
        WORK="$WORK1"
        ROLLBACK="$ROLLBACK1"

        echo ""
        echo "========== ETAPA 1/2: Holo.sh =========="
    else
        TARGET="$TARGET2"
        BACKUP="$BACKUP2"
        WORK="$WORK2"
        ROLLBACK="$ROLLBACK2"

        echo ""
        echo "========== ETAPA 2/2: Holoo.sh =========="
    fi

    case "$ACTION" in
        1) save_original ;;
        2) activate_holo_and_color ;;
        3) restore_original ;;
        4) activate_strong_color ;;
    esac
}

# ============================================================
# 14. ACAO SOLICITADA
# ============================================================

ACTION="${1:-2}"

case "$ACTION" in
    1|2|3|4) ;;
    *)
        echo "[ERRO] Funcao invalida. Use 1, 2, 3 ou 4."
        exit 1
        ;;
esac

# ============================================================
# 15. EXECUTAR EM ORDEM
# ============================================================

echo ""
echo "[INFO] Iniciando etapa 1 de 2."

run_for_target 1 || exit $?

echo ""
echo "[INFO] Etapa 1 concluida. Iniciando etapa 2 de 2."

run_for_target 2 || exit $?

echo ""
echo "[OK] As duas etapas foram concluidas na ordem solicitada."

exit 0
