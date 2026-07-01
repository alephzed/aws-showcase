provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = "aws-showcase"
      Env       = var.environment
      ManagedBy = "terraform"
    }
  }
}
