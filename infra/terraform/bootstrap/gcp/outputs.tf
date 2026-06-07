output "state_bucket_name" {
  description = "The name of the created GCS bucket for Terraform state"
  value       = google_storage_bucket.terraform_state.name
}
