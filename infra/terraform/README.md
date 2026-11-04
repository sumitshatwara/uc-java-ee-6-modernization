# DayTrader infrastructure (validation only)

No infrastructure was deployed. Never run `terraform apply` in this lab.
No AWS credentials are required for init or validate. A real plan requires authorized AWS access;
the credential-free plan is expected to fail.

## Layout

- `modules/network`: VPC, two public and two private subnets in two AZs, IGW,
  one NAT gateway/EIP per AZ, public/private routes.
- `modules/alb`: public HTTP ALB, CIDR-restricted ingress, IP target group on
  8080, health check `/daytrader/actuator/health`, explicit `/rest/*` forwarding rule.
- `modules/rds`: encrypted Aurora PostgreSQL, private instance(s), subnet group,
  managed master secret, port 5432 ingress only from the ECS security group.
- `modules/ecs`: private Fargate service, task/execution IAM roles, logs with
  14-day retention, port 8080 ingress only from the ALB security group.
- `envs/dev`: composition and example inputs; local state only (no backend configured).

TARGET_STATE.md does not specify a region or numeric resource sizes. Dev defaults
here are us-east-1, two AZs, 512 CPU units/1024 MiB, one task and one db.t4g.medium
Aurora instance. Two NAT gateways incur costs if deployed. Allowed ingress defaults
to the documentation-only CIDR 192.0.2.0/24: replace with your authorized client CIDRs.
The explicit step requirements use HTTP; production HTTPS needs an ACM certificate
and TLS listener before deployment. This is a minimal validation scaffold, not a
production deployment (no autoscaling or SSM application settings yet).

## Validate

Use Terraform >=1.5 (tested with 1.9.8) and AWS provider ~>5.0:

```sh
terraform fmt -check -recursive infra/terraform
for dir in infra/terraform/modules/* infra/terraform/envs/dev; do
  terraform -chdir="$dir" init -backend=false
  terraform -chdir="$dir" validate
done
terraform -chdir=infra/terraform/envs/dev plan -input=false
```

Provider lock files are committed for all five validation roots; provider caches,
state, plans and local tfvars are ignored. CI validates each root independently.
Optionally set TF_PLUGIN_CACHE_DIR to an existing cache directory to reuse downloads.
No credentials, passwords or secret contents belong in tfvars or source control.

## Container and database wiring

Build from the standalone app context:

```sh
docker build -t daytrader-boot:infra daytrader-boot
# Only if Maven Central rate-limits:
docker build --build-arg MAVEN_MIRROR_URL=https://maven-central.storage-download.googleapis.com/maven2/ -t daytrader-boot:infra daytrader-boot
# With an authorized registry login, outside this validation-only run:
docker tag daytrader-boot:infra YOUR_REGISTRY/daytrader-boot:IMMUTABLE_TAG
docker push YOUR_REGISTRY/daytrader-boot:IMMUTABLE_TAG
```

Set `image` to that image URI (prefer a digest); the default example URI is not
deployable. Private-registry authentication other than ECR requires additional
execution-role/repository credentials configuration.

The task receives SPRING_PROFILES_ACTIVE=postgres,
SPRING_DATASOURCE_URL=jdbc:postgresql://<cluster endpoint>:5432/<database>, and TZ=UTC.
The image also sets -Duser.timezone=UTC. ECS injects SPRING_DATASOURCE_USERNAME and
SPRING_DATASOURCE_PASSWORD from the RDS-managed secret JSON keys using
`<secret ARN>:username::` and `<secret ARN>:password::`. Fargate platform 1.4.0
supports JSON-key secret injection. The execution role reads only this secret;
the task role has no AWS API grants. The default RDS Secrets Manager encryption
uses the AWS-managed key; the ECS module optionally accepts secret_kms_key_arn
for an externally configured customer-managed key (whose key policy must also
permit the execution role).

The ECS security group output depends only on VPC inputs. RDS consumes that ID,
while task/service resources consume RDS outputs; there is no whole-module RDS
dependency in ECS, avoiding a circular dependency. The root explicitly orders ECS
after ALB listener creation. Tasks can retry startup while the DB instance becomes
available; health checks gate traffic.

The ALB forwards /rest/* unchanged to the same target group; the application's
existing Tomcat rewrite handles the context path. Application-slice handlers are
outside this infrastructure step. The app does not auto-create the schema:
the lab's parity/seed.py invokes buildDBTables then buildDB over /daytrader/config.
Those actions are destructive and must not be exposed as production administration.
Restrict ALB CIDRs and design authenticated initialization before deployment.
Logs use awslogs; application JSON formatting is not changed here.
