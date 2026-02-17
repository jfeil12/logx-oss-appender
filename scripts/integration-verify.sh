#!/usr/bin/env bash

set -euo pipefail

MODE="${1:-full}"

if [[ "$MODE" != "quick" && "$MODE" != "full" ]]; then
    echo "用法: bash scripts/integration-verify.sh [quick|full]"
    exit 2
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="$ROOT_DIR/compatibility-tests/target/integration-verify"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
SUMMARY_FILE="$REPORT_DIR/summary-$TIMESTAMP.md"
LOG_FILE="$REPORT_DIR/run-$TIMESTAMP.log"

ensure_report_dir() {
    mkdir -p "$REPORT_DIR"
}

ensure_report_dir

MINIO_ENDPOINT="${LOGX_OSS_STORAGE_ENDPOINT:-http://localhost:9000}"
MINIO_ACCESS_KEY="${LOGX_OSS_STORAGE_ACCESS_KEY_ID:-minioadmin}"
MINIO_SECRET_KEY="${LOGX_OSS_STORAGE_ACCESS_KEY_SECRET:-minioadmin}"
MINIO_BUCKET="${LOGX_OSS_STORAGE_BUCKET:-logx-test-bucket}"

CLOUD_ENV="false"
PORT_FORWARD_URL="未检测到"
MINIO_SOURCE="unknown"
BUCKET_CHECK_METHOD="none"
JDK21_RUN_MODE="not-run"

STAGE="初始化"
LAST_COMMAND=""

declare -a EXECUTED_COMMANDS=()

log_info() {
    printf -- "[INFO] %s\n" "$1"
}

log_warn() {
    printf -- "[WARN] %s\n" "$1"
}

print_failure_template() {
    local error_message="$1"
    printf -- "\n"
    printf -- "失败阶段：%s\n" "$STAGE"
    printf -- "关键错误：%s\n" "$error_message"
    printf -- "已执行命令：%s\n" "${LAST_COMMAND:-无}"
    printf -- "建议动作：\n"
    printf -- "1. 检查 MinIO 服务状态与端口连通性（%s）\n" "$MINIO_ENDPOINT"
    printf -- "2. 检查认证参数与桶是否存在（bucket=%s）\n" "$MINIO_BUCKET"
    printf -- "3. 查看测试日志目录与 surefire 报告进行定位\n"
}

run_cmd() {
    local cmd="$1"
    LAST_COMMAND="$cmd"
    EXECUTED_COMMANDS+=("$cmd")

    ensure_report_dir
    log_info "执行命令: $cmd"
    if ! bash -lc "$cmd" >>"$LOG_FILE" 2>&1; then
        log_warn "命令失败，详细日志见: $LOG_FILE"
        print_failure_template "命令执行失败"
        write_summary "FAIL" "命令执行失败"
        exit 1
    fi
}

require_file() {
    local file_path="$1"
    if [[ ! -f "$file_path" ]]; then
        print_failure_template "缺少必要文件: $file_path"
        write_summary "FAIL" "缺少必要文件"
        exit 1
    fi
}

detect_cloud_env() {
    STAGE="云环境识别"
    if [[ "${CNB:-}" == "true" || -n "${CNB_PIPELINE_ID:-}" || -n "${CNB_VSCODE_PROXY_URI:-}" || -n "${VSCODE_PROXY_URI:-}" ]]; then
        CLOUD_ENV="true"
    fi

    local proxy_template="${CNB_VSCODE_PROXY_URI:-${VSCODE_PROXY_URI:-}}"
    if [[ -n "$proxy_template" ]]; then
        PORT_FORWARD_URL="${proxy_template//\{\{port\}\}/9001}"
    fi
}

compose_cmd() {
    if command -v docker-compose >/dev/null 2>&1; then
        printf "docker-compose"
        return
    fi

    if docker compose version >/dev/null 2>&1; then
        printf "docker compose"
        return
    fi

    printf ""
}

ensure_minio_up() {
    STAGE="准备 MinIO 环境"
    if curl -sf "$MINIO_ENDPOINT/minio/health/live" >/dev/null 2>&1; then
        MINIO_SOURCE="already-running"
        log_info "MinIO 已在运行: $MINIO_ENDPOINT"
        return
    fi

    if ! command -v docker >/dev/null 2>&1; then
        print_failure_template "MinIO 未运行且 Docker 不可用，无法自动拉起"
        write_summary "FAIL" "MinIO 未运行"
        exit 1
    fi

    local compose_runner
    compose_runner="$(compose_cmd)"
    if [[ -z "$compose_runner" ]]; then
        print_failure_template "Docker Compose 不可用，无法自动拉起 MinIO"
        write_summary "FAIL" "缺少 Docker Compose"
        exit 1
    fi

    log_warn "MinIO 未就绪，尝试使用 Docker 方式启动"
    run_cmd "cd '$ROOT_DIR/compatibility-tests/minio/docker' && $compose_runner down -v >/dev/null 2>&1 || true"
    run_cmd "cd '$ROOT_DIR/compatibility-tests/minio/docker' && $compose_runner up -d"
    run_cmd "curl -sf '$MINIO_ENDPOINT/minio/health/live' >/dev/null"
    MINIO_SOURCE="docker-compose"
}

endpoint_for_docker_check() {
    local endpoint="$1"
    endpoint="${endpoint/localhost/host.docker.internal}"
    endpoint="${endpoint/127.0.0.1/host.docker.internal}"
    printf "%s" "$endpoint"
}

check_bucket_and_credentials() {
    STAGE="MinIO 凭据与桶前置检查"

    if command -v mc >/dev/null 2>&1; then
        run_cmd "mc alias set logx '$MINIO_ENDPOINT' '$MINIO_ACCESS_KEY' '$MINIO_SECRET_KEY' --api S3v4 >/dev/null"
        run_cmd "mc ls 'logx/$MINIO_BUCKET' >/dev/null"
        BUCKET_CHECK_METHOD="mc-local"
        return
    fi

    local endpoint_docker
    endpoint_docker="$(endpoint_for_docker_check "$MINIO_ENDPOINT")"

    run_cmd "docker run --rm --add-host=host.docker.internal:host-gateway --entrypoint /bin/sh minio/mc:latest -c \"mc alias set logx '$endpoint_docker' '$MINIO_ACCESS_KEY' '$MINIO_SECRET_KEY' --api S3v4 >/dev/null 2>&1 && mc ls logx/'$MINIO_BUCKET' >/dev/null\""
    BUCKET_CHECK_METHOD="mc-docker"
}

run_prebuild() {
    STAGE="测试前构建"
    if [[ "$MODE" == "quick" ]]; then
        run_cmd "cd '$ROOT_DIR' && mvn -DskipTests install -pl logx-producer,logx-s3-adapter -am"
        return
    fi

    run_cmd "cd '$ROOT_DIR' && mvn clean install -DskipTests"
}

run_minio_core_test() {
    STAGE="MinIO 核心链路验证"
    run_cmd "cd '$ROOT_DIR' && mvn test -Dtest=MinIOIntegrationTest -pl logx-s3-adapter"
}

java_major() {
    local first_line
    first_line="$(java -version 2>&1 | sed -n '1p')"

    if [[ "$first_line" =~ \"1\.([0-9]+)\. ]]; then
        printf "%s" "${BASH_REMATCH[1]}"
        return
    fi

    if [[ "$first_line" =~ \"([0-9]+)\. ]]; then
        printf "%s" "${BASH_REMATCH[1]}"
        return
    fi

    printf "0"
}

run_full_chain() {
    STAGE="全量兼容链路验证"
    run_cmd "cd '$ROOT_DIR' && mvn compile exec:java -pl compatibility-tests/test-runner"

    local current_java_major
    current_java_major="$(java_major)"

    STAGE="jdk21-test 验证"
    if (( current_java_major >= 21 )); then
        run_cmd "cd '$ROOT_DIR' && mvn clean test -pl compatibility-tests/jdk21-test"
        JDK21_RUN_MODE="local-jdk21"
        return
    fi

    local endpoint_docker
    endpoint_docker="$(endpoint_for_docker_check "$MINIO_ENDPOINT")"
    run_cmd "docker run --rm --add-host=host.docker.internal:host-gateway -e LOGX_OSS_STORAGE_ENDPOINT='$endpoint_docker' -e LOGX_OSS_STORAGE_ACCESS_KEY_ID='$MINIO_ACCESS_KEY' -e LOGX_OSS_STORAGE_ACCESS_KEY_SECRET='$MINIO_SECRET_KEY' -e LOGX_OSS_STORAGE_BUCKET='$MINIO_BUCKET' -v '$ROOT_DIR:/workspace' -w /workspace maven:3.9.6-eclipse-temurin-21 bash -lc 'mvn clean install -DskipTests && mvn clean test -pl compatibility-tests/jdk21-test'"
    JDK21_RUN_MODE="docker-jdk21"
}

collect_trace_paths() {
    printf -- "- compatibility-tests/*/logs/application-error.log\n"
    printf -- "- compatibility-tests/*/target/surefire-reports\n"
    printf -- "- compatibility-tests/target/integration-verify/\n"
}

write_summary() {
    local result="$1"
    local note="$2"

    ensure_report_dir
    {
        printf -- "# Integration Verify Summary\n\n"
        printf -- "- 模式: %s\n" "$MODE"
        printf -- "- 结论: %s\n" "$result"
        printf -- "- 备注: %s\n" "$note"
        printf -- "- 云环境: %s\n" "$CLOUD_ENV"
        printf -- "- VSCode 9001 端口访问地址: %s\n" "$PORT_FORWARD_URL"
        printf -- "- MinIO 来源: %s\n" "$MINIO_SOURCE"
        printf -- "- MinIO 前置检查: endpoint=PASS, 凭据/桶检查方式=%s\n" "$BUCKET_CHECK_METHOD"
        printf -- "- jdk21-test 执行方式: %s\n\n" "$JDK21_RUN_MODE"

        printf -- "## 执行范围\n"
        if [[ "$MODE" == "quick" ]]; then
            printf -- "- logx-s3-adapter: MinIOIntegrationTest\n\n"
        else
            printf -- "- compatibility-tests/test-runner（覆盖兼容链路）\n"
            printf -- "- compatibility-tests/jdk21-test\n\n"
        fi

        printf -- "## 已执行关键命令\n"
        local i
        for i in "${EXECUTED_COMMANDS[@]}"; do
            printf -- "- %s\n" "$i"
        done

        printf -- "\n## 关键日志与报告路径\n"
        printf -- "- %s\n" "$LOG_FILE"
        collect_trace_paths
    } > "$SUMMARY_FILE"
}

main() {
    STAGE="预检"
    run_cmd "java -version"
    run_cmd "mvn -v"

    require_file "$ROOT_DIR/pom.xml"
    require_file "$ROOT_DIR/compatibility-tests/pom.xml"
    require_file "$ROOT_DIR/compatibility-tests/minio/start-minio-local.sh"
    require_file "$ROOT_DIR/compatibility-tests/minio/docker/start-minio-docker.sh"

    detect_cloud_env
    run_prebuild
    ensure_minio_up
    check_bucket_and_credentials
    run_minio_core_test

    if [[ "$MODE" == "full" ]]; then
        run_full_chain
    fi

    STAGE="输出总结"
    write_summary "PASS" "所有必选步骤通过"

    printf -- "\n"
    printf -- "模式: %s\n" "$MODE"
    printf -- "云环境识别: %s\n" "$CLOUD_ENV"
    printf -- "端口转发(9001): %s\n" "$PORT_FORWARD_URL"
    printf -- "MinIO 来源: %s\n" "$MINIO_SOURCE"
    printf -- "MinIOIntegrationTest: PASS\n"
    if [[ "$MODE" == "full" ]]; then
        printf -- "test-runner: PASS\n"
        printf -- "jdk21-test: PASS (%s)\n" "$JDK21_RUN_MODE"
    fi
    printf -- "bucket 验证: PASS (%s)\n" "$BUCKET_CHECK_METHOD"
    printf -- "总结论: PASS\n"
    printf -- "摘要文件: %s\n" "$SUMMARY_FILE"
    printf -- "详细日志: %s\n" "$LOG_FILE"
}

main "$@"
