# infrastructure/terraform

Placeholder. Phase 10 adds Terraform modules/environments per master
spec §27:

```text
terraform/
  modules/
    vpc/ eks/ ecr/ aurora/ msk/ redis/ s3/ cloudfront/
    api-gateway/ cognito/ iam/ kms/ secrets-manager/ observability/
  environments/
    dev/ qa/ prod/
```

No secrets are ever committed here; environment-specific configuration
is separated per environment directory. Nothing here yet — this is a
learning project's Phase 0 scaffold, not a live AWS account.
