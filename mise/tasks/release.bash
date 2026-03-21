#!/usr/bin/env bash
#MISE description="create the next semantic version tag"
#MISE alias="tag"
#MISE tools={svu="latest",usage="latest", gum="latest"}
#MISE quiet=true
#USAGE arg "[bump]" {
#USAGE   choices "next" "patch" "minor" "major"
#USAGE   default "next"
#USAGE   env "RELEASE_BUMP"
#USAGE }
#USAGE flag "-n --dry-run" help="print the next version without creating a tag" env="RELEASE_DRY_RUN"
#USAGE flag "--push" help="push created tag to origin" env="RELEASE_PUSH"
#USAGE flag "-f --force" help="overwrite an existing local and remote tag; requires --push" env="RELEASE_FORCE"
#USAGE flag "--skip-ci" help="append [skip ci] to the tag message" env="RELEASE_SKIP_CI"
#USAGE flag "--no-sign" help="create an unsigned annotated tag" env="RELEASE_NO_SIGN"

set -euo pipefail

print_stderr() {
  local color="$1"
  shift
  gum style --foreground "$color" "$*" >&2
}

warning() {
  print_stderr "#D97706" "$*"
}

info() {
  print_stderr "#2563EB" "$*"
}

success() {
  print_stderr "#16A34A" "$*"
}

BUMP="${usage_bump:-next}"
CURRENT_TAG="$(svu current)"
NEXT_TAG="$(svu "$BUMP")"
TAG_MESSAGE="Release $NEXT_TAG"
TAG_ARGS=(-s)
PUSH="${usage_push:-false}"
FORCE="${usage_force:-false}"

if [ "${usage_skip_ci:-false}" = "true" ]; then
  TAG_MESSAGE="${TAG_MESSAGE} [skip ci]"
fi

if [ "${usage_no_sign:-false}" = "true" ]; then
  TAG_ARGS=(-a)
fi

if [ "$FORCE" = "true" ] && [ "$PUSH" != "true" ]; then
  warning "--force requires --push"
  exit 1
fi

if [ "$CURRENT_TAG" = "$NEXT_TAG" ]; then
  case "$BUMP" in
    next)
      warning "No semantic versioning changes detected"
      ;;
    *)
      info "Computed next tag $NEXT_TAG matches current tag $CURRENT_TAG"
      ;;
  esac
  exit 1
fi

if [ "${usage_dry_run:-false}" = "true" ]; then
  info "Next tag: $NEXT_TAG"
  exit 0
fi

if [ "$FORCE" = "true" ]; then
  TAG_ARGS=(-f "${TAG_ARGS[@]}")
fi

git tag "${TAG_ARGS[@]}" "$NEXT_TAG" -m "$TAG_MESSAGE"
success "Created tag $NEXT_TAG"
if [ "$PUSH" = "true" ]; then
  if [ "$FORCE" = "true" ]; then
    git push origin "+refs/tags/$NEXT_TAG:refs/tags/$NEXT_TAG"
    success "Force-pushed tag $NEXT_TAG to origin"
  else
    git push origin "$NEXT_TAG"
    success "Pushed tag $NEXT_TAG to origin"
  fi
fi
