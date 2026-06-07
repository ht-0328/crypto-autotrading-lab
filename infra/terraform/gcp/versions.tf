terraform {
  required_version = ">= 1.5.0"

  backend "gcs" {
    # bucket name is passed via backend-config during terraform init
    prefix = "terraform/gcp"
  }

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}
