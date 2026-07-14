#!/usr/bin/env bash
# Build the iam Keycloak image from a given commit and deploy it to the test
# environment's iam-service (ECS). Repeats the manual procedure used for the
# RAID-712 branch deployment (task definition revision 955).
#
# Usage: ./scripts/deploy-iam-to-test.sh <commit-ish>
set -euo pipefail

COMMIT_ISH="${1:?usage: deploy-iam-to-test.sh <commit-ish>}"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ECR_REGISTRY="382051401658.dkr.ecr.ap-southeast-2.amazonaws.com"
ECR_PROFILE="raido-root"
TEST_PROFILE="raido-test"
REGION="ap-southeast-2"
CLUSTER="raid-api-cluster"
SERVICE="iam-service"

SHA="$(git -C "$REPO_ROOT" rev-parse "$COMMIT_ISH")"
IMAGE_URI="$ECR_REGISTRY/raid-iam:$SHA"
WORKTREE="$(mktemp -d /tmp/iam-build-XXXXXX)"
trap 'git -C "$REPO_ROOT" worktree remove --force "$WORKTREE" 2>/dev/null || true' EXIT

echo "==> Building iam from $SHA"
git -C "$REPO_ROOT" worktree add --detach "$WORKTREE" "$SHA"
(cd "$WORKTREE/iam" && ./gradlew build -q)

echo "==> Building image $IMAGE_URI"
docker build --platform linux/amd64 -t "$IMAGE_URI" "$WORKTREE/iam"

echo "==> Pushing to ECR"
aws ecr get-login-password --region "$REGION" --profile "$ECR_PROFILE" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"
docker push "$IMAGE_URI"

echo "==> Registering new task definition"
CURRENT_TD="$(aws ecs describe-services --cluster "$CLUSTER" --services "$SERVICE" \
  --profile "$TEST_PROFILE" --region "$REGION" \
  --query 'services[0].taskDefinition' --output text)"
echo "    current: $CURRENT_TD"

TD_JSON="$(mktemp /tmp/iam-taskdef-XXXXXX.json)"
aws ecs describe-task-definition --task-definition "$CURRENT_TD" \
  --profile "$TEST_PROFILE" --region "$REGION" --output json \
  | jq --arg img "$IMAGE_URI" '.taskDefinition
      | .containerDefinitions[0].image = $img
      | del(.taskDefinitionArn, .revision, .status, .requiresAttributes,
            .compatibilities, .registeredAt, .registeredBy)' > "$TD_JSON"

NEW_TD="$(aws ecs register-task-definition --cli-input-json "file://$TD_JSON" \
  --profile "$TEST_PROFILE" --region "$REGION" \
  --query 'taskDefinition.taskDefinitionArn' --output text)"
rm -f "$TD_JSON"
echo "    new:     $NEW_TD"

echo "==> Updating $SERVICE"
aws ecs update-service --cluster "$CLUSTER" --service "$SERVICE" \
  --task-definition "$NEW_TD" --force-new-deployment \
  --profile "$TEST_PROFILE" --region "$REGION" \
  --query 'service.deployments[0].id' --output text

echo "==> Waiting for service to stabilise (may take several minutes)"
aws ecs wait services-stable --cluster "$CLUSTER" --services "$SERVICE" \
  --profile "$TEST_PROFILE" --region "$REGION"

echo "==> Done. $SERVICE is running $IMAGE_URI"
