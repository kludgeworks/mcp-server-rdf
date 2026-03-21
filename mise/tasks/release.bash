#!/usr/bin/env bash
#MISE description="create the next semantic version tag"
#MISE alias="tag"
#MISE tools={svu="latest",usage="latest"}
#USAGE arg "[bump]" {
#USAGE   choices "next" "patch" "minor" "major"
#USAGE   default "next"
#USAGE   env "RELEASE_BUMP"
#USAGE }
#USAGE flag "-n --dry-run" help="print the next version without creating a tag" env="RELEASE_DRY_RUN"
#USAGE flag "--push" help="push created tag to origin" env="RELEASE_PUSH"
#USAGE flag "--skip-ci" help="append [skip ci] to the tag message" env="CI"
#USAGE flag "--no-sign" help="create an unsigned annotated tag" env="RELEASE_NO_SIGN"

set -euo pipefail

BUMP="${usage_bump:-next}"
CURRENT_TAG="$(svu current)"
NEXT_TAG="$(svu "$BUMP")"
TAG_MESSAGE="Release $NEXT_TAG"
TAG_ARGS=(-s)

if [ "${usage_skip_ci:-false}" = "true" ]; then
  TAG_MESSAGE="${TAG_MESSAGE} [skip ci]"
fi

if [ "${usage_no_sign:-false}" = "true" ]; then
  TAG_ARGS=(-a)
fi

if [ "$CURRENT_TAG" = "$NEXT_TAG" ]; then
  case "$BUMP" in
    next)
      printf '\033[33mNo semantic versioning changes detected\033[0m\n' >&2
      ;;
    *)
      printf '\033[33mComputed next tag %s matches current tag %s\033[0m\n' \
        "$NEXT_TAG" "$CURRENT_TAG" >&2
      ;;
  esac
  exit 1
fi

if [ "${usage_dry_run:-false}" = "true" ]; then
  printf '\033[32mNext tag: %s\033[0m\n' "$NEXT_TAG" >&2
  exit 0
fi

git tag "${TAG_ARGS[@]}" "$NEXT_TAG" -m "$TAG_MESSAGE"
printf '\033[32mCreated tag %s\033[0m\n' "$NEXT_TAG"
if [ "${usage_push:-false}" = "true" ]; then
  git push origin "$NEXT_TAG"
  echo "Pushed tag $NEXT_TAG to origin"
fi
