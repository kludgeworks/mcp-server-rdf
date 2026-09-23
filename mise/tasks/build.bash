#!/usr/bin/env bash
#MISE description="run Maven lifecycle goals"
#MISE alias="b"
#MISE tools={usage="latest"}
#USAGE arg "[goals]..." {
#USAGE   choices "validate" "compile" "test" "package" "verify" "install" "deploy" "native:compile" "spring-boot:run" "spring-boot:build-image"
#USAGE }
#USAGE flag "-c --clean" help="run clean before the selected goal"
#USAGE flag "-s --skip-tests" help="set -DskipTests"
#USAGE flag "-p --profile <profile>" {
#USAGE   choices "native" "nativeTest"
#USAGE }
#USAGE flag "--native-image-name <name>" help="set -Dnative.image.name for native builds"

set -euo pipefail

MVN_ARGS=()

if [ "${usage_skip_tests:-false}" = "true" ]; then
	MVN_ARGS+=("-DskipTests")
fi

if [ "${usage_clean:-false}" = "true" ]; then
	MVN_ARGS+=("clean")
fi

if [ -n "${usage_profile:-}" ]; then
	MVN_ARGS+=("-P${usage_profile}")
fi

if [ -n "${usage_native_image_name:-}" ]; then
	MVN_ARGS+=("-Dnative.image.name=${usage_native_image_name}")
fi

if [ -n "${usage_goals:-}" ]; then
	eval "GOALS=(${usage_goals:-})"
	MVN_ARGS+=("${GOALS[@]}")
else
	MVN_ARGS+=("package")
fi

./mvnw "${MVN_ARGS[@]}"
