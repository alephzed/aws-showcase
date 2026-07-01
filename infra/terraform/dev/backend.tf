terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Remote state (pre-provisioned out-of-band).
  backend "s3" {
    bucket         = "aws-showcase-tfstate-776163183313"
    key            = "env/dev/terraform.tfstate"
    region         = "us-east-2"
    dynamodb_table = "aws-showcase-tflock"
    encrypt        = true
  }
}
