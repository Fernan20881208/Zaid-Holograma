#!/system/bin/sh

# RATO PAINEL - arquivo unico em /data/local/tmp/rato_painel.sh

PANEL="$0"
TARGET="/data/user/0/com.dts.freefireth/files/split_asset_pack_install_time.apk.pdcache"
BACKUP="/data/local/tmp/split_asset_pack_install_time.apk.pdcache.bak"
WORK="/data/local/tmp/split_asset_pack_install_time.apk.pdcache.rato.tmp"

# Avatar <-> ESP material rosa (Holo)
AVATAR_B64="MjE2YTcxMTNiN2YxYmM3NGViMjI4OWQ0N2YyYTQwNmQ="
ESP_ROSA_B64="ZjAwY2VmMGYwYjFiYTI2NDdiZTZmMmU1MjE3NjhhNzI="
TEMP_HOLO_B64="UkFUT19TV0FQX1RFTVBfMDAwMDAwMDAwMDAwMDAwMDE="

# Shader material rosa <-> Shader contorno XRay (Cor Forte)
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
        echo "[ERRO] .pdcache nao encontrado:"
        echo "$TARGET"
        return 1
    fi
}

stop_game() {
    am force-stop com.dts.freefireth >/dev/null 2>&1
}

save_original() {
    check_target || return 1

    if [ -f "$BACKUP" ]; then
        echo "[OK] Backup original ja existe e foi preservado."
        return 0
    fi

    if ! cp -p "$TARGET" "$BACKUP"; then
        echo "[ERRO] Nao foi possivel salvar o backup."
        return 1
    fi

    chmod 600 "$BACKUP" 2>/dev/null
    sync
    echo "[OK] Backup original salvo com sucesso."
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

    if [ "$(printf '%s' "$FIRST" | wc -c | tr -d '[:space:]')" -ne 32 ] ||
       [ "$(printf '%s' "$SECOND" | wc -c | tr -d '[:space:]')" -ne 32 ] ||
       [ "$(printf '%s' "$TEMP" | wc -c | tr -d '[:space:]')" -ne 32 ]; then
        echo "[ERRO] Falha ao decodificar os codigos."
        return 1
    fi

    FIRST_COUNT="$(count_code "$FIRST" "$TARGET")"
    SECOND_COUNT="$(count_code "$SECOND" "$TARGET")"

    if [ "$FIRST_COUNT" -eq 0 ] || [ "$SECOND_COUNT" -eq 0 ]; then
        echo "[ERRO] O par de codigos nao existe neste .pdcache."
        return 1
    fi

    if ! cp -p "$TARGET" "$WORK"; then
        echo "[ERRO] Nao foi possivel criar o arquivo temporario."
        return 1
    fi

    ORIGINAL_SIZE="$(wc -c < "$TARGET" | tr -d '[:space:]')"

    if ! sed -i -e "s/$FIRST/$TEMP/g" -e "s/$SECOND/$FIRST/g" -e "s/$TEMP/$SECOND/g" "$WORK"; then
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
        echo "[ERRO] Nao foi possivel gravar o .pdcache."
        return 1
    fi

    rm -f "$WORK"
    chmod 660 "$TARGET"
    command -v restorecon >/dev/null 2>&1 && restorecon "$TARGET" 2>/dev/null
    sync
}

activate_holo() {
    prepare_change || return 1
    swap_pair "$AVATAR_B64" "$ESP_ROSA_B64" "$TEMP_HOLO_B64" || return 1
    echo "[OK] Holo ativado com sucesso."
}

activate_holo_and_color() {
    prepare_change || return 1

    ROLLBACK="/data/local/tmp/split_asset_pack_install_time.apk.pdcache.rollback.tmp"
    if ! cp -p "$TARGET" "$ROLLBACK"; then
        echo "[ERRO] Nao foi possivel preparar a troca conjunta."
        return 1
    fi

    if ! swap_pair "$AVATAR_B64" "$ESP_ROSA_B64" "$TEMP_HOLO_B64" ||
       ! swap_pair "$SHADER_ROSA_B64" "$XRAY_B64" "$TEMP_COR_B64"; then
        cat "$ROLLBACK" > "$TARGET"
        chmod 660 "$TARGET"
        command -v restorecon >/dev/null 2>&1 && restorecon "$TARGET" 2>/dev/null
        rm -f "$ROLLBACK"
        sync
        echo "[ERRO] A troca conjunta falhou e o arquivo anterior foi restaurado."
        return 1
    fi

    rm -f "$ROLLBACK"
    sync
    echo "[OK] Holo e Cor Forte ativados com sucesso."
}

activate_strong_color() {
    prepare_change || return 1
    swap_pair "$SHADER_ROSA_B64" "$XRAY_B64" "$TEMP_COR_B64" || return 1
    echo "[OK] Cor Forte ativada com sucesso."
}

restore_original() {
    check_target || return 1

    if [ ! -f "$BACKUP" ]; then
        echo "[ERRO] Backup original nao encontrado:"
        echo "$BACKUP"
        return 1
    fi

    stop_game

    if ! cat "$BACKUP" > "$TARGET"; then
        echo "[ERRO] Nao foi possivel restaurar o backup."
        return 1
    fi

    chmod 660 "$TARGET"
    command -v restorecon >/dev/null 2>&1 && restorecon "$TARGET" 2>/dev/null
    sync
    echo "[OK] Original restaurado com sucesso."
}

ACTION="$1"
if [ -z "$ACTION" ]; then ACTION=2; fi

case "$ACTION" in
    1|2|3|4) ;;
    *) echo "[ERRO] Funcao invalida. Use 1, 2, 3 ou 4."; exit 1 ;;
esac

if [ "$(id -u 2>/dev/null)" -ne 0 ]; then
    if ! command -v su >/dev/null 2>&1; then
        echo "[ERRO] ROOT/SU nao encontrado."
        exit 1
    fi
    exec su -c "sh \"$PANEL\" $ACTION"
fi

case "$ACTION" in
    1) save_original ;;
    2) activate_holo_and_color ;;
    3) restore_original ;;
    4) activate_strong_color ;;
esac

exit $?
