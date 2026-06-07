# GCP Infrastructure Configuration (Terraform)

This directory contains the Terraform configuration to provision and manage the GCP infrastructure resources.

## Division of Responsibilities between Terraform and GitHub Actions

In this project, responsibilities are divided between Terraform and GitHub Actions:

- **Terraform (Infrastructure Definition)**:
  - Enables necessary GCP APIs.
  - Creates the Artifact Registry repository.
  - Creates the GCS Buckets (for application data and terraform state).
  - Creates Service Accounts and configures IAM permissions.
  - Provisions Secret Manager secrets (the containers only, not the values).
  - Defines the Cloud Run Job and Cloud Scheduler job.

- **GitHub Actions (Application Build & Execution)**:
  - Builds the Docker image.
  - Pushes the Docker image to Artifact Registry.
  - Executes the Cloud Run Job (e.g. `gcloud run jobs execute`).

*Note: The `image_uri` parameter for the Cloud Run Job is provided as a variable in Terraform. Terraform does not manage the image build process; it just expects a valid image URI to define the job correctly.*
