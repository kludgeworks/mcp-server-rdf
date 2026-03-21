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
PUSH="${usage_push:-false}"
FORCE="${usage_force:-false}"

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
  git tag -f -s "$NEXT_TAG" -m "Release $NEXT_TAG"
else
  git tag -s "$NEXT_TAG" -m "Release $NEXT_TAG"
fi

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
