#!/system/bin/sh

# Asegura permisos root antes de ejecutar la primera operacion.
SCRIPT="$0"
if [ "$(id -u 2>/dev/null)" -ne 0 ]; then
    if ! command -v su >/dev/null 2>&1; then
        echo "[ERRO] ROOT/SU nao encontrado."
        exit 1
    fi
    exec su -c "sh \"$SCRIPT\" \"${1:-2}\""
fi

# Verifica nuevamente que la copia se ejecute con root (UID 0).
if [ "$(id -u 2>/dev/null)" -ne 0 ]; then
    echo "[ERRO] A copia precisa ser executada como root."
    exit 1
fi

# Primera operacion como root: copiar el APK al directorio del juego.
echo "[OK] Copiando como root (UID 0)..."
if ! cp /data/local/tmp/split_asset_pack_install_time.apk /data/data/com.dts.freefireth/files; then
    echo "[ERRO] Nao foi possivel copiar split_asset_pack_install_time.apk."
    exit 1
fi
echo "[OK] Copia concluida como root."

# Panel unificado: primero krissgamervipHolo.sh y despues krissgamervipHoloo.sh.
# Acciones: 1=guardar copias, 2=activar Holo + Cor Forte, 3=restaurar, 4=solo Cor Forte.
TARGET1="/data/user/0/com.dts.freefireth/files/split_asset_pack_install_time.apk"
TARGET2="/data/user/0/com.dts.freefireth/files/split_asset_pack_install_time.apk.pdcache"

# Copias separadas para no mezclar los originales de los dos archivos.
BACKUP1="/data/local/tmp/krissgamervipHolo.original.bak"
BACKUP2="/data/local/tmp/krissgamervipHoloo.original.bak"
WORK1="/data/local/tmp/krissgamervipHolo.work"
WORK2="/data/local/tmp/krissgamervipHoloo.work"
ROLLBACK1="/data/local/tmp/krissgamervipHolo.rollback"
ROLLBACK2="/data/local/tmp/krissgamervipHoloo.rollback"

AVATAR_B64="MjE2YTcxMTNiN2YxYmM3NGViMjI4OWQ0N2YyYTQwNmQ="
ESP_ROSA_B64="ZjAwY2VmMGYwYjFiYTI2NDdiZTZmMmU1MjE3NjhhNzI="
TEMP_HOLO_B64="UkFUT19TV0FQX1RFTVBfMDAwMDAwMDAwMDAwMDAwMDE="
SHADER_ROSA_B64="MTJlYTM0MDcyYjM3Yjc5NDE4NGM5MTI1NmM4MDkwOTc="
XRAY_B64="MjdjY2NlMmFiNmEyZDczNGNiZDRhOTc1MDc0ZGZiN2I="
TEMP_COR_B64="UkFUT19TV0FQX1RFTVBfMDAwMDAwMDAwMDAwMDAwMDI="

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

save_original() {
    check_target || return 1
    if [ -f "$BACKUP" ]; then
        echo "[OK] Copia original ja existe e foi preservada: $BACKUP"
        return 0
    fi
    if ! cp -p "$TARGET" "$BACKUP"; then
        echo "[ERRO] Nao foi possivel salvar a copia original: $BACKUP"
        return 1
    fi
    chmod 600 "$BACKUP" 2>/dev/null
    sync
    echo "[OK] Copia original salva: $BACKUP"
}

prepare_change() {
    check_target || return 1
    stop_game
    return 0
}

swap_pair() {
    FIRST="$(decode64 "$1")"
    SECOND="$(decode64 "$2")"
    TEMP="$(decode64 "$3")"

    if [ "${#FIRST}" -ne 32 ] || [ "${#SECOND}" -ne 32 ] || [ "${#TEMP}" -ne 32 ]; then
        echo "[ERRO] Falha ao decodificar os codigos."
        return 1
    fi

    FIRST_COUNT="$(count_code "$FIRST" "$TARGET")"
    SECOND_COUNT="$(count_code "$SECOND" "$TARGET")"
    if [ "$FIRST_COUNT" -eq 0 ] || [ "$SECOND_COUNT" -eq 0 ]; then
        echo "[ERRO] O par de codigos nao existe neste arquivo: $TARGET"
        return 1
    fi

    if ! cp -p "$TARGET" "$WORK"; then
        echo "[ERRO] Nao foi possivel criar o arquivo temporario: $WORK"
        return 1
    fi

    ORIGINAL_SIZE="$(wc -c < "$TARGET" | tr -d '[:space:]')"
    if ! sed -i \
        -e "s/$FIRST/$TEMP/g" \
        -e "s/$SECOND/$FIRST/g" \
        -e "s/$TEMP/$SECOND/g" \
        "$WORK"; then
        rm -f "$WORK"
        echo "[ERRO] A troca dos codigos falhou."
        return 1
    fi

    NEW_SIZE="$(wc -c < "$WORK" | tr -d '[:space:]')"
    NEW_FIRST="$(count_code "$FIRST" "$WORK")"
    NEW_SECOND="$(count_code "$SECOND" "$WORK")"
    NEW_TEMP="$(count_code "$TEMP" "$WORK")"

    if [ "$NEW_SIZE" -ne "$ORIGINAL_SIZE" ] ||
       [ "$NEW_FIRST" -ne "$SECOND_COUNT" ] ||
       [ "$NEW_SECOND" -ne "$FIRST_COUNT" ] || [ "$NEW_TEMP" -ne 0 ]; then
        rm -f "$WORK"
        echo "[ERRO] A verificacao da troca falhou."
        return 1
    fi

    if ! cat "$WORK" > "$TARGET"; then
        rm -f "$WORK"
        echo "[ERRO] Nao foi possivel gravar o arquivo alvo."
        return 1
    fi

    rm -f "$WORK"
    chmod 660 "$TARGET"
    command -v restorecon >/dev/null 2>&1 && restorecon "$TARGET" 2>/dev/null
    sync
    return 0
}

activate_holo_and_color() {
    prepare_change || return 1
    if ! cp -p "$TARGET" "$ROLLBACK"; then
        echo "[ERRO] Nao foi possivel preparar a reversao: $ROLLBACK"
        return 1
    fi

    if ! swap_pair "$AVATAR_B64" "$ESP_ROSA_B64" "$TEMP_HOLO_B64" ||
       ! swap_pair "$SHADER_ROSA_B64" "$XRAY_B64" "$TEMP_COR_B64"; then
        cat "$ROLLBACK" > "$TARGET"
        chmod 660 "$TARGET"
        command -v restorecon >/dev/null 2>&1 && restorecon "$TARGET" 2>/dev/null
        rm -f "$ROLLBACK"
        sync
        echo "[ERRO] A troca conjunta falhou; o arquivo desta etapa foi restaurado."
        return 1
    fi

    rm -f "$ROLLBACK"
    sync
    echo "[OK] Holo e Cor Forte ativados neste arquivo."
}

activate_strong_color() {
    prepare_change || return 1
    swap_pair "$SHADER_ROSA_B64" "$XRAY_B64" "$TEMP_COR_B64" || return 1
    echo "[OK] Cor Forte ativada neste arquivo."
}

restore_original() {
    check_target || return 1
    if [ ! -f "$BACKUP" ]; then
        echo "[ERRO] Copia original nao encontrada: $BACKUP"
        return 1
    fi
    stop_game
    if ! cat "$BACKUP" > "$TARGET"; then
        echo "[ERRO] Nao foi possivel restaurar a copia original."
        return 1
    fi
    chmod 660 "$TARGET"
    command -v restorecon >/dev/null 2>&1 && restorecon "$TARGET" 2>/dev/null
    sync
    echo "[OK] Original restaurado para: $TARGET"
}

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

ACTION="${1:-2}"
case "$ACTION" in
    1|2|3|4) ;;
    *) echo "[ERRO] Funcao invalida. Use 1, 2, 3 ou 4."; exit 1 ;;
esac

# Executa o primeiro arquivo e, somente se concluir sem erro, o segundo.
run_for_target 1 || exit $?
run_for_target 2 || exit $?

echo ""
echo "[OK] As duas etapas foram concluidas na ordem solicitada."
exit 0
