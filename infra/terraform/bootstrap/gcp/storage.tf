resource "google_storage_bucket" "terraform_state" {
  name          = var.state_bucket_name
  location      = var.region
  force_destroy = false

  # Enable versioning for terraform.tfstate backup/recovery
  versioning {
    enabled = true
  }

  uniform_bucket_level_access = true

  # Ignore changes to lifecycle rules or other settings that might be added externally
  lifecycle {
    prevent_destroy = true
  }
}
